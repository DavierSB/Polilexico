import { call, whenReady, unwatch, watch } from "../engine.js";
import { load, save } from "../store.js";
import { normalizeDuplicate } from "./state.js";
import { cueWatcher } from "../sound.js";

const SAVE_KEY = "duplicateGame";
const TICK_MS = 250;
const TICKING = new Set(["playing", "confirming"]);

let current = null;

export function currentDuplicate() {
  return current;
}

export function hasSavedDuplicate() {
  return !!load(SAVE_KEY, null);
}

export async function startDuplicate(setup) {
  await whenReady();
  const match = await call("duplicateNew", setup.turnMs, setup.single, setup.atEnd, setup.rounds);
  return adopt(new DuplicateSession(match, setup));
}

export async function resumeDuplicate() {
  const saved = load(SAVE_KEY, null);
  if (!saved) return null;
  await whenReady();
  const match = await call("duplicateLoad", saved.text).catch(() => null);
  if (!match) return save(SAVE_KEY, null);
  return adopt(new DuplicateSession(match, saved.setup));
}

function adopt(session) {
  current?.close();
  current = session;
  return session;
}

class DuplicateSession {
  constructor(match, setup) {
    this.match = match;
    this.setup = setup;
    this.listeners = new Set();
    this.state = null;
    this.ticker = null;
    this.cues = cueWatcher();
    watch(match, () => this.refresh());
    this.refresh();
  }

  subscribe(fn) {
    this.listeners.add(fn);
    if (this.state) fn(this.state);
    return () => this.listeners.delete(fn);
  }

  live() {
    return !!this.state && !this.state.finished;
  }

  async refresh() {
    const raw = await call("duplicateRead", this.match).catch(() => null);
    if (!raw) return;
    this.cues(raw.CueCount, raw.Cue);
    const changed = raw.Turns?.length !== this.state?.rounds.length || raw.Phase !== this.state?.phase;
    this.state = normalizeDuplicate(raw);
    this.listeners.forEach((fn) => fn(this.state));
    this.tickWhileRunning();
    if (changed) this.keep();
  }

  tickWhileRunning() {
    const running = TICKING.has(this.state.phase) && !this.state.paused;
    if (running && !this.ticker) this.ticker = setInterval(() => this.refresh(), TICK_MS);
    if (!running && this.ticker) this.stopTicking();
  }

  stopTicking() {
    clearInterval(this.ticker);
    this.ticker = null;
  }

  async keep() {
    if (this !== current) return;
    if (this.state.finished) return save(SAVE_KEY, null);
    const text = await call("duplicateSave", this.match).catch(() => null);
    if (text && this === current) save(SAVE_KEY, { text, setup: this.setup });
  }

  act(name, ...args) {
    return call(name, this.match, ...args).then(() => null, (e) => e.message).finally(() => this.refresh());
  }

  showRack() {
    return this.act("duplicateShowRack");
  }

  propose(input) {
    return this.act("duplicatePropose", input);
  }

  cancel() {
    return this.act("duplicateCancel");
  }

  pause() {
    return this.act("duplicatePause");
  }

  resume() {
    return this.act("duplicateResume");
  }

  resign() {
    save(SAVE_KEY, null);
    this.close();
  }

  close() {
    this.stopTicking();
    unwatch(this.match);
    call("duplicateClose", this.match).catch(() => {});
    this.listeners.clear();
    if (current === this) current = null;
  }
}

document.addEventListener("visibilitychange", () => document.hidden && current?.keep());
