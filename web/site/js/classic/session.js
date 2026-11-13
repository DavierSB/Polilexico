import { call, whenReady, unwatch, watch } from "../engine.js";
import { clocks, flipClocks, normalize } from "./state.js";
import { cueWatcher } from "../sound.js";
import { keepRecord, countAbandoned } from "../records.js";
import { readSaved, writeSaved, deleteSaved, newSaveId } from "../savedGames.js";

const TICK_MS = 250;

let current = null;
let closing = null;

export function currentSession() {
  return current;
}

export async function startLocal(setup) {
  await whenReady();
  const match = setup.players
    ? await call("classicNewLocal", setup.players[0], setup.players[1], setup.starter, setup.timeMs, setup.overtimeMs, setup.single)
    : await call("classicNew", setup.bot, setup.starter, setup.timeMs, setup.overtimeMs, setup.single);
  return adopt(new Session(match, { setup, id: newSaveId(), mode: "classic" }));
}

export async function startEndgame(setup) {
  await whenReady();
  const e = setup.endgame;
  const match = await call("endgameNew", e.minBag, e.maxBag, e.minLead, e.maxLead, e.q, setup.timeMs, setup.overtimeMs, setup.single);
  return adopt(new Session(match, { setup, id: newSaveId(), mode: "endgame" }));
}

export function startAgain(setup) {
  return setup.endgame ? startEndgame(setup) : startLocal(setup);
}

export async function openSaved(id) {
  if (current?.id === id) return current;
  await closing;
  const saved = readSaved(id);
  if (!saved) return null;
  await whenReady();
  const match = await call("classicLoad", saved.text).catch(() => null);
  return match ? adopt(new Session(match, saved)) : null;
}

function adopt(session) {
  current?.close();
  current = session;
  return session;
}

export class Session {
  constructor(match, { setup = {}, id, mode }) {
    this.match = match;
    this.setup = setup ?? {};
    this.id = id;
    this.mode = mode;
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
    return !!this.state && !this.state.result;
  }

  async refresh() {
    const raw = await call("classicRead", this.match).catch(() => null);
    if (!raw || this.closing) return;
    this.cues(raw.CueCount, raw.Cue);
    if (raw.Result?.RecordPath && !this.recorded) this.recorded = keepRecord(raw.Result.RecordPath).catch(() => null);
    this.flipped = raw.Local && !raw.Status.Over && !raw.Status.HumanToMove;
    this.state = normalize(raw, this.flipped);
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
    if (!raw || !this.state || this.closing) return;
    const c = clocks(raw);
    this.state = { ...this.state, clocks: this.flipped ? flipClocks(c) : c };
    this.publish();
  }

  keep() {
    if (!this.closing) return this.store();
  }

  async store() {
    if (!this.state || this.resigned) return;
    if (this.state.result) return deleteSaved(this.id);
    const text = await call("classicSave", this.match).catch(() => null);
    if (text && !this.resigned && !this.state.result) writeSaved(this.id, this.mode, text, this.setup);
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
    this.resigned = true;
    deleteSaved(this.id);
    countAbandoned(this.mode);
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
    if (this.live() && !this.resigned) await call("classicPause", this.match).then(() => this.store(), () => this.store());
    call("classicClose", this.match).catch(() => {});
  }
}

document.addEventListener("visibilitychange", () => document.hidden && current?.keep());
