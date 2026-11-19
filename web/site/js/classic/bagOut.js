import { h } from "../dom.js";
import { openDialog } from "../dialogs.js";

const WINDOW_MS = 30_000;
const TICK_MS = 200;

export class BagOutPopup {
  constructor({ accept, claim }) {
    this.answer = { accept, claim };
    this.key = null;
  }

  update(state, watching) {
    const due = !watching && state.finalPass && state.myTurn && !state.result && state.finalDeadline;
    const key = due ? state.moves.length : null;
    if (due) this.deadline = state.finalDeadline;
    if (key === this.key) return;
    this.close();
    this.key = key;
    if (due) this.wait = setTimeout(() => this.open(state), Math.max(0, this.left() - WINDOW_MS));
  }

  left() {
    return this.deadline - performance.now();
  }

  open(state) {
    this.ring = h("div.bag-out-ring", {}, h("span"));
    this.closeDialog = openDialog((close) => [
      this.ring,
      h("h2.bag-out-title", {}, "La bolsa se agotó"),
      h("p.bag-out-lead", {}, "La última jugada consistió en:"),
      h("div.bag-out-words", {}, lastWords(state).map((w) => h("span.bag-out-word", {}, w))),
      h("div.bag-out-buttons", {},
        h("button.btn.outlined", { onclick: () => (close(), this.answer.claim()) }, "Reclamar jugada"),
        h("button.btn", { onclick: () => (close(), this.answer.accept()) }, "Aceptar jugada"),
      ),
    ], { dismissable: false, onClose: () => this.stopTicking() });
    this.tick();
    this.ticker = setInterval(() => this.tick(), TICK_MS);
  }

  tick() {
    const ms = Math.max(0, Math.min(this.left(), WINDOW_MS));
    this.ring.style.setProperty("--left", String(ms / WINDOW_MS));
    this.ring.firstChild.textContent = String(Math.ceil(ms / 1000));
    if (ms === 0) this.closeDialog?.();
  }

  stopTicking() {
    clearInterval(this.ticker);
    this.ticker = null;
  }

  close() {
    clearTimeout(this.wait);
    this.stopTicking();
    this.closeDialog?.();
    this.closeDialog = null;
  }
}

function lastWords(state) {
  if (state.finalWords.length) return state.finalWords;
  const word = state.moves.at(-1)?.word;
  return word ? [word] : [];
}
