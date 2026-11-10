import { load, save } from "../store.js";
import { nextPuzzle, DIFFICULTIES } from "./puzzles.js";

const TICK_MS = 100;

export function recordKey(config) {
  return `anagramRecord.${config.difficulty}.${config.totalMs}.${config.lives}`;
}

export class AnagramSession {
  constructor(config) {
    this.config = config;
    this.listeners = new Set();
    this.clock = setInterval(() => this.tick(), TICK_MS);
    this.restart();
  }

  subscribe(fn) {
    this.listeners.add(fn);
    fn(this);
    return () => this.listeners.delete(fn);
  }

  changed() {
    this.listeners.forEach((fn) => fn(this));
  }

  close() {
    clearInterval(this.clock);
    this.listeners.clear();
  }

  restart() {
    this.lives = this.config.lives;
    this.solved = 0;
    this.hand = 0;
    this.remainingMs = this.config.totalMs;
    this.best = load(recordKey(this.config), 0);
    this.record = null;
    this.paused = false;
    this.next = nextPuzzle(this.config.difficulty);
    this.startHand();
  }

  async startHand() {
    this.stage = { name: "loading" };
    this.changed();
    const pending = this.next;
    this.next = nextPuzzle(this.config.difficulty);
    try {
      const puzzle = await pending;
      this.hand++;
      this.stage = { name: "solving", puzzle, found: [] };
    } catch (e) {
      this.stage = { name: "failed", message: e.message };
    }
    this.lastTick = performance.now();
    this.changed();
  }

  tick() {
    const now = performance.now();
    const elapsed = now - (this.lastTick ?? now);
    this.lastTick = now;
    if (this.stage?.name !== "solving" || this.paused) return;
    this.remainingMs = Math.max(0, this.remainingMs - elapsed);
    if (this.remainingMs === 0) this.finish("timeout");
    this.changed();
  }

  submit(letters) {
    const s = this.stage;
    if (s.name !== "solving") return null;
    const key = letters.join();
    if (!s.puzzle.answers.some((a) => a.join() === key)) return this.miss(s);
    if (s.found.some((f) => f.join() === key)) return "repeated";
    return this.hit(s, [...s.found, letters]);
  }

  hit(s, found) {
    if (DIFFICULTIES[this.config.difficulty].findAll && found.length < s.puzzle.answers.length) {
      this.stage = { ...s, found };
      this.changed();
      return "found";
    }
    this.solved++;
    this.stage = { name: "revealed", puzzle: s.puzzle, found, outcome: "solved" };
    this.changed();
    return "solved";
  }

  miss(s) {
    this.lives--;
    if (this.lives === 0) this.finish("no_lives");
    this.changed();
    return "invalid";
  }

  giveUp() {
    const s = this.stage;
    if (s.name !== "solving") return;
    this.lives--;
    if (this.lives === 0) this.finish("no_lives");
    else this.stage = { name: "revealed", puzzle: s.puzzle, found: s.found, outcome: "gave_up" };
    this.changed();
  }

  finish(outcome) {
    const s = this.stage;
    const best = load(recordKey(this.config), 0);
    if (this.solved > best) save(recordKey(this.config), this.solved);
    this.record = { best: Math.max(best, this.solved), isNew: this.solved > best };
    this.stage = { name: "finished", puzzle: s.puzzle ?? null, found: s.found ?? [], outcome };
  }

  nextHand() {
    if (this.stage.name === "revealed") this.startHand();
  }

  togglePause() {
    if (this.stage.name === "solving") this.paused = !this.paused;
    this.changed();
  }
}
