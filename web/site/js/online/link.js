import { socketUrl, token, account } from "./api.js";

const RETRY_MS = 3000;
const CHECK_MS = 2000;
const PING_MS = 8000;
const SILENCE_MS = 16000;
const OPEN_MS = 10000;
const PROTOCOL = 5;
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
    this.pings = 0;
    this.pongs = 0;
    this.unconfirmed = [];
  }

  open() {
    this.connect();
    this.beat = setInterval(() => this.check(), CHECK_MS);
    this.wake = () => this.reconnectNow();
    this.lose = () => this.drop();
    window.addEventListener("online", this.wake);
    window.addEventListener("offline", this.lose);
  }

  close() {
    this.closed = true;
    clearInterval(this.beat);
    clearTimeout(this.retry);
    window.removeEventListener("online", this.wake);
    window.removeEventListener("offline", this.lose);
    this.socket?.close(1000);
  }

  send(message) {
    if (!this.live) return false;
    this.socket.send(JSON.stringify(message));
    if (message.t === "ping") this.pings++;
    return true;
  }

  sendSure(message) {
    if (!this.send(message)) return false;
    this.send({ t: "ping" });
    this.unconfirmed.push({ message, ping: this.pings });
    return true;
  }

  takeUnconfirmed() {
    return this.unconfirmed.splice(0).map((u) => u.message);
  }

  reconnectNow() {
    if (this.live || this.closed) return;
    clearTimeout(this.retry);
    this.abandon();
    this.connect();
  }

  abandon() {
    const s = this.socket;
    if (!s) return;
    this.socket = null;
    s.onopen = s.onmessage = s.onclose = null;
    s.close();
  }

  async connect() {
    if (this.closed || this.connecting) return;
    this.connecting = true;
    const t = await token();
    this.connecting = false;
    if (!t && !account()) return this.onEvent({ t: "refused", reason: "La sesión caducó: vuelve a entrar." });
    if (!t) return this.later();
    if (this.closed || this.socket) return;
    const socket = new WebSocket(socketUrl());
    this.socket = socket;
    socket.onopen = () => this.opened(t);
    socket.onmessage = (e) => this.heardMessage(e.data);
    socket.onclose = () => this.ended();
    clearTimeout(this.opening);
    this.opening = setTimeout(() => !this.live && this.socket === socket && this.drop(), OPEN_MS);
  }

  opened(t) {
    clearTimeout(this.opening);
    this.live = true;
    this.pings = this.pongs = 0;
    this.heard = this.pinged = Date.now();
    this.socket.send(JSON.stringify({ t: "hello", version: PROTOCOL, token: t }));
    this.onStatus(null);
  }

  heardMessage(text) {
    this.heard = Date.now();
    try {
      const e = JSON.parse(text);
      if (e.t === "pong") this.ponged();
      this.onEvent(e);
    } catch {
      return;
    }
  }

  ponged() {
    this.pongs++;
    this.unconfirmed = this.unconfirmed.filter((u) => u.ping > this.pongs);
  }

  drop() {
    if (!this.socket) return;
    this.abandon();
    this.ended();
  }

  ended() {
    clearTimeout(this.opening);
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
    if (now - this.heard > SILENCE_MS) return this.drop();
    if (now - this.pinged >= PING_MS) {
      this.pinged = now;
      this.send({ t: "ping" });
    }
  }
}
