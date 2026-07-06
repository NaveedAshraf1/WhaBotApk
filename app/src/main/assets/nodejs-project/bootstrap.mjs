// Bootstrap entry point - MUST run before any other code
// nodejs-mobile (Node v18.20.4) does not expose globalThis.crypto by default
// Baileys' crypto.js does `const { subtle } = globalThis.crypto` at module load time
// so we must polyfill it BEFORE dynamically importing main.mjs

import { webcrypto } from 'node:crypto';

if (!globalThis.crypto) {
    globalThis.crypto = webcrypto;
} else if (!globalThis.crypto.subtle) {
    // Merge subtle into existing crypto object
    globalThis.crypto = {
        ...globalThis.crypto,
        subtle: webcrypto.subtle,
        getRandomValues: globalThis.crypto.getRandomValues || webcrypto.getRandomValues
    };
}

console.log('[bootstrap] globalThis.crypto polyfill applied, subtle:', typeof globalThis.crypto?.subtle);

// Now dynamically import main.mjs after polyfill is in place
const projectDir = process.argv[2] || import.meta.dirname;
await import('./main.mjs');
