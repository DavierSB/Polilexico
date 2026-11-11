import { call, whenReady } from "../engine.js";
import { load, save } from "../store.js";
import { pick, dictionaryText } from "./words.js";
import { play } from "../sound.js";

const WORDS_PER_GAME = 10;

export function recordKey(config) {
  return `recallRecord.${config.intervalMs}.${config.lives}.${config.single}`;
}

export class RecallSession {
  constructor(config) {
    this.config = config;
    this.listeners = new Set();
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

  restart() {
    this.lives = this.config.lives;
    this.recalled = 0;
    this.asked = 0;
    this.round = 1;
    this.best = load(recordKey(this.config), 0);
    this.record = null;
    this.history = [];
    this.next = this.fetch();
    this.startRound();
  }

  fetch() {
    return whenReady().then(() => call("demoScored")).then(async (game) => ({ game, words: await pick(game, WORDS_PER_GAME) }));
  }

  async startRound() {
    this.stage = { name: "loading" };
    this.answers = [];
    this.changed();
    const pending = this.next;
    this.next = this.fetch();
    try {
      const round = await pending;
      this.words = round.words;
      this.stage = { name: "watching", game: round.game };
    } catch (e) {
      this.stage = { name: "failed", message: e.message };
    }
    this.changed();
  }

  watched() {
    if (this.stage.name !== "watching") return;
    this.stage = this.words.length ? { name: "solving", index: 0, verdict: null } : this.endRound();
    this.changed();
  }

  solving() {
    return this.stage.name === "solving" && !this.stage.verdict ? this.stage : null;
  }

  async submit(letters) {
    const s = this.solving();
    if (!s) return null;
    const word = this.words[s.index];
    const verdict = letters.join() === word.tiles.join() ? "hit" : (await call("isValidWord", dictionaryText(letters))) ? "not_played" : this.config.single ? "invalid" : null;
    if (!verdict) return (play("wrong"), "rejected");
    if (this.solving() !== s) return null;
    this.resolve(s, verdict);
    return verdict;
  }

  forget() {
    const s = this.solving();
    if (s) this.resolve(s, "forgotten");
  }

  resolve(s, verdict) {
    const word = this.words[s.index];
    const hit = verdict === "hit";
    if (hit) play("correct");
    else if (verdict !== "forgotten") play("wrong");
    this.answers = [...this.answers, { word, hit }];
    this.history = [...this.history, { word, hit }];
    this.asked++;
    if (hit) this.recalled++;
    else this.lives--;
    this.stage = { ...s, verdict };
    this.changed();
  }

  advance() {
    const s = this.stage;
    if (s.name !== "solving" || !s.verdict) return;
    this.stage = this.lives === 0 || s.index + 1 >= this.words.length ? this.endRound() : { name: "solving", index: s.index + 1, verdict: null };
    this.changed();
  }

  endRound() {
    if (this.lives > 0) return { name: "round_done" };
    const best = load(recordKey(this.config), 0);
    if (this.recalled > best) save(recordKey(this.config), this.recalled);
    this.record = { best: Math.max(best, this.recalled), isNew: this.recalled > best };
    if (this.record.isNew) play("victory");
    return { name: "series_done" };
  }

  nextRound() {
    if (this.stage.name !== "round_done") return;
    this.round++;
    this.startRound();
  }
}
