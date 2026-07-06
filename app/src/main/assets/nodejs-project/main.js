const http = require('http');
const { default: makeWASocket, useMultiFileAuthState, DisconnectReason } = require('@whiskeysockets/baileys');
const { Boom } = require('@hapi/boom');
const pino = require('pino');
const path = require('path');
const fs = require('fs');

const projectDir = process.argv[2] || __dirname;
const PORT = 3001;
const AUTH_PATH = path.join(projectDir, 'auth_storage');

let sock = null;
let connectionState = 'disconnected';
let pairingCode = null;
let pairingPhone = null;
let qrReceived = false;
let reconnectAttempts = 0;
const MAX_RECONNECT_ATTEMPTS = 5;

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
    log('Starting Baileys...');
    const { state, saveCreds } = await useMultiFileAuthState(AUTH_PATH);

    sock = makeWASocket({
        auth: state,
        logger: pino({ level: 'silent' }),
        printQRInTerminal: false
    });

    sock.ev.on('connection.update', async (update) => {
        const { connection, lastDisconnect, qr } = update;

        if (qr) {
            qrReceived = true;
            log('QR received, waiting for pairing-code request...');
        }

        if (connection === 'close') {
            qrReceived = false;
            pairingCode = null;
            const statusCode = lastDisconnect?.error instanceof Boom
                ? lastDisconnect.error.output?.statusCode
                : undefined;
            const loggedOut = statusCode === DisconnectReason.loggedOut;
            const hasCreds = hasCredentials();
            const shouldReconnect = !loggedOut && hasCreds && reconnectAttempts < MAX_RECONNECT_ATTEMPTS;
            log(`Connection closed. Status: ${statusCode}. Reconnect: ${shouldReconnect}`);
            connectionState = 'disconnected';
            if (shouldReconnect) {
                reconnectAttempts++;
                setTimeout(startBaileys, 3000);
            } else {
                reconnectAttempts = 0;
            }
        } else if (connection === 'open') {
            reconnectAttempts = 0;
            qrReceived = false;
            pairingCode = null;
            connectionState = 'connected';
            log('Connected successfully');
        } else if (connection === 'connecting') {
            connectionState = 'connecting';
        }
    });

    sock.ev.on('creds.update', saveCreds);
}

async function requestPairingCode(phoneNumber) {
    if (!sock) throw new Error('Baileys not initialized');
    if (!qrReceived && connectionState !== 'connecting') throw new Error('Not ready for pairing. Please wait or restart.');
    const code = await sock.requestPairingCode(phoneNumber);
    return code;
}

function sendMessage(recipient, content) {
    if (!sock) throw new Error('Baileys not initialized');
    const cleaned = recipient.replace(/[\s\-]/g, '');
    const jid = cleaned.includes('@') ? cleaned : `${cleaned}@s.whatsapp.net`;
    return sock.sendMessage(jid, { text: content });
}

function sendFile(recipient, filePath, caption) {
    if (!sock) throw new Error('Baileys not initialized');
    if (!fs.existsSync(filePath)) throw new Error('File not found');
    const cleaned = recipient.replace(/[\s\-]/g, '');
    const jid = cleaned.includes('@') ? cleaned : `${cleaned}@s.whatsapp.net`;
    const mimeType = require('mime-types').lookup(filePath) || 'application/octet-stream';
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
            readyForPairing: qrReceived || connectionState === 'connecting'
        }));
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
        let body = '';
        req.on('data', chunk => body += chunk);
        req.on('end', () => resolve(body));
        req.on('error', reject);
    });
}

startBaileys().then(() => {
    server.listen(PORT, '127.0.0.1', () => {
        log(`Embedded Baileys server running on http://127.0.0.1:${PORT}`);
    });
}).catch(err => {
    log(`Failed to start: ${err.message}`);
    process.exit(1);
});
