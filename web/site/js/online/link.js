import { socketUrl, token, account } from "./api.js";

const RETRY_MS = 3000;
const CHECK_MS = 2000;
const PING_MS = 8000;
const SILENCE_MS = 16000;
const PROTOCOL = 4;
const LINK_DOWN = "Sin conexión. Reconectando…";

export class Link {
  constructor(onEvent, onStatus) {
    this.onEvent = onEvent;
    this.onStatus = onStatus;
    this.socket = null;
    this.live = false;
    this.closed = false;
    this.heard = 0;
    this.pinged = 0;
  }

  open() {
    this.connect();
    this.beat = setInterval(() => this.check(), CHECK_MS);
    this.wake = () => !this.live && this.connect();
    window.addEventListener("online", this.wake);
  }

  close() {
    this.closed = true;
    clearInterval(this.beat);
    clearTimeout(this.retry);
    window.removeEventListener("online", this.wake);
    this.socket?.close(1000);
  }

  send(message) {
    if (!this.live) return false;
    this.socket.send(JSON.stringify(message));
    return true;
  }

  async connect() {
    if (this.closed || this.connecting) return;
    this.connecting = true;
    const t = await token();
    this.connecting = false;
    if (!t && !account()) return this.onEvent({ t: "refused", reason: "La sesión caducó: vuelve a entrar." });
    if (!t) return this.later();
    this.socket = new WebSocket(socketUrl());
    this.socket.onopen = () => this.opened(t);
    this.socket.onmessage = (e) => this.heardMessage(e.data);
    this.socket.onclose = () => this.ended();
  }

  opened(t) {
    this.live = true;
    this.heard = this.pinged = Date.now();
    this.socket.send(JSON.stringify({ t: "hello", version: PROTOCOL, token: t }));
    this.onStatus(null);
  }

  heardMessage(text) {
    this.heard = Date.now();
    try {
      this.onEvent(JSON.parse(text));
    } catch {
      return;
    }
  }

  ended() {
    this.live = false;
    this.socket = null;
    if (this.closed) return;
    this.onStatus(LINK_DOWN);
    this.later();
  }

  later() {
    clearTimeout(this.retry);
    this.retry = setTimeout(() => this.connect(), RETRY_MS);
  }

  check() {
    if (!this.live) return;
    const now = Date.now();
    if (now - this.heard > SILENCE_MS) return this.socket?.close();
    if (now - this.pinged >= PING_MS) {
      this.pinged = now;
      this.send({ t: "ping" });
    }
  }
}
