// Cl0de Deep bridge: an OpenAI-compatible HTTP front for the DeepChat module.
//
// deepchat.mjs (in this same directory) is a third-party module bundled
// byte-for-byte; this bridge is ours and the module is never edited. The
// module keeps its DeepChat session in this process's memory, so the bridge
// is one long-lived process and chat calls are serialized: the module's
// session is a single global and concurrent calls would race its
// bootstrap/claim flow.
//
// The module has no timeout anywhere, so the bridge imposes its own
// (120 s). A timed-out HTTP answer does NOT free the queue slot early: the
// underlying module call is allowed to settle first, so the next call never
// overlaps it inside the module.
//
// Health is local liveness only: it answers while this process is alive and
// the module imported, even if the DeepChat session has not bootstrapped yet
// or the upstream is unreachable. Upstream failures surface per request as
// OpenAI-shaped errors, never as a dead bridge.

import http from 'node:http';
import { chat, getConfig } from './deepchat.mjs';

const HOST = '127.0.0.1';
const PORT = 8181;
const SERVICE = 'cl0de-deep-bridge';
const CHAT_TIMEOUT_MS = 120_000;
const MAX_BODY_BYTES = 32 * 1024 * 1024;
// DeepChat rejects a systemPrompt longer than this with HTTP 400
// ("systemPrompt exceeds maximum length of 4000 characters").
const SYSTEM_PROMPT_MAX_CHARS = 4000;

// Static catalog snapshot from the module's own getConfig() (8 Nov 2026),
// filtered to capabilities.text_chat == true && enabled == true. A background
// refresh replaces it when the live catalog answers; a failed refresh keeps
// the snapshot. Models that are text_chat but disabled upstream (for example
// deepseek-reasoner and sonar-deep-research) are deliberately absent.
let catalog = [
  { providerId: 'chatgpt', id: 'gpt-4o-mini', displayName: 'GPT-4o Mini', contextWindow: 128000, image: true },
  { providerId: 'chatgpt', id: 'gpt-4.1', displayName: 'GPT-4.1', contextWindow: 1000000, image: true },
  { providerId: 'claude', id: 'claude-haiku-4-5-20251001', displayName: 'Claude Haiku 4.5', contextWindow: 200000, image: true },
  { providerId: 'claude', id: 'claude-opus-4-7', displayName: 'Claude Opus 4.7', contextWindow: 1000000, image: true },
  { providerId: 'gemini', id: 'gemini-2.5-flash-lite', displayName: 'Gemini 2.5 Flash Lite', contextWindow: 1000000, image: false },
  { providerId: 'gemini', id: 'gemini-2.5-pro', displayName: 'Gemini 2.5 Pro', contextWindow: 1000000, image: true },
  { providerId: 'deepseek', id: 'deepseek-v4-flash', displayName: 'DeepSeek V4 Flash', contextWindow: 128000, image: false },
  { providerId: 'deepseek', id: 'deepseek-chat', displayName: 'DeepSeek V3', contextWindow: 128000, image: false },
  { providerId: 'kimi', id: 'kimi-k3', displayName: 'Kimi K3', contextWindow: 1048576, image: true },
  { providerId: 'kimi', id: 'kimi-k2.6', displayName: 'Kimi K2.6', contextWindow: 256000, image: false },
  { providerId: 'grok', id: 'grok-4-1-fast', displayName: 'Grok 4.1 Fast', contextWindow: 2000000, image: false },
  { providerId: 'grok', id: 'grok-4', displayName: 'Grok 4', contextWindow: 131072, image: false },
  { providerId: 'mistral', id: 'ministral-8b-latest', displayName: 'Mistral Small', contextWindow: 128000, image: false },
  { providerId: 'mistral', id: 'mistral-large-latest', displayName: 'Mistral Large 3', contextWindow: 128000, image: false },
  { providerId: 'perplexity', id: 'sonar-pro', displayName: 'Sonar Pro', contextWindow: 200000, image: false },
  { providerId: 'perplexity', id: 'sonar', displayName: 'Sonar', contextWindow: 200000, image: false },
];
let index = new Map(catalog.map((m) => [m.id, m]));

async function refreshCatalog() {
  try {
    const cfg = await getConfig();
    const list = [];
    for (const p of cfg?.providers ?? []) {
      if (!p || p.enabled !== true) continue;
      for (const m of p.models ?? []) {
        if (!m || m.enabled !== true || m.capabilities?.text_chat !== true) continue;
        list.push({
          providerId: p.id,
          id: m.id,
          displayName: m.displayName || m.id,
          contextWindow: m.contextWindow || 0,
          image: m.capabilities?.image_input === true,
        });
      }
    }
    if (list.length > 0) {
      catalog = list;
      index = new Map(list.map((m) => [m.id, m]));
      console.log(`catalog refreshed from getConfig: ${list.length} chat models`);
    }
  } catch (e) {
    console.log(`catalog refresh failed, keeping snapshot: ${String(e?.message ?? e).slice(0, 160)}`);
  }
}

// ---------- OpenAI request mapping ----------

// Text of a message content: a plain string, or the text parts of an array.
// Everything that is not text or the one supported image part is flattened
// away here; the module only ever receives plain strings.
function textOf(content) {
  if (typeof content === 'string') return content;
  if (Array.isArray(content)) {
    return content
      .filter((p) => p && p.type === 'text' && typeof p.text === 'string')
      .map((p) => p.text)
      .join('');
  }
  return '';
}

// First image of a message, as a data URL or remote URL string. The module
// resolves both itself; a local file path is never passed (inside the proot
// it would be ambiguous, and attachments arrive as data URLs anyway).
function firstImage(content) {
  if (!Array.isArray(content)) return null;
  for (const p of content) {
    if (p && p.type === 'image_url' && typeof p.image_url?.url === 'string' && p.image_url.url) return p.image_url.url;
  }
  return null;
}

// ---------- serialized module calls with a bridge-side timeout ----------

let chain = Promise.resolve();
const noop = () => {};

function callModule(message, options) {
  const task = chain.then(() => chat(message, options));
  // The queue advances only when the module call itself settles, even if the
  // HTTP side already gave up on it: the module session must never see two
  // calls overlap.
  chain = task.then(noop, noop);
  let timer = null;
  const timeout = new Promise((_, reject) => {
    timer = setTimeout(() => {
      const e = new Error(`DeepChat did not answer within ${CHAT_TIMEOUT_MS / 1000} seconds`);
      e.status = 504;
      reject(e);
    }, CHAT_TIMEOUT_MS);
    timer.unref?.();
  });
  return Promise.race([task, timeout]).finally(() => clearTimeout(timer));
}

// ---------- responses ----------

function sendJson(res, status, obj) {
  const body = JSON.stringify(obj);
  res.writeHead(status, { 'Content-Type': 'application/json', 'Content-Length': Buffer.byteLength(body) });
  res.end(body);
}

// Upstream error text carries the raw server body; forward a bounded,
// tag-stripped version inside the OpenAI error shape, never the whole body.
function cleanMessage(err) {
  return String(err?.message ?? err ?? 'unknown error')
    .replace(/<[^>]*>/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
    .slice(0, 300);
}

function sendError(res, status, message) {
  sendJson(res, status, { error: { message: cleanMessage({ message }), type: 'upstream_error', code: status } });
}

function upstreamStatus(err) {
  if (Number.isInteger(err?.status)) return err.status;
  const m = /HTTP (\d{3})/.exec(String(err?.message ?? ''));
  if (m) {
    const s = Number(m[1]);
    if (s >= 400 && s < 600) return s;
  }
  return 502;
}

async function handleChat(req, res) {
  let raw = '';
  let size = 0;
  for await (const chunk of req) {
    size += chunk.length;
    if (size > MAX_BODY_BYTES) {
      sendError(res, 413, 'Request body too large');
      req.destroy();
      return;
    }
    raw += chunk;
  }
  let body;
  try {
    body = JSON.parse(raw || '{}');
  } catch {
    sendError(res, 400, 'Request body is not valid JSON');
    return;
  }

  const entry = index.get(body?.model);
  if (!entry) {
    sendError(res, 404, `Unknown Deep model: ${String(body?.model ?? '')}`);
    return;
  }
  const messages = Array.isArray(body.messages) ? body.messages : [];

  // system turns merge into the module's single systemPrompt; only system,
  // user and assistant have a mapping, every other role is dropped.
  const mergedSystem = messages
    .filter((m) => m && m.role === 'system')
    .map((m) => textOf(m.content))
    .filter(Boolean)
    .join('\n\n');
  // The merged text is capped at the upstream limit, keeping the front: the
  // agent prompt comes first, environment context and AGENTS.md follow and
  // are what gets cut. This also covers a single oversized system turn.
  let systemPrompt = mergedSystem;
  if (systemPrompt.length > SYSTEM_PROMPT_MAX_CHARS) {
    console.log(`systemPrompt trimmed: ${systemPrompt.length} -> ${SYSTEM_PROMPT_MAX_CHARS} chars`);
    systemPrompt = systemPrompt.slice(0, SYSTEM_PROMPT_MAX_CHARS);
  }

  let lastUser = -1;
  for (let i = messages.length - 1; i >= 0; i--) {
    if (messages[i] && messages[i].role === 'user') { lastUser = i; break; }
  }
  if (lastUser < 0) {
    sendError(res, 400, 'No user message in the request');
    return;
  }

  let message = textOf(messages[lastUser].content);
  // One image slot, only from the last user turn, and only for models whose
  // catalog entry accepts image input; for text-only models it is dropped.
  const image = entry.image ? firstImage(messages[lastUser].content) : null;
  // The module throws on an empty message: an image-only turn gets a
  // placeholder text instead.
  if (!message && image) message = 'Describe this image.';
  if (!message) {
    sendError(res, 400, 'The last user message has no text');
    return;
  }

  // History is every mapped turn BEFORE the last user message; the last
  // message itself is never duplicated into it.
  const history = [];
  for (let i = 0; i < lastUser; i++) {
    const m = messages[i];
    if (!m || (m.role !== 'user' && m.role !== 'assistant')) continue;
    const t = textOf(m.content);
    if (t) history.push({ role: m.role, content: t });
  }

  const options = { providerId: entry.providerId, modelId: entry.id };
  if (systemPrompt) options.systemPrompt = systemPrompt;
  if (history.length > 0) options.conversationHistory = history;
  if (image) options.image = image;
  // deepThink / deepResearch / writeCode are never mapped in v1.

  const started = Date.now();
  let result;
  try {
    result = await callModule(message, options);
  } catch (e) {
    console.log(`chat ${entry.id} failed after ${Date.now() - started}ms: ${cleanMessage(e).slice(0, 160)}`);
    sendError(res, upstreamStatus(e), e?.message ?? 'DeepChat request failed');
    return;
  }
  const reply = typeof result?.reply === 'string' ? result.reply : '';
  const md = result?.metadata ?? {};
  const usage = {
    prompt_tokens: Number.isFinite(md.inputTokens) ? md.inputTokens : 0,
    completion_tokens: Number.isFinite(md.outputTokens) ? md.outputTokens : 0,
    total_tokens: (Number.isFinite(md.inputTokens) ? md.inputTokens : 0) + (Number.isFinite(md.outputTokens) ? md.outputTokens : 0),
  };
  console.log(
    `chat ${entry.id} ok in ${Date.now() - started}ms, creditsUsed=${result?.creditsUsed ?? '?'}, creditsRemaining=${result?.creditsRemaining ?? '?'}`,
  );
  const id = `chatcmpl-${Date.now().toString(36)}${Math.floor(Math.random() * 1e6).toString(36)}`;
  const created = Math.floor(Date.now() / 1000);

  if (body.stream === true) {
    // The upstream answers whole, never streamed: synthesize the SSE form,
    // one content chunk and the finish chunk, then [DONE].
    res.writeHead(200, { 'Content-Type': 'text/event-stream', 'Cache-Control': 'no-cache', Connection: 'keep-alive' });
    const base = { id, object: 'chat.completion.chunk', created, model: entry.id };
    res.write(`data: ${JSON.stringify({ ...base, choices: [{ index: 0, delta: { role: 'assistant', content: reply }, finish_reason: null }] })}\n\n`);
    res.write(`data: ${JSON.stringify({ ...base, choices: [{ index: 0, delta: {}, finish_reason: 'stop' }], usage })}\n\n`);
    res.write('data: [DONE]\n\n');
    res.end();
    return;
  }

  sendJson(res, 200, {
    id,
    object: 'chat.completion',
    created,
    model: entry.id,
    choices: [{ index: 0, message: { role: 'assistant', content: reply }, finish_reason: 'stop' }],
    usage,
  });
}

const server = http.createServer((req, res) => {
  const url = new URL(req.url ?? '/', `http://${HOST}:${PORT}`);
  if (req.method === 'GET' && url.pathname === '/health') {
    sendJson(res, 200, { ok: true, service: SERVICE, version: 1 });
    return;
  }
  if (req.method === 'GET' && url.pathname === '/v1/models') {
    sendJson(res, 200, {
      object: 'list',
      data: catalog.map((m) => ({ id: m.id, object: 'model', created: 0, owned_by: 'deep' })),
    });
    return;
  }
  if (req.method === 'POST' && url.pathname === '/v1/chat/completions') {
    handleChat(req, res).catch((e) => {
      try { sendError(res, 500, e?.message ?? 'bridge error'); } catch { /* response already gone */ }
    });
    return;
  }
  sendError(res, 404, `No route for ${req.method} ${url.pathname}`);
});

server.listen(PORT, HOST, () => {
  console.log(`${SERVICE} listening on ${HOST}:${PORT} (${catalog.length} chat models)`);
  refreshCatalog();
});
