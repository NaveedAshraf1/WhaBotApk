import http from 'http';
import { Boom } from '@hapi/boom';
import pino from 'pino';
import path from 'path';
import fs from 'fs';
import { default as makeWASocket, useMultiFileAuthState, DisconnectReason } from '@whiskeysockets/baileys';
import mimeTypes from 'mime-types';
import QRCode from 'qrcode';

const projectDir = process.argv[2] || import.meta.dirname;
const PORT = 3001;
const AUTH_PATH = path.join(projectDir, 'auth_storage');

let sock = null;
let connectionState = 'disconnected';
let pairingCode = null;
let pairingPhone = null;
let qrReceived = false;
let currentQrCode = null;
let currentQrDataUrl = null;
let reconnectAttempts = 0;
const MAX_RECONNECT_ATTEMPTS = 10;
let baileysStarting = false;

// Queue of incoming messages for the Android app to poll
const messageQueue = [];

function log(message) {
    const logFile = path.join(projectDir, 'baileys.log');
    const line = `[${new Date().toISOString()}] ${message}\n`;
    try {
        fs.appendFileSync(logFile, line);
    } catch (e) {
        console.error('Failed to write log:', e.message);
    }
    console.log(line.trim());
}

function hasCredentials() {
    return fs.existsSync(AUTH_PATH) && fs.readdirSync(AUTH_PATH).length > 0;
}

async function startBaileys() {
    if (baileysStarting) return;
    baileysStarting = true;
    log('Starting Baileys...');

    try {
        const { state, saveCreds } = await useMultiFileAuthState(AUTH_PATH);

        sock = makeWASocket({
            auth: state,
            logger: pino({ level: 'warn' }),
            printQRInTerminal: false,
            connectTimeoutMs: 20000,
            keepAliveIntervalMs: 10000,
            browser: ['Mac OS', 'Chrome', '1.0.0'],
            version: [2, 3000, 1033893291]
        });

        sock.ev.on('connection.update', async (update) => {
            const { connection, lastDisconnect, qr, receivedPendingNotifications } = update;

            if (qr) {
                qrReceived = true;
                currentQrCode = qr;
                connectionState = 'qr_ready';
                // Generate QR code image as base64 data URL
                try {
                    currentQrDataUrl = await QRCode.toDataURL(qr, { width: 300, margin: 2 });
                    log('QR code received and image generated');
                } catch (e) {
                    log(`QR image generation error: ${e.message}`);
                }
            }

            if (receivedPendingNotifications) {
                log('Received pending notifications');
            }

            if (connection === 'close') {
                qrReceived = false;
                pairingCode = null;
                currentQrCode = null;
                currentQrDataUrl = null;
                const statusCode = (lastDisconnect?.error instanceof Boom)?.output?.statusCode;
                const errorMsg = lastDisconnect?.error?.message || 'unknown';
                const errorStack = lastDisconnect?.error?.stack || '';
                const loggedOut = statusCode === DisconnectReason.loggedOut;
                log(`Disconnect error details: ${JSON.stringify({ statusCode, errorMsg, errorStack: errorStack.substring(0, 500) })}`);
                const hasCreds = hasCredentials();
                // Always reconnect for fresh sessions (no creds) or if not logged out
                const shouldReconnect = !loggedOut && reconnectAttempts < MAX_RECONNECT_ATTEMPTS;
                log(`Connection closed. Status: ${statusCode}. Error: ${errorMsg}. HasCreds: ${hasCreds}. Reconnect: ${shouldReconnect} (attempt ${reconnectAttempts + 1}/${MAX_RECONNECT_ATTEMPTS})`);
                connectionState = 'disconnected';
                sock = null;
                baileysStarting = false;
                if (shouldReconnect) {
                    reconnectAttempts++;
                    const delay = Math.min(3000 * reconnectAttempts, 15000);
                    log(`Reconnecting in ${delay}ms...`);
                    setTimeout(startBaileys, delay);
                } else if (loggedOut) {
                    log('Logged out. Clearing auth and restarting...');
                    if (fs.existsSync(AUTH_PATH)) {
                        fs.rmSync(AUTH_PATH, { recursive: true, force: true });
                    }
                    reconnectAttempts = 0;
                    setTimeout(startBaileys, 2000);
                } else {
                    reconnectAttempts = 0;
                }
            } else if (connection === 'open') {
                reconnectAttempts = 0;
                qrReceived = false;
                pairingCode = null;
                currentQrCode = null;
                currentQrDataUrl = null;
                connectionState = 'connected';
                log('Connected successfully to WhatsApp');
            } else if (connection === 'connecting') {
                connectionState = 'connecting';
                log('Connecting to WhatsApp servers...');
            }
        });

        sock.ev.on('creds.update', saveCreds);
        sock.ev.on('messages.upsert', async ({ messages }) => {
            for (const msg of messages) {
                if (!msg.message) continue;
                // Skip own messages
                if (msg.key.fromMe) continue;
                const from = msg.key.remoteJid || '';
                // Skip group messages, newsletters, and broadcasts — only handle direct chats
                if (from.endsWith('@g.us') || from.endsWith('@newsletter') || from.endsWith('@broadcast')) continue;
                const name = msg.pushName || from;
                const body = msg.message.conversation || msg.message.extendedTextMessage?.text || '';
                if (body) {
                    // Resolve LID to actual phone JID if needed
                    let replyJid = from;
                    if (from.endsWith('@lid')) {
                        try {
                            const resolved = sock.getKeyFromLid?.(from);
                            if (resolved) replyJid = resolved;
                        } catch (e) {}
                    }
                    log(`Message from ${from} (${name}): ${body.substring(0, 100)}`);
                    // Queue for Android app to poll — include replyJid for sending replies
                    messageQueue.push({ from, name, body, replyJid, timestamp: Date.now() });
                }
            }
        });
    } catch (e) {
        log(`Baileys start error: ${e.message}`);
        baileysStarting = false;
        reconnectAttempts++;
        if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
            setTimeout(startBaileys, 3000);
        }
    }
}

// Wait for Baileys to be ready for pairing (qrReceived or connecting)
function waitForReady(timeoutMs = 30000) {
    return new Promise((resolve, reject) => {
        if (qrReceived || (sock && connectionState === 'connecting')) {
            resolve();
            return;
        }
        const start = Date.now();
        const interval = setInterval(() => {
            if (qrReceived || (sock && connectionState === 'connecting')) {
                clearInterval(interval);
                resolve();
            } else if (Date.now() - start > timeoutMs) {
                clearInterval(interval);
                reject(new Error('Timeout waiting for Baileys to be ready. Try again.'));
            }
        }, 500);
    });
}

async function requestPairingCode(phoneNumber) {
    if (!sock) {
        // Try to restart Baileys if not running
        log('Baileys not running, attempting to start...');
        await startBaileys();
    }
    // Wait for the socket to be ready for pairing
    await waitForReady();
    const code = await sock.requestPairingCode(phoneNumber);
    return code;
}

function sendMessage(recipient, content) {
    if (!sock) throw new Error('Baileys not initialized');
    const cleaned = recipient.replace(/[\s\-]/g, '');
    // If it's already a JID (contains @), use as-is; otherwise build s.whatsapp.net JID
    const jid = cleaned.includes('@') ? cleaned : `${cleaned}@s.whatsapp.net`;
    log(`Sending message to ${jid}: ${content.substring(0, 80)}`);
    return sock.sendMessage(jid, { text: content });
}

function sendFile(recipient, filePath, caption) {
    if (!sock) throw new Error('Baileys not initialized');
    if (!fs.existsSync(filePath)) throw new Error('File not found');
    const cleaned = recipient.replace(/[\s\-]/g, '');
    const jid = cleaned.includes('@') ? cleaned : `${cleaned}@s.whatsapp.net`;
    const mimeType = mimeTypes.lookup(filePath) || 'application/octet-stream';
    if (mimeType.startsWith('image/')) {
        return sock.sendMessage(jid, { image: { url: filePath }, caption: caption || '' });
    } else if (mimeType.startsWith('video/')) {
        return sock.sendMessage(jid, { video: { url: filePath }, caption: caption || '' });
    } else {
        return sock.sendMessage(jid, { document: { url: filePath }, mimetype: mimeType, fileName: path.basename(filePath), caption: caption || '' });
    }
}

const server = http.createServer(async (req, res) => {
    res.setHeader('Content-Type', 'application/json');
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
    res.setHeader('Access-Control-Allow-Headers', 'Content-Type');

    if (req.method === 'OPTIONS') {
        res.writeHead(200);
        res.end();
        return;
    }

    const url = new URL(req.url, `http://${req.headers.host}`);

    if (url.pathname === '/api/status' && req.method === 'GET') {
        res.writeHead(200);
        res.end(JSON.stringify({
            state: connectionState,
            pairingCode: pairingCode,
            pairingPhone: pairingPhone,
            qrCode: currentQrDataUrl,
            readyForPairing: qrReceived || connectionState === 'connecting',
            hasCredentials: hasCredentials(),
            reconnectAttempts: reconnectAttempts
        }));
        return;
    }

    // Return queued incoming messages and clear the queue
    if (url.pathname === '/api/messages' && req.method === 'GET') {
        const messages = messageQueue.splice(0, messageQueue.length);
        res.writeHead(200);
        res.end(JSON.stringify({ messages }));
        return;
    }

    if (url.pathname === '/api/qr' && req.method === 'GET') {
        if (currentQrDataUrl) {
            res.writeHead(200);
            res.end(JSON.stringify({ success: true, qrCode: currentQrDataUrl }));
        } else {
            res.writeHead(200);
            res.end(JSON.stringify({ success: false, error: 'No QR code available' }));
        }
        return;
    }

    if (url.pathname === '/api/pairing-code' && req.method === 'POST') {
        try {
            const body = await readBody(req);
            const { phone } = JSON.parse(body);
            if (!phone) throw new Error('Phone number required');
            const cleaned = phone.replace(/[+\s\-]/g, '');
            if (cleaned.length < 10) throw new Error('Invalid phone number');
            log(`Requesting pairing code for ${cleaned}`);
            const code = await requestPairingCode(cleaned);
            pairingCode = code;
            pairingPhone = cleaned;
            log(`Pairing code generated: ${code}`);
            res.writeHead(200);
            res.end(JSON.stringify({ success: true, phone: cleaned, code: code }));
        } catch (e) {
            log(`Pairing code error: ${e.message}`);
            res.writeHead(400);
            res.end(JSON.stringify({ success: false, error: e.message }));
        }
        return;
    }

    if (url.pathname === '/api/send-text' && req.method === 'POST') {
        try {
            const body = await readBody(req);
            const { number, message } = JSON.parse(body);
            if (!number || !message) throw new Error('number and message required');
            await sendMessage(number, message);
            res.writeHead(200);
            res.end(JSON.stringify({ success: true }));
        } catch (e) {
            log(`Send text error: ${e.message}`);
            res.writeHead(400);
            res.end(JSON.stringify({ success: false, error: e.message }));
        }
        return;
    }

    if (url.pathname === '/api/send-file' && req.method === 'POST') {
        try {
            const body = await readBody(req);
            const { number, filePath, caption } = JSON.parse(body);
            if (!number || !filePath) throw new Error('number and filePath required');
            await sendFile(number, filePath, caption);
            res.writeHead(200);
            res.end(JSON.stringify({ success: true }));
        } catch (e) {
            log(`Send file error: ${e.message}`);
            res.writeHead(400);
            res.end(JSON.stringify({ success: false, error: e.message }));
        }
        return;
    }

    if (url.pathname === '/api/restart' && req.method === 'POST') {
        try {
            log('Manual restart requested');
            if (sock) {
                try { await sock.logout(); } catch (e) {}
                sock = null;
            }
            baileysStarting = false;
            reconnectAttempts = 0;
            setTimeout(startBaileys, 500);
            res.writeHead(200);
            res.end(JSON.stringify({ success: true }));
        } catch (e) {
            res.writeHead(400);
            res.end(JSON.stringify({ success: false, error: e.message }));
        }
        return;
    }

    if (url.pathname === '/api/logout' && req.method === 'POST') {
        try {
            if (sock) {
                try { await sock.logout(); } catch (e) {}
                sock = null;
            }
            if (fs.existsSync(AUTH_PATH)) {
                fs.rmSync(AUTH_PATH, { recursive: true, force: true });
                log('Session storage cleared');
            }
            reconnectAttempts = 0;
            pairingCode = null;
            pairingPhone = null;
            connectionState = 'connecting';
            qrReceived = false;
            baileysStarting = false;
            setTimeout(startBaileys, 500);
            res.writeHead(200);
            res.end(JSON.stringify({ success: true, cleared: true }));
        } catch (e) {
            res.writeHead(400);
            res.end(JSON.stringify({ success: false, error: e.message }));
        }
        return;
    }

    res.writeHead(404);
    res.end(JSON.stringify({ error: 'Not found' }));
});

function readBody(req) {
    return new Promise((resolve, reject) => {
        let data = '';
        req.on('data', chunk => data += chunk);
        req.on('end', () => resolve(data));
        req.on('error', reject);
    });
}

// Start HTTP server first, then Baileys
server.listen(PORT, '0.0.0.0', () => {
    log(`Embedded Baileys server running on http://127.0.0.1:${PORT}`);
    startBaileys();
});
