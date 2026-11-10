import { normalize } from "../classic/state.js";
import { parseSave, readSave } from "./mirror.js";

const TICK_MS = 250;

export class RemoteSession {
  constructor(text, { guest, watching, remote }) {
    this.guest = guest;
    this.watching = watching;
    this.remote = remote;
    this.setup = {};
    this.listeners = new Set();
    this.ticker = null;
    this.replace(text);
  }

  replace(text) {
    this.saved = parseSave(text);
    this.receivedAt = performance.now();
    this.refresh();
  }

  refresh() {
    this.state = normalize(readSave(this.saved, this.guest, performance.now() - this.receivedAt), this.guest);
    this.listeners.forEach((fn) => fn(this.state));
    this.tickWhileRunning();
  }

  tickWhileRunning() {
    const running = !!this.state.clocks?.running && !this.state.result;
    if (running && !this.ticker) this.ticker = setInterval(() => this.refresh(), TICK_MS);
    if (!running && this.ticker) this.stopTicking();
  }

  stopTicking() {
    clearInterval(this.ticker);
    this.ticker = null;
  }

  subscribe(fn) {
    this.listeners.add(fn);
    fn(this.state);
    return () => this.listeners.delete(fn);
  }

  live() {
    return !this.state.result;
  }

  async play(input) {
    return this.remote.move(input);
  }

  pause() {}

  resume() {}

  resign() {
    this.remote.resign();
    this.close();
  }

  close() {
    this.stopTicking();
    this.listeners.clear();
  }
}
