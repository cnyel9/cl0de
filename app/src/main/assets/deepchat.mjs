import { createHmac, createHash, randomUUID } from 'node:crypto';
import { existsSync, readFileSync } from 'node:fs';

const BASE_URL = 'https://uebueqfehjrruiwrbmgs.supabase.co';
const ANON_KEY = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InVlYnVlcWZlaGpycnVpd3JibWdzIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzI4NTI0NDYsImV4cCI6MjA4ODQyODQ0Nn0.pC43b2gMMx44q7_SUfJDM_ceLaYWeMzKI4QvMwz_AxU';

let cachedSession = null;

function buildSignedHeaders(endpoint, bodyStr, session) {
  const timestampSec = Math.floor(Date.now() / 1000);
  const nonce = randomUUID();
  const idempotencyKey = randomUUID();
  const bodySha256 = createHash('sha256').update(bodyStr, 'utf-8').digest('hex');
  const message = `POST\n${endpoint}\n${timestampSec}\n${nonce}\n${bodySha256}`;
  const secretBytes = Buffer.from(session.signingSecret, 'hex');
  const signature = createHmac('sha256', secretBytes).update(message, 'utf-8').digest('hex');

  return {
    'Content-Type': 'application/json',
    'apikey': ANON_KEY,
    'Authorization': `Bearer ${session.accessToken}`,
    'X-Request-Timestamp': String(timestampSec),
    'X-Idempotency-Key': idempotencyKey,
    'X-Request-Signature': signature,
    'X-Request-Nonce': nonce,
    'User-Agent': 'Ktor client'
  };
}

async function bootstrap() {
  const signupRes = await fetch(`${BASE_URL}/auth/v1/signup`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'apikey': ANON_KEY,
      'Authorization': `Bearer ${ANON_KEY}`,
      'User-Agent': 'Ktor client'
    },
    body: '{}'
  });

  if (!signupRes.ok) {
    throw new Error(`Anonymous auth failed: HTTP ${signupRes.status}`);
  }

  const signupData = await signupRes.json();
  const accessToken = signupData.access_token;
  const anonUserId = signupData.user?.id;

  const randomAndroidId = Array.from({ length: 16 }, () => Math.floor(Math.random() * 16).toString(16)).join('');
  const installIdHash = createHash('sha256').update(randomAndroidId).digest('hex');

  const createRes = await fetch(`${BASE_URL}/functions/v1/create-user`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'apikey': ANON_KEY,
      'Authorization': `Bearer ${accessToken}`,
      'User-Agent': 'Ktor client'
    },
    body: JSON.stringify({ anonUserId, installIdHash })
  });

  if (!createRes.ok) {
    throw new Error(`User provisioning failed: HTTP ${createRes.status}`);
  }

  const createData = await createRes.json();
  const newSession = {
    accessToken,
    anonUserId,
    signingSecret: createData.signingSecret
  };

  const initialBalance = createData.credits?.balance ?? 0;
  if (initialBalance < 1) {
    const claimHeaders = buildSignedHeaders('claim-ad-reward', '{}', newSession);
    await fetch(`${BASE_URL}/functions/v1/claim-ad-reward`, {
      method: 'POST',
      headers: claimHeaders,
      body: '{}'
    });
  }

  cachedSession = newSession;
  return cachedSession;
}

async function requestEndpoint(endpoint, payload, isRetry = false) {
  if (!cachedSession) {
    await bootstrap();
  }

  const bodyStr = JSON.stringify(payload);
  const headers = buildSignedHeaders(endpoint, bodyStr, cachedSession);

  let res = await fetch(`${BASE_URL}/functions/v1/${endpoint}`, {
    method: 'POST',
    headers,
    body: bodyStr
  });

  if ((res.status === 401 || res.status === 402) && !isRetry) {
    for (let i = 0; i < 5; i++) {
      const claimRes = await fetch(`${BASE_URL}/functions/v1/claim-ad-reward`, {
        method: 'POST',
        headers: buildSignedHeaders('claim-ad-reward', '{}', cachedSession),
        body: '{}'
      });
      const claimData = await claimRes.json().catch(() => null);
      if (!claimData || !claimData.success) {
        await bootstrap();
        break;
      }
    }
    return requestEndpoint(endpoint, payload, true);
  }

  if (!res.ok) {
    const errorText = await res.text();
    throw new Error(`[${endpoint}] HTTP ${res.status}: ${errorText}`);
  }

  return await res.json();
}

async function resolveImageData(imageInput) {
  if (!imageInput) return null;

  if (typeof imageInput === 'string') {
    if (imageInput.startsWith('data:')) {
      const parts = imageInput.split(',');
      const mime = parts[0].match(/data:(.*?);/)?.[1] || 'image/jpeg';
      return { base64: parts[1], mimeType: mime };
    }
    if (existsSync(imageInput)) {
      const buf = readFileSync(imageInput);
      const mime = imageInput.endsWith('.png') ? 'image/png' : (imageInput.endsWith('.webp') ? 'image/webp' : 'image/jpeg');
      return { base64: buf.toString('base64'), mimeType: mime };
    }
    if (imageInput.startsWith('http://') || imageInput.startsWith('https://')) {
      const resp = await fetch(imageInput);
      if (!resp.ok) throw new Error(`Failed to fetch image: HTTP ${resp.status}`);
      const ab = await resp.arrayBuffer();
      const mime = resp.headers.get('content-type') || 'image/jpeg';
      return { base64: Buffer.from(ab).toString('base64'), mimeType: mime };
    }
    return { base64: imageInput, mimeType: 'image/jpeg' };
  }

  if (Buffer.isBuffer(imageInput) || imageInput instanceof Uint8Array) {
    return { base64: Buffer.from(imageInput).toString('base64'), mimeType: 'image/jpeg' };
  }

  return null;
}

export async function chat(message, options = {}) {
  if (!message || typeof message !== 'string') {
    throw new Error('Message is required and must be a string');
  }

  const payload = {
    message: message.trim(),
    providerId: options.providerId || 'chatgpt',
    modelId: options.modelId || 'gpt-4o-mini'
  };

  if (options.systemPrompt) payload.systemPrompt = options.systemPrompt;
  if (Array.isArray(options.conversationHistory)) payload.conversationHistory = options.conversationHistory;
  if (options.categoryId) payload.categoryId = options.categoryId;
  if (options.subcategoryId) payload.subcategoryId = options.subcategoryId;
  if (options.assistantId) payload.assistantId = options.assistantId;
  if (options.deepThink) payload.deepThink = true;
  if (options.deepResearch) payload.deepResearch = true;
  if (options.writeCode) payload.writeCode = true;

  if (options.image) {
    const imgData = await resolveImageData(options.image);
    if (imgData) {
      payload.imageBase64 = imgData.base64;
      payload.imageMimeType = imgData.mimeType;
    }
  }

  const data = await requestEndpoint('send-message', payload);
  return {
    reply: data.reply || '',
    provider: data.provider || payload.providerId,
    model: data.model || payload.modelId,
    creditsUsed: data.creditsUsed ?? 0,
    creditsRemaining: data.creditsRemaining ?? 0,
    creditsExpiry: data.creditsExpiry || null,
    metadata: data.metadata || null
  };
}

export async function humanize(text, options = {}) {
  if (!text || typeof text !== 'string') {
    throw new Error('Text is required');
  }

  const payload = {
    text: text.trim(),
    providerId: options.providerId || 'chatgpt',
    modelId: options.modelId || 'gpt-4o-mini',
    intensity: options.intensity || 'medium'
  };

  const data = await requestEndpoint('humanize-text', payload);
  return {
    humanizedText: data.humanizedText || '',
    provider: data.provider || payload.providerId,
    model: data.model || payload.modelId,
    intensity: data.intensity || payload.intensity,
    creditsUsed: data.creditsUsed ?? 0,
    creditsRemaining: data.creditsRemaining ?? 0
  };
}

export async function optimizePrompt(prompt, options = {}) {
  if (!prompt || typeof prompt !== 'string') {
    throw new Error('Prompt is required');
  }

  const payload = {
    prompt: prompt.trim(),
    context: options.context || 'generic',
    mode: options.mode || 'optimize'
  };

  const data = await requestEndpoint('optimize-prompt', payload);
  return {
    optimizedPrompt: data.optimizedPrompt || '',
    creditsUsed: data.creditsUsed ?? 0,
    creditsRemaining: data.creditsRemaining ?? 0,
    metadata: data.metadata || null
  };
}

export async function generateImage(prompt, options = {}) {
  if (!prompt || typeof prompt !== 'string') {
    throw new Error('Prompt is required');
  }

  const payload = {
    prompt: prompt.trim(),
    providerId: options.providerId || 'flux',
    modelId: options.modelId || 'flux/schnell',
    size: options.size || '1024x1024',
    quality: options.quality || 'standard',
    n: options.n || 1
  };

  if (options.referenceImage) {
    const imgData = await resolveImageData(options.referenceImage);
    if (imgData) {
      payload.referenceImageBase64 = imgData.base64;
    }
  }

  const data = await requestEndpoint('generate-image', payload);
  return {
    images: data.images || [],
    provider: data.provider || payload.providerId,
    model: data.model || payload.modelId,
    creditsUsed: data.creditsUsed ?? 0,
    creditsRemaining: data.creditsRemaining ?? 0
  };
}

export async function imageToImage(prompt, imageInput, options = {}) {
  if (!prompt || typeof prompt !== 'string') {
    throw new Error('Prompt is required');
  }
  if (!imageInput) {
    throw new Error('Image input is required (file path, URL, or Buffer)');
  }

  const imgData = await resolveImageData(imageInput);
  if (!imgData) {
    throw new Error('Invalid image input');
  }

  const payload = {
    prompt: prompt.trim(),
    providerId: options.providerId || 'flux',
    modelId: options.modelId || 'flux/schnell',
    size: options.size || '1024x1024',
    quality: options.quality || 'standard',
    n: options.n || 1,
    referenceImageBase64: imgData.base64
  };

  const data = await requestEndpoint('generate-image', payload);
  return {
    images: data.images || [],
    provider: data.provider || payload.providerId,
    model: data.model || payload.modelId,
    creditsUsed: data.creditsUsed ?? 0,
    creditsRemaining: data.creditsRemaining ?? 0
  };
}

export async function editImage(prompt, imageInput, options = {}) {
  if (!prompt || typeof prompt !== 'string') {
    throw new Error('Prompt is required');
  }
  if (!imageInput) {
    throw new Error('Image input is required');
  }

  const imgData = await resolveImageData(imageInput);
  if (!imgData) {
    throw new Error('Invalid image input');
  }

  let maskBase64 = 'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAAAAAA6fptVAAAACklEQVR4nGNiAAAABgADNjd8qAAAAABJRU5ErkJggg==';
  if (options.mask) {
    const maskData = await resolveImageData(options.mask);
    if (maskData) maskBase64 = maskData.base64;
  }

  const payload = {
    prompt: prompt.trim(),
    imageBase64: imgData.base64,
    maskBase64,
    size: options.size || '1024x1024',
    n: options.n || 1,
    outputFormat: options.outputFormat || 'jpeg'
  };

  const data = await requestEndpoint('edit-image', payload);
  return {
    images: data.images || [],
    creditsUsed: data.creditsUsed ?? 0,
    creditsRemaining: data.creditsRemaining ?? 0
  };
}

export async function faceSwap(sourceImage, faceImage, options = {}) {
  if (!sourceImage || !faceImage) {
    throw new Error('sourceImage and faceImage are required');
  }

  const srcData = await resolveImageData(sourceImage);
  const faceData = await resolveImageData(faceImage);
  if (!srcData || !faceData) {
    throw new Error('Invalid source or face image');
  }

  const payload = {
    sourceImageData: srcData.base64,
    sourceFormat: options.sourceFormat || 'jpeg',
    faceImageData: faceData.base64,
    faceFormat: options.faceFormat || 'jpeg',
    generateVariant: Boolean(options.generateVariant),
    replaceAll: Boolean(options.replaceAll),
    providerId: options.providerId || 'chatgpt',
    modelId: options.modelId || 'gpt-image-1'
  };

  const data = await requestEndpoint('face-swap', payload);
  return {
    original: data.original || '',
    variant: data.variant || '',
    creditsUsed: data.creditsUsed ?? 0,
    creditsRemaining: data.creditsRemaining ?? 0,
    creditsExpiry: data.creditsExpiry || null
  };
}

export async function claimReward() {
  const data = await requestEndpoint('claim-ad-reward', {});
  return {
    success: Boolean(data.success),
    creditsAdded: data.creditsAdded ?? 0,
    newBalance: data.newBalance ?? 0,
    newExpiry: data.newExpiry || null,
    adsWatchedToday: data.adsWatchedToday ?? 0,
    adsRemainingToday: data.adsRemainingToday ?? 0
  };
}

export async function getBalance() {
  return await requestEndpoint('get-credit-balance', {});
}

export async function getConfig() {
  const res = await fetch(`${BASE_URL}/functions/v1/get-config`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'apikey': ANON_KEY,
      'Authorization': `Bearer ${ANON_KEY}`,
      'User-Agent': 'Ktor client'
    },
    body: '{}'
  });
  if (!res.ok) throw new Error(`Failed to get config: HTTP ${res.status}`);
  return await res.json();
}

export async function downloadMedia(url) {
  if (!url || typeof url !== 'string') {
    throw new Error('URL is required');
  }
  const res = await fetch(url, {
    headers: {
      'User-Agent': 'Mozilla/5.0'
    }
  });
  if (!res.ok) {
    throw new Error(`Download failed: HTTP ${res.status}`);
  }
  const ab = await res.arrayBuffer();
  return Buffer.from(ab);
}
