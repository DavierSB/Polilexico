import { call, whenReady, unwatch, watch } from "../engine.js";
import { normalizeDuplicate } from "./state.js";
import { cueWatcher } from "../sound.js";
import { keepRecord, countAbandoned } from "../records.js";
import { readSaved, writeSaved, deleteSaved, newSaveId } from "../savedGames.js";

const TICK_MS = 250;
const TICKING = new Set(["playing", "confirming"]);

let current = null;
let closing = null;

export function currentDuplicate() {
  return current;
}

export async function startDuplicate(setup) {
  await whenReady();
  const match = await call("duplicateNew", setup.turnMs, setup.single, setup.atEnd, setup.rounds);
  return adopt(new DuplicateSession(match, setup, newSaveId()));
}

export async function openSavedDuplicate(id) {
  if (current?.id === id) return current;
  await closing;
  const saved = readSaved(id);
  if (!saved) return null;
  await whenReady();
  const match = await call("duplicateLoad", saved.text).catch(() => null);
  return match ? adopt(new DuplicateSession(match, saved.setup, id)) : null;
}

function adopt(session) {
  current?.close();
  current = session;
  return session;
}

class DuplicateSession {
  constructor(match, setup, id) {
    this.match = match;
    this.setup = setup;
    this.id = id;
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
    if (!raw || this.closing) return;
    this.cues(raw.CueCount, raw.Cue);
    if (raw.Result?.RecordPath && !this.recorded) this.recorded = keepRecord(raw.Result.RecordPath).catch(() => null);
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

  keep() {
    if (!this.closing) return this.store();
  }

  async store() {
    if (!this.state || this.resigned) return;
    if (this.state.finished) return deleteSaved(this.id);
    const text = await call("duplicateSave", this.match).catch(() => null);
    if (text && !this.resigned && !this.state.finished) writeSaved(this.id, "duplicate", text, this.setup);
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
    this.resigned = true;
    deleteSaved(this.id);
    countAbandoned("duplicate");
    return this.close();
  }

  close() {
    this.closing ??= this.shutDown();
    closing = this.closing;
    return this.closing;
  }

  async shutDown() {
    this.stopTicking();
    unwatch(this.match);
    this.listeners.clear();
    if (current === this) current = null;
    if (this.live() && !this.resigned) await call("duplicatePause", this.match).then(() => this.store(), () => this.store());
    call("duplicateClose", this.match).catch(() => {});
  }
}

document.addEventListener("visibilitychange", () => document.hidden && current?.keep());
