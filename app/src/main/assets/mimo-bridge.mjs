// Cl0de MiMo bridge: an OpenAI-compatible HTTP front for the Xiaomi MiMo
// Studio module.
//
// mimo.js (in this same directory) is a third-party module bundled
// byte-for-byte; this bridge is ours and the module is never edited. The
// module is pure CommonJS, so it is loaded through the default import
// interop below. Its file name matters: the module resolves its session
// cookie per call, and the first candidate is mimocook.txt next to the
// module file itself (its __dirname), so the staged cookie must live in
// this same directory under exactly that name.
//
// The module's shape differs from a plain chat API in ways this bridge
// absorbs:
// - It takes ONE prompt string per turn plus an optional conversationId;
//   the conversation's memory lives on the MiMo server, keyed by that id.
//   There is no system-prompt slot and no history parameter.
// - It has NO timeout anywhere, so the bridge imposes its own (120 s),
//   and calls are serialized per conversation (a keyed chain in the Deep
//   bridge's enqueue pattern) so two requests of one conversation never
//   race inside the module.
// - Its errors read "MiMo chat error (401): ...", not "HTTP 401".
// - Its result names the answer `text` (not `reply`), carries no
//   metadata, and its usage comes raw from the SSE usage event in
//   camelCase ({ promptTokens, completionTokens, totalTokens,
//   nativeUsage }) and may be undefined entirely.
//
// Conversation mapping: OpenCode resends the whole message list on every
// request, while the module wants only the newest turn. The bridge keeps
// a bounded LRU map from a conversation key to the server conversationId.
// The key is sha256(folded system text + first user message): the first
// user message alone collides across rooms that open with the same word
// ("halo") and would leak one room's context into another. The folded
// system text is stable across one session's turns; if it ever changes,
// the map simply misses and the defined miss behaviour below applies,
// never a wrong-room hit.
//
// Defined behaviours:
// - First turn (key unknown, one user turn): the system turns and that
//   user message fold into ONE prompt (front-kept, ~6000 chars), sent
//   without a conversationId; the id the module echoes back is stored.
// - Continuation (key known): only the last user message's text is sent,
//   with the stored conversationId.
// - Map miss on a MULTI-turn request (bridge restart, LRU eviction): the
//   record is gone, so a new conversation is seeded with the folded
//   system + first user message + last user message (when it differs);
//   middle turns are NOT replayed. The current question is always part
//   of what the model sees.
// - Dead conversationId: when a continuation call fails, the stored id
//   is dropped and the call is retried ONCE as a new conversation (the
//   same seeded fold as a multi-turn miss). Only if the retry also fails
//   does the error surface, OpenAI-shaped.
//
// Health is local liveness only, like the Deep bridge: it answers while
// this process is alive and the module imported, even if the MiMo cookie
// is missing or stale. Cookie and upstream failures surface per request.

import http from 'node:http';
import { createHash } from 'node:crypto';
import mimoPkg from './mimo.js';

const { askMiMoWeb, AVAILABLE_MODELS, loadMiMoCookiesSync } = mimoPkg;

const HOST = '127.0.0.1';
const PORT = 8182;
const SERVICE = 'cl0de-mimo-bridge';
const CHAT_TIMEOUT_MS = 120_000;
const MAX_BODY_BYTES = 32 * 1024 * 1024;
// First-turn fold budget: the module has no system slot, so the folded
// prompt is the only place the agent prompt and context can ride. The
// front is kept (agent prompt first, like the Deep bridge's trim).
const FOLD_MAX_CHARS = 6000;
const FOLD_MARKER = '\n\n[...]';
// Bounded conversation map: beyond this many entries the least recently
// used conversation is forgotten and its next request takes the defined
// map-miss path above.
const CONV_MAP_MAX = 50;

// The four MiMo models, enforced as a whitelist here: the module itself
// validates nothing, so an unknown id must 404 at the bridge (the Deep
// bridge's pattern) instead of travelling upstream as-is.
const MODELS = Array.isArray(AVAILABLE_MODELS) && AVAILABLE_MODELS.length > 0
  ? AVAILABLE_MODELS
  : ['mimo-v2.6-pro', 'mimo-v2.6-flash', 'mimo-v2.5-pro', 'mimo-v2.5'];
const modelSet = new Set(MODELS);

// ---------- request mapping ----------

// Text of a message content: a plain string, or the text parts of an
// array. MiMo has no image path at all (the module's multiMedias is
// always empty), so non-text parts simply contribute nothing.
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

// ---------- conversation map (bounded LRU) ----------

const convMap = new Map();

function convGet(key) {
  const v = convMap.get(key);
  if (v !== undefined) {
    // refresh recency: a hit makes this the most recently used entry
    convMap.delete(key);
    convMap.set(key, v);
  }
  return v;
}

function convSet(key, conversationId) {
  convMap.delete(key);
  convMap.set(key, conversationId);
  while (convMap.size > CONV_MAP_MAX) {
    convMap.delete(convMap.keys().next().value);
  }
}

function convDelete(key) {
  convMap.delete(key);
}

// ---------- serialized module calls with a bridge-side timeout ----------

// One keyed chain per conversation key (the Deep bridge's enqueue,
// keyed): the chain advances only when the module call itself settles,
// even if the HTTP side already gave up on it, so a timed-out call never
// overlaps its successor inside the module.
const chains = new Map();
const noop = () => {};

function enqueueFor(key, fn, timeoutMs) {
  const prev = chains.get(key) ?? Promise.resolve();
  const task = prev.then(fn);
  const settled = task.then(noop, noop);
  chains.set(key, settled);
  settled.then(() => { if (chains.get(key) === settled) chains.delete(key); });
  let timer = null;
  const timeout = new Promise((_, reject) => {
    timer = setTimeout(() => {
      const e = new Error(`MiMo Studio did not answer within ${timeoutMs / 1000} seconds`);
      e.status = 504;
      reject(e);
    }, timeoutMs);
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

// ---------- secret censoring ----------

// This bridge, unlike Deep's, handles a real secret: the MiMo session
// cookie. Bridge stdout lands in the app log, and the app log is pasted
// into public bug reports, so EVERY log line and EVERY outgoing error
// message passes through censor() (via cleanMessage, the single funnel).
// The module puts the ph token in its request URLs, so a raw network
// error can carry it; the bridge itself never logs a URL. The secret set
// is the cookie's own values, re-read from the module's loader (the
// module re-reads the cookie file per call, so the set tracks rotation).
let secretValues = [];
function refreshSecrets() {
  try {
    const header = loadMiMoCookiesSync();
    const values = [];
    if (typeof header === 'string') {
      for (const part of header.split(';')) {
        const eq = part.indexOf('=');
        if (eq > 0) {
          const v = part.slice(eq + 1).trim().replace(/^"|"$/g, '');
          if (v.length >= 8) values.push(v);
        }
      }
    }
    if (values.length > 0) secretValues = values;
  } catch { /* no cookie yet: the pattern rules below still apply */ }
}

function censor(text) {
  let out = String(text ?? '');
  for (const v of secretValues) out = out.split(v).join('***');
  return out
    // No URL survives censoring: the module's URLs carry the ph token,
    // and upstream error bodies can embed signed login URLs. Neither
    // belongs in a log line or an error message, ever.
    .replace(/https?:\/\/\S+/g, '[url]')
    .replace(/xiaomichatbot_ph=[^&\s"']+/g, 'xiaomichatbot_ph=***')
    .replace(/xiaomichatbot_serviceToken=[^;\s"']+/g, 'xiaomichatbot_serviceToken=***')
    .replace(/cookie:\s*[^\n]+/gi, 'cookie: ***');
}

// Upstream error text carries the raw server body; forward a bounded,
// censored, tag-stripped version inside the OpenAI error shape, never
// the whole body. (Deep's cleanMessage is deliberately NOT copied as-is:
// it censors nothing, which was fine for a provider without secrets.)
function cleanMessage(err) {
  return censor(String(err?.message ?? err ?? 'unknown error'))
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
  // The module's own format is "MiMo chat error (401): ..." (and
  // "MiMo conversation/save error (401): ..."); a bare "HTTP 401" pattern
  // would miss it and disguise every upstream failure, a dead cookie
  // included, as a bridge-side 502.
  const raw = String(err?.message ?? '');
  const m = /error \((\d{3})\)/.exec(raw) ?? /HTTP (\d{3})/.exec(raw);
  if (m) {
    const s = Number(m[1]);
    if (s >= 400 && s < 600) return s;
  }
  return 502;
}

// ---------- chat flow ----------

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

  refreshSecrets();
  const model = body?.model;
  if (!modelSet.has(model)) {
    sendError(res, 404, `Unknown MiMo model: ${String(model ?? '')}`);
    return;
  }
  const messages = Array.isArray(body.messages) ? body.messages : [];

  const systemText = messages
    .filter((m) => m && m.role === 'system')
    .map((m) => textOf(m.content))
    .filter(Boolean)
    .join('\n\n')
    .trim();
  const userTexts = messages
    .filter((m) => m && m.role === 'user')
    .map((m) => textOf(m.content).trim())
    .filter(Boolean);
  if (userTexts.length === 0) {
    sendError(res, 400, 'No user message in the request');
    return;
  }
  const firstUser = userTexts[0];
  const lastUser = userTexts[userTexts.length - 1];

  const key = createHash('sha256').update(`${systemText}\n${firstUser}`).digest('hex');

  // The prompt that seeds a brand-new server conversation: system turns
  // folded in front of the first user message, plus the last user
  // message when it is a later turn (map miss / dead-id retry), front
  // kept inside the fold budget with a visible cut marker.
  const seedPrompt = () => {
    const parts = [];
    if (systemText) parts.push(systemText);
    parts.push(firstUser);
    if (lastUser !== firstUser) parts.push(lastUser);
    const folded = parts.join('\n\n');
    return folded.length > FOLD_MAX_CHARS ? folded.slice(0, FOLD_MAX_CHARS) + FOLD_MARKER : folded;
  };

  // Parameter mapping is explicit: OpenAI names it top_p, the module
  // only reads topP (and temperature); a wrong name would vanish
  // silently. Thinking and web search stay at the module's own defaults
  // for this provider (reasoning still comes back filled server-side).
  const options = { model, enableThinking: false, webSearchStatus: 'disabled' };
  if (typeof body.temperature === 'number' && !Number.isNaN(body.temperature)) options.temperature = body.temperature;
  if (typeof body.top_p === 'number' && !Number.isNaN(body.top_p)) options.topP = body.top_p;

  const started = Date.now();
  let outcome;
  try {
    outcome = await enqueueFor(key, async () => {
      const stored = convGet(key);
      if (stored) {
        try {
          const r = await askMiMoWeb(lastUser, { ...options, conversationId: stored });
          return { result: r, continued: true, retried: false };
        } catch (e) {
          // A stored id the server no longer knows (foreign, expired):
          // drop it and retry exactly once as a new conversation rather
          // than surfacing the raw error.
          console.log(`chat ${model}: stored conversation failed (${cleanMessage(e).slice(0, 120)}), retrying once as new`);
          convDelete(key);
          const r = await askMiMoWeb(seedPrompt(), options);
          if (r?.conversationId) convSet(key, r.conversationId);
          return { result: r, continued: false, retried: true };
        }
      }
      const r = await askMiMoWeb(seedPrompt(), options);
      if (r?.conversationId) convSet(key, r.conversationId);
      return { result: r, continued: false, retried: false };
    }, CHAT_TIMEOUT_MS);
  } catch (e) {
    console.log(`chat ${model} failed after ${Date.now() - started}ms: ${cleanMessage(e).slice(0, 160)}`);
    sendError(res, upstreamStatus(e), e?.message ?? 'MiMo Studio request failed');
    return;
  }

  const result = outcome.result;
  // Defensive result mapping, against the module's real field names:
  // the answer is `text`, there is no metadata field, and usage is the
  // raw SSE usage (camelCase, with a nativeUsage twin) or undefined.
  const reply = typeof result?.text === 'string' ? result.text : '';
  const reasoning = typeof result?.reasoning === 'string' && result.reasoning ? result.reasoning : null;
  const u = result?.usage ?? {};
  const nat = u.nativeUsage ?? {};
  const num = (v) => (Number.isFinite(v) ? v : 0);
  const promptTokens = num(u.promptTokens ?? nat.prompt_tokens);
  const completionTokens = num(u.completionTokens ?? nat.completion_tokens);
  const usage = {
    prompt_tokens: promptTokens,
    completion_tokens: completionTokens,
    total_tokens: num(u.totalTokens ?? nat.total_tokens) || promptTokens + completionTokens,
  };
  console.log(
    `chat ${model} ok in ${Date.now() - started}ms, conv=${outcome.retried ? 'retry-new' : outcome.continued ? 'lanjutan' : 'baru'}, promptLen=${outcome.continued ? lastUser.length : seedPrompt().length}`,
  );
  const id = `chatcmpl-${Date.now().toString(36)}${Math.floor(Math.random() * 1e6).toString(36)}`;
  const created = Math.floor(Date.now() / 1000);

  if (body.stream === true) {
    // The module answers whole, never streamed: synthesize the SSE form,
    // one content chunk (reasoning rides as reasoning_content, which the
    // module returns even with thinking off) and the finish chunk, then
    // [DONE].
    res.writeHead(200, { 'Content-Type': 'text/event-stream', 'Cache-Control': 'no-cache', Connection: 'keep-alive' });
    const base = { id, object: 'chat.completion.chunk', created, model };
    const delta = { role: 'assistant', content: reply };
    if (reasoning) delta.reasoning_content = reasoning;
    res.write(`data: ${JSON.stringify({ ...base, choices: [{ index: 0, delta, finish_reason: null }] })}\n\n`);
    res.write(`data: ${JSON.stringify({ ...base, choices: [{ index: 0, delta: {}, finish_reason: 'stop' }], usage })}\n\n`);
    res.write('data: [DONE]\n\n');
    res.end();
    return;
  }

  const message = { role: 'assistant', content: reply };
  if (reasoning) message.reasoning_content = reasoning;
  sendJson(res, 200, {
    id,
    object: 'chat.completion',
    created,
    model,
    choices: [{ index: 0, message, finish_reason: 'stop' }],
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
      data: MODELS.map((m) => ({ id: m, object: 'model', created: 0, owned_by: 'mimo' })),
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
  refreshSecrets();
  console.log(`${SERVICE} listening on ${HOST}:${PORT} (${MODELS.length} models)`);
});
