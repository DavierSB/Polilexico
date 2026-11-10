import { call, whenReady, unwatch, watch } from "../engine.js";
import { load, save } from "../store.js";
import { parseBoard, tokens } from "../tiles.js";
import { parsePlacement, spelled } from "../placement.js";

const TICK_MS = 250;
let current = null;

export function currentSprint() {
  return current;
}

export async function startSprint(setup) {
  await whenReady();
  current?.close();
  const match = await call("sprintNew", setup.totalMs, setup.lives, setup.single, setup.difficulty);
  current = new SprintSession(match, setup);
  return current;
}

export function recordKey(setup) {
  return `sprintRecord.${setup.difficulty}.${setup.totalMs}.${setup.lives}.${setup.single}`;
}

class SprintSession {
  constructor(match, setup) {
    this.match = match;
    this.setup = setup;
    this.best = load(recordKey(setup), 0);
    this.played = [];
    this.listeners = new Set();
    this.state = null;
    watch(match, () => this.refresh());
    this.refresh();
  }

  subscribe(fn) {
    this.listeners.add(fn);
    if (this.state) fn(this.state);
    return () => this.listeners.delete(fn);
  }

  async refresh() {
    const raw = await call("sprintRead", this.match).catch(() => null);
    if (!raw) return;
    this.state = this.normalize(raw);
    this.noteRecord();
    this.listeners.forEach((fn) => fn(this.state));
    this.tickWhileRunning();
  }

  normalize(raw) {
    const cells = raw.Board ? parseBoard(raw.Board) : null;
    const bingo = (s) => bingoOf(s, cells);
    return {
      phase: raw.Phase, paused: raw.Paused, lives: raw.Lives, maxLives: raw.MaxLives, solved: raw.Solved,
      remainingMs: raw.RemainingMs, boardText: raw.Board, cells, rack: tokens(raw.Rack), bingoCount: raw.Bingos,
      outcome: raw.Outcome, answer: raw.Answer ? bingo(raw.Answer) : null, bingos: (raw.Solutions ?? []).map(bingo),
      lastError: raw.LastError || null, best: this.best, played: this.played,
    };
  }

  noteRecord() {
    const a = this.state.answer;
    if (this.state.outcome === "solved" && a && this.played.at(-1)?.placement !== a.placement) this.played.push(a);
    if (this.state.phase === "finished" && this.state.solved > load(recordKey(this.setup), 0)) save(recordKey(this.setup), this.state.solved);
  }

  tickWhileRunning() {
    const running = this.state.phase === "solving" && !this.state.paused;
    if (running && !this.ticker) this.ticker = setInterval(() => this.refresh(), TICK_MS);
    if (!running && this.ticker) {
      clearInterval(this.ticker);
      this.ticker = null;
    }
  }

  act(name, ...args) {
    return call(name, this.match, ...args).then(() => null, (e) => e.message).finally(() => this.refresh());
  }

  propose(input) {
    return this.act("sprintPropose", input);
  }

  giveUp() {
    return this.act("sprintGiveUp");
  }

  next() {
    return this.act("sprintNext");
  }

  pause() {
    return this.act("sprintPause");
  }

  resume() {
    return this.act("sprintResume");
  }

  close() {
    clearInterval(this.ticker);
    unwatch(this.match);
    call("sprintClose", this.match).catch(() => {});
    this.listeners.clear();
    if (current === this) current = null;
  }
}

function bingoOf(s, cells) {
  const text = `${s.Coords} ${s.Tiles}`;
  const p = parsePlacement(text);
  const shown = p && cells ? spelled(p, cells) : text.replace(/[[\]]/g, "");
  const [at, word] = shown.split(" ");
  return { placement: text, score: s.Score, at, word };
}
