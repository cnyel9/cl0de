/* Mimo Ai
   by: FruatreMaou
   tags: Ai
*/

/**
 * Xiaomi MiMo Studio Web Scraper (https://aistudio.xiaomimimo.com/#/c)
 *
 * Model yang aktif & terverifikasi:
 * - mimo-v2.6-pro (default)
 * - mimo-v2.6-flash
 * - mimo-v2.5-pro
 * - mimo-v2.5
 */

const crypto = require("node:crypto");
const fs = require("node:fs");
const path = require("node:path");

const MIMO_BASE = "https://aistudio.xiaomimimo.com";
const AVAILABLE_MODELS = [
  "mimo-v2.6-pro",
  "mimo-v2.6-flash",
  "mimo-v2.5-pro",
  "mimo-v2.5"
];
const DEFAULT_USER_AGENT =
  "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Mobile Safari/537.36";

function parseMiMoCookies(input) {
  const trimmed = String(input || "").trim();
  if (!trimmed) return "";

  if (trimmed.startsWith("[")) {
    try {
      const parsed = JSON.parse(trimmed);
      if (Array.isArray(parsed)) {
        return parsed
          .filter((c) => c && typeof c.name === "string" && typeof c.value === "string")
          .map((c) => `${c.name}=${c.value}`)
          .join("; ");
      }
    } catch {
      // Fall through to line/header parsing.
    }
  }

  if (!trimmed.includes("\t") && !trimmed.includes("\n") && trimmed.includes("=")) {
    return trimmed;
  }

  const cookies = [];
  for (const rawLine of trimmed.split("\n")) {
    const line = rawLine.trim();
    if (!line || (line.startsWith("#") && !line.startsWith("#HttpOnly_"))) continue;
    const cleanLine = line.startsWith("#HttpOnly_") ? line.slice(10) : line;
    const parts = cleanLine.split(/\s+/);
    if (parts.length >= 7) {
      cookies.push(`${parts[5]}=${parts.slice(6).join(" ")}`);
    }
  }
  return cookies.join("; ");
}

function loadMiMoCookiesSync() {
  if (process.env.MIMO_COOKIE) {
    return parseMiMoCookies(process.env.MIMO_COOKIE);
  }

  const candidatePaths = [
    path.join(__dirname, "mimocook.txt"),
    path.join(__dirname, "Mimocook.txt"),
    path.join(process.cwd(), "mimocook.txt"),
    path.join(process.cwd(), "Mimocook.txt"),
    "/data/data/com.termux/files/home/bismilah-main/Mimocook.txt",
    "/data/data/com.termux/files/home/storage/downloads/scrape/mimocook.txt"
  ];

  for (const p of candidatePaths) {
    try {
      if (fs.existsSync(p)) {
        const parsed = parseMiMoCookies(fs.readFileSync(p, "utf8"));
        if (parsed) return parsed;
      }
    } catch {
      // Try next candidate path.
    }
  }
  return "";
}

function extractPhToken(cookieHeader) {
  const match = cookieHeader.match(/(?:^|;\s*)xiaomichatbot_ph=([^;]+)/);
  if (!match) return "";
  let val = match[1].trim();
  if (val.startsWith('"') && val.endsWith('"') && val.length >= 2) {
    val = val.slice(1, -1);
  }
  try {
    return decodeURIComponent(val);
  } catch {
    return val;
  }
}

function buildHeaders(cookie) {
  return {
    "Content-Type": "application/json",
    Cookie: cookie,
    Origin: MIMO_BASE,
    Referer: `${MIMO_BASE}/`,
    "User-Agent": DEFAULT_USER_AGENT
  };
}

function resolveCookie(customCookie) {
  const cookie = customCookie ? parseMiMoCookies(customCookie) : loadMiMoCookiesSync();
  if (!cookie) {
    throw new Error("Cookie MiMo Studio tidak ditemukan. Isi mimocook.txt, env MIMO_COOKIE, atau options.cookies.");
  }
  const ph = extractPhToken(cookie);
  if (!ph) {
    throw new Error("Token 'xiaomichatbot_ph' tidak ditemukan di dalam cookie MiMo Studio.");
  }
  return { cookie, ph };
}

function randomHexId() {
  return crypto.randomBytes(16).toString("hex");
}

async function createConversation(cookie, ph, conversationId, title = "New conversation") {
  const url = `${MIMO_BASE}/open-apis/chat/conversation/save?xiaomichatbot_ph=${encodeURIComponent(ph)}`;
  const res = await fetch(url, {
    method: "POST",
    headers: buildHeaders(cookie),
    body: JSON.stringify({ conversationId, title, type: "chat" })
  });

  const raw = await res.text();
  if (!res.ok) {
    throw new Error(`MiMo conversation/save error (${res.status}): ${raw.slice(0, 300)}`);
  }

  try {
    const json = JSON.parse(raw);
    if (json && typeof json.code === "number" && json.code !== 0) {
      throw new Error(`MiMo conversation/save failed (code ${json.code}): ${json.msg || raw.slice(0, 300)}`);
    }
  } catch (err) {
    if (err instanceof Error && err.message.startsWith("MiMo conversation/save failed")) {
      throw err;
    }
  }
}

function mergeOverlappingChunks(chunks) {
  if (chunks.length === 0) return "";
  if (chunks.length === 1) return chunks[0];
  const first = chunks[0];
  const rest = chunks.slice(1).join("");
  const maxOverlap = Math.min(first.length, rest.length);
  for (let len = maxOverlap; len > 0; len--) {
    if (first.endsWith(rest.slice(0, len))) {
      return first + rest.slice(len);
    }
  }
  return first + rest;
}

function parseMiMoSSE(rawText) {
  const contentChunks = [];
  let reasoning = "";
  let dialogId;
  let usage;
  let errorMsg;

  let currentEvent = "message";
  for (const rawLine of rawText.split("\n")) {
    const line = rawLine.replace(/\r$/, "");
    if (!line) {
      currentEvent = "message";
      continue;
    }
    if (line.startsWith("event:")) {
      currentEvent = line.slice(6).trim();
      continue;
    }
    if (!line.startsWith("data:")) continue;

    const payload = line.slice(5).trim();
    if (!payload || payload === "[DONE]") continue;

    let data;
    try {
      data = JSON.parse(payload);
    } catch {
      continue;
    }

    if (currentEvent === "dialogId" && typeof data?.content === "string") {
      dialogId = data.content;
    } else if (currentEvent === "usage") {
      usage = data;
    } else if (currentEvent === "error") {
      errorMsg = data?.message || data?.msg || data?.content || JSON.stringify(data);
    } else if (currentEvent === "message") {
      if (typeof data?.reasoningContent === "string") {
        reasoning += data.reasoningContent;
      }
      if (typeof data?.content === "string") {
        contentChunks.push(data.content);
      }
      if (data?.error?.message) {
        errorMsg = data.error.message;
      }
    }
  }

  return {
    text: mergeOverlappingChunks(contentChunks).trim(),
    reasoning: reasoning.trim(),
    dialogId,
    usage,
    errorMsg
  };
}

/**
 * Send a prompt to Xiaomi MiMo Studio (aistudio.xiaomimimo.com).
 * @param {string} prompt
 * @param {object} [options]
 */
async function askMiMoWeb(prompt, options = {}) {
  const { cookie, ph } = resolveCookie(options.cookies);
  const model = options.model || "mimo-v2.6-pro";
  const enableThinking = options.enableThinking ?? false;
  const webSearchStatus = options.webSearchStatus || "disabled";

  const conversationId = options.conversationId || randomHexId();
  if (!options.conversationId) {
    await createConversation(cookie, ph, conversationId, options.title || "New conversation");
  }

  const msgId = randomHexId();
  const modelConfig = { enableThinking, webSearchStatus, model };
  if (typeof options.temperature === "number" && !Number.isNaN(options.temperature)) {
    modelConfig.temperature = options.temperature;
  }
  if (typeof options.topP === "number" && !Number.isNaN(options.topP)) {
    modelConfig.topP = options.topP;
  }

  const chatUrl = `${MIMO_BASE}/open-apis/react/chat?xiaomichatbot_ph=${encodeURIComponent(ph)}`;
  const res = await fetch(chatUrl, {
    method: "POST",
    headers: buildHeaders(cookie),
    body: JSON.stringify({
      msgId,
      conversationId,
      query: prompt,
      isEditedQuery: false,
      modelConfig,
      multiMedias: []
    })
  });

  if (!res.ok) {
    const errText = await res.text();
    throw new Error(`MiMo chat error (${res.status}): ${errText.slice(0, 400)}`);
  }

  let rawText = "";
  if (res.body) {
    const reader = res.body.getReader();
    const decoder = new TextDecoder();
    while (true) {
      const { done, value } = await reader.read();
      if (done) break;
      const chunk = decoder.decode(value, { stream: true });
      rawText += chunk;
      if (chunk.includes('"content":"[DONE]"') || chunk.includes("event:finish\n")) {
        await reader.cancel().catch(() => {});
        break;
      }
    }
    rawText += decoder.decode();
  } else {
    rawText = await res.text();
  }

  const parsed = parseMiMoSSE(rawText);
  if (parsed.errorMsg && !parsed.text) {
    throw new Error(`MiMo stream error: ${parsed.errorMsg}`);
  }
  if (!parsed.text) {
    throw new Error("MiMo Studio tidak mengembalikan teks respons.");
  }

  return {
    text: parsed.text,
    ...(parsed.reasoning ? { reasoning: parsed.reasoning } : {}),
    model,
    conversationId,
    msgId,
    dialogId: parsed.dialogId,
    usage: parsed.usage
  };
}

module.exports = {
  AVAILABLE_MODELS,
  askMiMoWeb,
  parseMiMoCookies,
  loadMiMoCookiesSync
};

if (require.main === module) {
  const prompt = process.argv.slice(2).join(" ").trim() || "Halo MiMo, jawab singkat: OK";
  askMiMoWeb(prompt)
    .then((res) => console.log(JSON.stringify(res, null, 2)))
    .catch((err) => {
      console.error("Error:", err.message);
      process.exit(1);
    });
}
