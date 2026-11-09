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
import { createHash } from 'node:crypto';
import { chat, claimReward, generateImage, getConfig, imageToImage } from './deepchat.mjs';

const HOST = '127.0.0.1';
const PORT = 8181;
const SERVICE = 'cl0de-deep-bridge';
const CHAT_TIMEOUT_MS = 120_000;
const MAX_BODY_BYTES = 32 * 1024 * 1024;
// DeepChat rejects a systemPrompt longer than this with HTTP 400
// ("systemPrompt exceeds maximum length of 4000 characters").
const SYSTEM_PROMPT_MAX_CHARS = 4000;
// Bridge-side credit refill (second layer: the module already claims up to
// five times and retries once internally before a 402 reaches us). At most
// five more claims inside a ~6 s wall budget, then exactly one chat retry.
// The whole flow (chat 120 s + refill ~6 s + retry 120 s = 246 s worst case)
// stays below the app gate's 300 s, so the flow race is 250 s, not 120 s.
const REFILL_MAX_CLAIMS = 5;
const REFILL_WALL_MS = 6_000;
const REFILL_GAP_MS = 150;
const CHAT_FLOW_TIMEOUT_MS = 250_000;
// Image route budget: one generation attempt, the optional refill (~6 s)
// and one retry, mirroring the chat flow's reasoning.
const IMAGE_FLOW_TIMEOUT_MS = 250_000;
// Image relay for text-only models: one gpt-4o-mini call describes the image
// and the description travels as text. Its own shorter race, and successful
// descriptions are cached per image (the fallback scan would otherwise pay
// for the same description again on every follow-up request).
const RELAY_TIMEOUT_MS = 40_000;
const RELAY_MAX_CHARS = 1000;
const RELAY_CACHE_MAX = 20;
const RELAY_PROMPT =
  'Deskripsikan gambar ini secara faktual dan rinci dalam Bahasa Indonesia: sebutkan objek utama, suasana, warna dominan, dan tata letaknya. ' +
  'Salin persis semua teks yang terbaca di dalam gambar. Tanpa opini, tanpa sapaan, dan tanpa penjelasan bahwa kamu adalah AI. Maksimal 120 kata.';
const RELAY_FAIL_BLOCK = '[Pengguna melampirkan sebuah gambar, tetapi gambar itu tidak dapat dideskripsikan saat ini.]';
// Image acceptance gate: only data URLs with a whitelisted image mime may
// reach the module (it would treat any other string as a local file path or
// an unbounded remote fetch). ~8M chars of data URL is the size cap.
const IMAGE_MIMES = new Set(['image/jpeg', 'image/png', 'image/webp', 'image/gif']);
const MAX_IMAGE_CHARS = 8_000_000;
// How many user turns back (including the last one) the image fallback scans.
const IMAGE_SCAN_TURNS = 3;
// Sentinel comments the app wraps around its compact Deep memory block.
const MEMORY_START = '<!-- cl0de-deep-memory:start -->';
const MEMORY_END = '<!-- cl0de-deep-memory:end -->';

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

// Image models live OUTSIDE the chat catalog on purpose: refreshCatalog()
// rebuilds `catalog` from getConfig() with a text_chat filter and would
// wipe anything parked inside it. They merge into /v1/models answers and
// are intercepted in handleChat before the chat entry lookup (an unknown
// id there is a 404). The OpenCode-side id carries no slash; the module's
// own provider/model pair rides in the call options.
const IMAGE_MODELS = [
  { providerId: 'flux', moduleModelId: 'flux/schnell', id: 'flux-schnell', displayName: 'Flux Schnell' },
];
const imageIndex = new Map(IMAGE_MODELS.map((m) => [m.id, m]));

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

// One candidate image string through the strict gate. Success returns
// { url, via, mime }; rejection returns { dropped: reason } and never fails
// the request: the turn is treated as having no image, with the reason kept
// for the diagnostic log line.
function gatedImage(candidate, via, partMime) {
  if (typeof candidate !== 'string' || !candidate) return { dropped: 'not-data' };
  if (!candidate.startsWith('data:image/')) return { dropped: 'not-data' };
  const m = /^data:([^;,]+)/.exec(candidate);
  const mime = m ? m[1].toLowerCase() : '';
  if (!IMAGE_MIMES.has(mime)) return { dropped: 'bad-mime' };
  if (partMime && !IMAGE_MIMES.has(String(partMime).toLowerCase())) return { dropped: 'bad-mime' };
  if (candidate.length > MAX_IMAGE_CHARS) return { dropped: 'too-large' };
  return { url: candidate, via, mime };
}

// First accepted image of a message. The app's own attachment parts are
// OpenCode file parts ({ type: 'file', mime, url: 'data:...' }), so those are
// the main catch after the classic image_url shape; 'image' parts and a
// file part's file_data are lower-probability shapes kept for safety.
function findImageIn(content) {
  if (!Array.isArray(content)) return { image: null, dropped: null };
  const buckets = [[], [], [], []];
  for (const p of content) {
    if (!p) continue;
    if (p.type === 'image_url' && typeof p.image_url?.url === 'string' && p.image_url.url) {
      buckets[0].push([p.image_url.url, 'image_url', null]);
    } else if (p.type === 'file' && typeof p.url === 'string' && typeof p.mime === 'string' && p.mime.toLowerCase().startsWith('image/')) {
      buckets[1].push([p.url, 'file', p.mime]);
    } else if (p.type === 'image') {
      const u = typeof p.image === 'string' ? p.image
        : typeof p.image?.url === 'string' ? p.image.url
        : typeof p.url === 'string' ? p.url
        : typeof p.data === 'string' ? p.data
        : null;
      if (u) buckets[2].push([u, 'image', typeof p.mime === 'string' ? p.mime : null]);
    }
    if (p.type === 'file' && typeof p.file_data === 'string') {
      buckets[3].push([p.file_data, 'file', typeof p.mime === 'string' ? p.mime : null]);
    }
  }
  let dropped = null;
  for (const bucket of buckets) {
    for (const [url, via, partMime] of bucket) {
      const g = gatedImage(url, via, partMime);
      if (g.url) return { image: g, dropped };
      if (!dropped) dropped = g.dropped;
    }
  }
  return { image: null, dropped };
}

// ---------- serialized module calls with a bridge-side timeout ----------

let chain = Promise.resolve();
const noop = () => {};

// One link of the single-flight chain. The link body runs module calls
// directly; it must NEVER call enqueue/callModule from inside itself (the
// chain would wait on itself and every later Deep chat would hang).
function enqueue(fn, timeoutMs) {
  const task = chain.then(fn);
  // The queue advances only when the module call itself settles, even if the
  // HTTP side already gave up on it: the module session must never see two
  // calls overlap.
  chain = task.then(noop, noop);
  let timer = null;
  const timeout = new Promise((_, reject) => {
    timer = setTimeout(() => {
      const e = new Error(`DeepChat did not answer within ${timeoutMs / 1000} seconds`);
      e.status = 504;
      reject(e);
    }, timeoutMs);
    timer.unref?.();
  });
  return Promise.race([task, timeout]).finally(() => clearTimeout(timer));
}

function callModule(message, options, timeoutMs = CHAT_TIMEOUT_MS) {
  return enqueue(() => chat(message, options), timeoutMs);
}

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

// Only a real credit shortage triggers the refill: an upstream 402 whose raw
// error body names the shortage. A 401 (session trouble) never does; the
// module's own bootstrap/claim flow owns that case.
function isCreditError(err) {
  if (upstreamStatus(err) !== 402) return false;
  const raw = String(err?.message ?? '');
  return raw.includes('insufficient_credits') || raw.includes('Insufficient credits');
}

// The "~N" figure in "Required: ~7, available: 6" is an estimate; it is only
// the yardstick for stopping the claims early once the balance covers it.
function requiredCredits(err) {
  const m = /Required:\s*~?\s*(\d+(?:\.\d+)?)/.exec(String(err?.message ?? ''));
  return m ? Number(m[1]) : null;
}

// Runs INSIDE a flow's chain link: claim a few times, then retry the call
// once. Gives up (rethrowing the original 402 untouched) when two
// claims in a row fail, when the daily ad quota is spent with the balance
// still short, or when the wall budget runs out before any claim succeeded.
async function refillThenRetry(firstError, retryFn, label = 'chat') {
  const required = requiredCredits(firstError);
  const started = Date.now();
  let claims = 0;
  let consecutiveFails = 0;
  let anySuccess = false;
  let lastBalance = null;
  let giveUp = false;
  const balanceMet = () => required != null && lastBalance != null && lastBalance >= required;
  while (claims < REFILL_MAX_CLAIMS) {
    if (Date.now() - started > REFILL_WALL_MS) {
      if (!anySuccess) giveUp = true;
      break;
    }
    let r = null;
    try { r = await claimReward(); } catch { r = null; }
    claims++;
    if (r && r.success) {
      anySuccess = true;
      consecutiveFails = 0;
      if (Number.isFinite(r.newBalance)) lastBalance = r.newBalance;
      if (balanceMet()) break;
      if (r.adsRemainingToday === 0) { giveUp = true; break; }
    } else {
      consecutiveFails++;
      if (consecutiveFails >= 2) { giveUp = true; break; }
    }
    if (Date.now() - started > REFILL_WALL_MS) {
      if (!anySuccess) giveUp = true;
      break;
    }
    await sleep(REFILL_GAP_MS);
  }
  if (giveUp || !anySuccess) {
    console.log(`credit refill: gave up after ${claims} claims in ${Date.now() - started}ms, surfacing the original 402`);
    throw firstError;
  }
  console.log(
    `credit refill: ${claims} claims in ${Date.now() - started}ms, balance=${lastBalance ?? '?'}, required~=${required ?? '?'}, retrying ${label} once`,
  );
  return retryFn();
}

// A chat send as ONE chain link: the first attempt, the optional refill and
// the single retry all live inside the same link, so the module session
// never sees them overlap with another request's calls.
function callChatFlow(message, options) {
  return enqueue(async () => {
    try {
      return await chat(message, options);
    } catch (e) {
      if (!isCreditError(e)) throw e;
      console.log(`chat ${options.modelId} hit insufficient credits, starting bridge refill`);
      return refillThenRetry(e, () => chat(message, options));
    }
  }, CHAT_FLOW_TIMEOUT_MS);
}

// ---------- image generation route ----------

// One image call as ONE chain link (the module session is shared with
// chat, so generations serialize with it): the first attempt, the
// optional refill and the single retry all live inside the same link.
function callImageFlow(mod, prompt, imageUrl) {
  const attempt = () => imageUrl
    ? imageToImage(prompt, imageUrl, { providerId: mod.providerId, modelId: mod.moduleModelId })
    : generateImage(prompt, { providerId: mod.providerId, modelId: mod.moduleModelId });
  return enqueue(async () => {
    try {
      return await attempt();
    } catch (e) {
      if (!isCreditError(e)) throw e;
      console.log(`image ${mod.id} hit insufficient credits, starting bridge refill`);
      return refillThenRetry(e, attempt, 'image');
    }
  }, IMAGE_FLOW_TIMEOUT_MS);
}

// The flux-schnell route: stateless, the last user turn only (no system
// prompt, no memory block, no history, never the vision/relay logic). The
// prompt is that turn's text; an attached image (the chat scan's data URL,
// exactly the string form the module accepts) turns the call into
// imageToImage. The reply text is one opener line plus the image as
// markdown on its own line, pointing at the service's own https URL: the
// module's result bytes are never downloaded here. Every failure becomes
// a clear Indonesian reply text, never a raw upstream error.
async function runImageRoute(mod, prompt, imageUrl, started) {
  const route = imageUrl ? 'image-to-image' : 'image-generate';
  if (!prompt && !imageUrl) {
    console.log(`image route=${route} skipped in 0ms: no prompt text`);
    return { reply: 'Tulis dulu gambar apa yang ingin dibuat, lalu kirim lagi.' };
  }
  const text = prompt || 'Improve this image: keep the same subject and composition, cleaner and more detailed.';
  let result;
  try {
    result = await callImageFlow(mod, text, imageUrl);
  } catch (e) {
    console.log(`image route=${route} failed in ${Date.now() - started}ms: ${cleanMessage(e).slice(0, 160)}`);
    return {
      reply: isCreditError(e)
        ? 'Maaf, kredit DeepChat sedang habis dan isi ulang otomatisnya belum berhasil. Coba lagi sebentar lagi.'
        : 'Maaf, gambarnya gagal dibuat saat ini. Coba ulangi sebentar lagi, atau ubah sedikit perintahnya.',
    };
  }
  const first = Array.isArray(result?.images) ? result.images[0] : null;
  const url = typeof first === 'string' ? first : (typeof first?.url === 'string' ? first.url : null);
  if (!url) {
    console.log(`image route=${route} empty in ${Date.now() - started}ms, creditsUsed=${result?.creditsUsed ?? '?'}`);
    return { reply: 'Maaf, layanan gambar tidak mengembalikan gambar apa pun kali ini. Coba ulangi sebentar lagi.' };
  }
  let host = '?';
  try { host = new URL(url).host; } catch { /* the URL is forwarded as-is either way */ }
  console.log(
    `image route=${route} ok in ${Date.now() - started}ms, creditsUsed=${result?.creditsUsed ?? '?'}, creditsRemaining=${result?.creditsRemaining ?? '?'}, host=${host}`,
  );
  const opener = imageUrl ? 'Ini gambar hasil ubahannya:' : 'Ini gambar hasil buatannya:';
  return { reply: `${opener}\n\n![gambar](${url})`, creditsUsed: result?.creditsUsed, creditsRemaining: result?.creditsRemaining };
}

// ---------- image relay (text-only models) ----------

const relayCache = new Map();

// One clean module call (image only: no system prompt, no history) through
// the same chain, with the relay's own shorter race. Successful descriptions
// are cached by the image's hash; a follow-up request about the same image
// pays nothing. Throws on failure: the caller softens it to a note.
async function describeImage(imageUrl) {
  const key = createHash('sha256').update(imageUrl).digest('hex');
  const hit = relayCache.get(key);
  if (hit) {
    console.log('relay: description from cache, no module call (0 credits)');
    return hit;
  }
  const started = Date.now();
  const result = await callModule(RELAY_PROMPT, { providerId: 'chatgpt', modelId: 'gpt-4o-mini', image: imageUrl }, RELAY_TIMEOUT_MS);
  const desc = (typeof result?.reply === 'string' ? result.reply : '').trim().slice(0, RELAY_MAX_CHARS);
  console.log(
    `relay gpt-4o-mini ok in ${Date.now() - started}ms, creditsUsed=${result?.creditsUsed ?? '?'}, creditsRemaining=${result?.creditsRemaining ?? '?'}`,
  );
  if (desc) {
    relayCache.set(key, desc);
    if (relayCache.size > RELAY_CACHE_MAX) relayCache.delete(relayCache.keys().next().value);
  }
  return desc;
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

  const imgMod = imageIndex.get(body?.model);
  const entry = imgMod ?? index.get(body?.model);
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
  // The app's compact Deep memory block travels inside the merged text,
  // wrapped in sentinel comments, wherever OpenCode placed it. Lift it out
  // and re-insert it right after the first segment: the agent prompt (the
  // behaviour instructions) stays at the head, the memory (data) follows it,
  // and both survive the cap below. A lopsided sentinel pair is ignored
  // completely, and only the first complete pair is lifted.
  let systemPrompt = mergedSystem;
  {
    const a = mergedSystem.indexOf(MEMORY_START);
    const b = mergedSystem.indexOf(MEMORY_END);
    if (a >= 0 && b > a) {
      const block = mergedSystem.slice(a + MEMORY_START.length, b).trim();
      const rest = (mergedSystem.slice(0, a) + mergedSystem.slice(b + MEMORY_END.length))
        .split('\n\n').map((s) => s.trim()).filter(Boolean).join('\n\n');
      if (block) {
        if (!rest) systemPrompt = block;
        else {
          const cut = rest.indexOf('\n\n');
          systemPrompt = cut < 0
            ? `${block}\n\n${rest}`
            : `${rest.slice(0, cut)}\n\n${block}\n\n${rest.slice(cut + 2)}`;
        }
        console.log(`deep memory block placed in systemPrompt: ${block.length} chars`);
      }
    }
  }
  // The merged text is capped at the upstream limit, keeping the front: the
  // agent prompt comes first, environment context and AGENTS.md follow and
  // are what gets cut. This also covers a single oversized system turn.
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
  // One image slot, found by scanning user turns backwards over a bounded
  // window (the last three user turns): a follow-up question about an image
  // from a moment ago still carries the image. Newest wins; outside the
  // window the user re-attaches. The scan also feeds the diagnostic line:
  // on 'none' it lists the part types it actually saw.
  let found = { image: null, dropped: null };
  let imageFrom = -1;
  const scannedTypes = [];
  {
    let seen = 0;
    for (let i = lastUser; i >= 0 && seen < IMAGE_SCAN_TURNS; i--) {
      const m = messages[i];
      if (!m || m.role !== 'user') continue;
      seen++;
      if (Array.isArray(m.content)) {
        for (const p of m.content) {
          const t = p && typeof p.type === 'string' ? p.type : '?';
          if (!scannedTypes.includes(t)) scannedTypes.push(t);
        }
      } else if (!scannedTypes.includes('text')) {
        scannedTypes.push('text');
      }
      const r = findImageIn(m.content);
      if (r.dropped && !found.dropped) found.dropped = r.dropped;
      if (r.image) { found.image = r.image; imageFrom = seen - 1; break; }
    }
  }
  console.log(
    `chat ${entry.id} ` +
    (found.image
      ? `image=${imageFrom === 0 ? 'last' : `fallback-${imageFrom}`} via=${found.image.via} mime=${found.image.mime} imgKB=${Math.max(1, Math.round((found.image.url.length * 0.75) / 1024))}`
      : `image=none parts=[${scannedTypes.join(',')}]`) +
    (found.dropped ? ` imageDropped=${found.dropped}` : ''),
  );

  const started = Date.now();
  let result;
  if (imgMod) {
    // Image model: its own route, fed by the scan above and nothing else
    // (no system prompt, no memory block, no history, no vision/relay pass).
    result = await runImageRoute(imgMod, message, found.image?.url ?? null, started);
  } else {
    let image = null;
    if (entry.image) {
      // Vision models keep the direct path: the image rides as the module's
      // one image slot.
      image = found.image?.url ?? null;
      // The module throws on an empty message: an image-only turn gets a
      // placeholder text instead.
      if (!message && image) message = 'Describe this image.';
    } else if (found.image) {
      // Text-only models cannot see: a vision model describes the image and
      // the description is prepended as text. A failed relay never fails the
      // chat; a short note takes the description's place.
      let block;
      try {
        const desc = await describeImage(found.image.url);
        block = desc
          ? `[Pengguna melampirkan sebuah gambar. Kamu tidak dapat melihat gambar secara langsung; yang berikut ini adalah deskripsi isi gambar tersebut, dibuat oleh model vision: "${desc}"]`
          : RELAY_FAIL_BLOCK;
      } catch (e) {
        console.log(`relay failed, continuing without a description: ${cleanMessage(e).slice(0, 160)}`);
        block = RELAY_FAIL_BLOCK;
      }
      message = message ? `${block}\n\n${message}` : (block === RELAY_FAIL_BLOCK ? block : `${block}\n\nJelaskan gambar ini.`);
    }
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

    try {
      result = await callChatFlow(message, options);
    } catch (e) {
      console.log(`chat ${entry.id} failed after ${Date.now() - started}ms: ${cleanMessage(e).slice(0, 160)}`);
      sendError(res, upstreamStatus(e), e?.message ?? 'DeepChat request failed');
      return;
    }
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
      data: [...catalog, ...IMAGE_MODELS].map((m) => ({ id: m.id, object: 'model', created: 0, owned_by: 'deep' })),
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
