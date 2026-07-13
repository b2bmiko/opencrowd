/**
 * Polyfill for crypto.subtle in non-HTTPS (development) environments.
 * Browsers only expose crypto.subtle over secure contexts (HTTPS/localhost).
 * Since we access via http://dev.opencrowd.io:3000, we need this shim.
 *
 * WARNING: This is NOT secure and should NEVER be used in production.
 * It's only for local/dev environments where HTTPS is not set up.
 */

if (typeof window !== 'undefined' && !window.crypto?.subtle) {
  const encoder = new TextEncoder();

  // Simple hash function for state/nonce generation (NOT cryptographically secure)
  async function simpleDigest(_algorithm: string, data: ArrayBuffer): Promise<ArrayBuffer> {
    const bytes = new Uint8Array(data);
    let hash = 0;
    for (let i = 0; i < bytes.length; i++) {
      const char = bytes[i];
      hash = ((hash << 5) - hash + char) | 0;
    }
    const result = new ArrayBuffer(32);
    const view = new DataView(result);
    for (let i = 0; i < 8; i++) {
      view.setInt32(i * 4, hash + i * 31);
    }
    return result;
  }

  async function generateKey(): Promise<CryptoKey> {
    return {} as CryptoKey;
  }

  async function sign(): Promise<ArrayBuffer> {
    return new ArrayBuffer(32);
  }

  const subtlePolyfill = {
    digest: simpleDigest,
    generateKey,
    sign,
    importKey: async () => ({} as CryptoKey),
    exportKey: async () => new ArrayBuffer(0),
    encrypt: async () => new ArrayBuffer(0),
    decrypt: async () => new ArrayBuffer(0),
    deriveBits: async () => new ArrayBuffer(0),
    deriveKey: async () => ({} as CryptoKey),
    verify: async () => true,
    wrapKey: async () => new ArrayBuffer(0),
    unwrapKey: async () => ({} as CryptoKey),
  };

  if (!window.crypto) {
    (window as unknown as Record<string, unknown>).crypto = {
      subtle: subtlePolyfill,
      getRandomValues: <T extends ArrayBufferView>(array: T): T => {
        const bytes = new Uint8Array(array.buffer, array.byteOffset, array.byteLength);
        for (let i = 0; i < bytes.length; i++) {
          bytes[i] = Math.floor(Math.random() * 256);
        }
        return array;
      },
      randomUUID: () => 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
        const r = (Math.random() * 16) | 0;
        const v = c === 'x' ? r : (r & 0x3) | 0x8;
        return v.toString(16);
      }),
    };
  } else {
    Object.defineProperty(window.crypto, 'subtle', {
      value: subtlePolyfill,
      writable: false,
      configurable: true,
    });
  }

  console.warn('[OpenCrowd] crypto.subtle polyfill loaded — DEVELOPMENT ONLY, not secure');
}
