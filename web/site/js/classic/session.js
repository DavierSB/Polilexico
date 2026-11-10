import { call, whenReady, unwatch, watch } from "../engine.js";
import { clocks, normalize } from "./state.js";
import { load, save } from "../store.js";

const SAVE_KEY = "classicGame";
const TICK_MS = 250;

let current = null;

export function currentSession() {
  return current;
}

export async function startLocal(setup) {
  await whenReady();
  const match = await call("classicNew", setup.bot, setup.starter, setup.timeMs, setup.overtimeMs, setup.single);
  return adopt(new Session(match, { setup }));
}

export async function resumeSaved() {
  const saved = load(SAVE_KEY, null);
  if (!saved) return null;
  await whenReady();
  const match = await call("classicLoad", saved.text).catch(() => null);
  if (!match) return save(SAVE_KEY, null);
  return adopt(new Session(match, { setup: saved.setup }));
}

export function hasSaved() {
  return !!load(SAVE_KEY, null);
}

function adopt(session) {
  current?.close();
  current = session;
  return session;
}

export class Session {
  constructor(match, { setup = {} }) {
    this.match = match;
    this.setup = setup;
    this.listeners = new Set();
    this.state = null;
    this.ticker = null;
    watch(match, () => this.refresh());
    this.refresh();
  }

  subscribe(fn) {
    this.listeners.add(fn);
    if (this.state) fn(this.state);
    return () => this.listeners.delete(fn);
  }

  live() {
    return !!this.state && !this.state.result;
  }

  async refresh() {
    const raw = await call("classicRead", this.match).catch(() => null);
    if (!raw) return;
    this.state = normalize(raw, false);
    this.publish();
    this.tickWhileRunning();
    this.keep();
  }

  publish() {
    this.listeners.forEach((fn) => fn(this.state));
  }

  tickWhileRunning() {
    const running = this.state.clocks?.running && !this.state.paused && !this.state.result;
    if (running && !this.ticker) this.ticker = setInterval(() => this.tick(), TICK_MS);
    if (!running && this.ticker) this.stopTicking();
  }

  stopTicking() {
    clearInterval(this.ticker);
    this.ticker = null;
  }

  async tick() {
    const raw = await call("classicClocks", this.match).catch(() => null);
    if (!raw || !this.state) return;
    const c = clocks(raw);
    this.state = { ...this.state, clocks: c };
    this.publish();
  }

  async keep() {
    if (this !== current) return;
    if (this.state.result) return save(SAVE_KEY, null);
    const text = await call("classicSave", this.match).catch(() => null);
    if (text && this === current) save(SAVE_KEY, { text, setup: this.setup });
  }

  async play(input) {
    return call("classicPlay", this.match, input).then(() => null, (e) => e.message);
  }

  pause() {
    call("classicPause", this.match).then(() => this.refresh());
  }

  resume() {
    call("classicResume", this.match).then(() => this.refresh());
  }

  resign() {
    save(SAVE_KEY, null);
    this.close();
  }

  close() {
    this.stopTicking();
    unwatch(this.match);
    call("classicClose", this.match).catch(() => {});
    this.listeners.clear();
    if (current === this) current = null;
  }
}

document.addEventListener("visibilitychange", () => document.hidden && current?.keep());
