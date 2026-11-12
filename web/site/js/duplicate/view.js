import { setting } from "../settings.js";
import { h, replace } from "../dom.js";
import { icon, bagIcon } from "../icons.js";
import { createBoard, tile } from "../board.js";
import { confirm, themeDialog, toast } from "../dialogs.js";
import { at } from "../placement.js";
import { placementScore } from "../score.js";
import { formatClock } from "../parts.js";
import { Placer } from "../classic/placer.js";
import { createRack, face } from "../classic/rack.js";
import { askBlank, bagDialog } from "../classic/moves.js";
import { dragFrom } from "../classic/drag.js";
import { bindKeys } from "../classic/keys.js";
import { totals } from "./state.js";
import { movesDialog, roundDialog, endDialogs } from "./dialogs.js";
import { lookUp } from "../study/lookup.js";

const RESIGN_TEXT = "¿Abandonar la partida? Se borra de partidas en curso y cuenta como abandonada en tus estadísticas.";
const LOW_ROUND_MS = 10000;

export function duplicateView(session, options) {
  const view = new DuplicateView(session, options);
  return { el: view.el, destroy: () => view.destroy() };
}

class DuplicateView {
  constructor(session, options) {
    this.session = session;
    this.options = options;
    this.state = null;
    this.score = null;
    this.placer = new Placer(() => this.placerChanged());
    this.placer.askBlank = askBlank;
    this.board = createBoard({ onSquare: (i) => this.tapSquare(i) });
    this.rack = createRack(this.placer);
    this.el = this.layout();
    this.board.el.addEventListener("pointerdown", (e) => this.dragPending(e));
    this.unbindKeys = bindKeys(this);
    this.unsubscribe = session.subscribe((s) => this.show(s));
  }

  layout() {
    this.bar = h("div.game-bar");
    this.scoreboard = h("div.dup-score");
    this.panel = h("div.dup-panel");
    this.buttons = h("div.game-buttons");
    this.notice = h("div.game-notice");
    this.pauseCover = h("div.pause-cover.hidden", {}, h("div.card.pause-card", {}, h("p", {}, "En pausa"), h("button.btn", { onclick: () => this.session.resume() }, "Seguir")));
    return h("div.game.duplicate-game", {},
      this.bar,
      h("div.game-board", {}, this.board.el, this.pauseCover),
      this.scoreboard,
      this.panel,
      h("div.game-rack", {}, this.rack.el),
      this.buttons,
      this.notice,
    );
  }

  destroy() {
    this.unsubscribe();
    this.unbindKeys();
  }

  show(state) {
    const previous = this.state;
    this.state = state;
    const key = shapeKey(state);
    if (key === this.key) return this.tick();
    this.key = key;
    if (state.phase === "playing" || state.phase === "confirming") this.placer.sync(state.cells, state.rack);
    else this.placer.sync(state.cells, []);
    this.renderAll();
    this.announce(previous, state);
  }

  announce(previous, state) {
    if (!previous) return;
    if (state.finished && !previous.finished) return endDialogs(state);
    if (state.rounds.length > previous.rounds.length) roundDialog(state.rounds.at(-1));
    if (state.lastError && state.lastError !== previous.lastError) toast(state.lastError);
  }

  renderAll() {
    this.renderBar();
    this.renderBoard();
    this.renderScore();
    this.renderPanel();
    this.rack.el.classList.toggle("hidden", !this.showsRack());
    this.rack.render(this.canPlay());
    this.renderButtons();
    this.pauseCover.classList.toggle("hidden", !this.state.paused);
  }

  placerChanged() {
    if (!this.state) return;
    this.renderBoard();
    this.rack.render(this.canPlay());
    this.renderButtons();
    this.liveScore();
  }

  tapSquare(i) {
    const studying = this.options.study || this.state?.finished;
    if (studying && this.state.cells[i] && !this.placer.pending.size) return lookUp(this.state.cells, i);
    if (this.canPlay()) this.placer.clickSquare(i);
  }

  showsRack() {
    return this.state.phase === "playing" || this.state.phase === "confirming";
  }

  canPlay() {
    return !!this.state && this.state.phase === "playing" && !this.state.paused;
  }

  tick() {
    const clock = this.panel.querySelector(".round-clock");
    if (clock) {
      clock.textContent = formatClock(this.state.remainingMs);
      clock.classList.toggle("low", this.state.remainingMs < LOW_ROUND_MS);
    }
    const cancel = this.buttons.querySelector(".confirming .btn");
    if (cancel) cancel.textContent = `Cancelar (${formatClock(Math.min(this.state.remainingMs, this.state.cancelMs))})`;
  }

  renderBar() {
    const live = !this.state.finished;
    replace(this.bar,
      h("button.text-btn", { onclick: () => this.options.exit() }, "Salir"),
      live ? h("button.text-btn", { onclick: () => this.resign() }, "Abandonar") : null,
      h("span.grow"),
      live ? h("button.icon-btn", { "aria-label": "Pausa", onclick: () => this.togglePause() }, icon(this.state.paused ? "play" : "pause")) : null,
      h("button.icon-btn", { "aria-label": "Tema", onclick: themeDialog }, icon("palette")),
      h("button.text-btn.moves-btn", { onclick: () => movesDialog(this.state.rounds) }, "Movidas"),
      h("button.bag-btn", { "aria-label": "Quedan en la bolsa", onclick: () => bagDialog({ unseen: this.state.bag }, "Quedan en la bolsa") }, bagIcon(this.state.bag.length)),
    );
  }

  renderBoard() {
    const p = this.placer;
    this.board.render({ cells: this.state.cells, latest: this.state.latest, pending: new Map([...p.pending].map(([i, x]) => [i, x.tile])), arrow: this.canPlay() ? p.arrow : null, score: this.score });
  }

  renderScore() {
    const t = totals(this.state.rounds);
    replace(this.scoreboard, [["Ronda", this.state.round], ["Tú", t.mine], ["Máster", t.master], ["Aciertos", `${t.hits}/${this.state.rounds.length}`]].map(([label, value]) =>
      h("div.dup-cell", {}, h("span.muted", {}, label), h("b", {}, value)),
    ));
  }

  renderPanel() {
    const s = this.state;
    const panels = {
      waiting: () => h("button.btn.block", { onclick: () => this.session.showRack() }, `Ver atril y arrancar el reloj (${formatClock(s.turnMs)})`),
      invalid_rack: () => invalidRack("¡Mano inválida!", s.invalidRack),
      many_invalid: () => invalidRack("Tras varias manos inválidas se obtiene el atril válido siguiente:", s.rack),
      playing: () => roundBar(s.round, s.remainingMs),
      confirming: () => roundBar(s.round, s.remainingMs),
      finished: () => h("p.section-title", {}, "Partida terminada"),
    };
    replace(this.panel, panels[s.phase]?.() ?? null);
  }

  renderButtons() {
    const p = this.placer;
    const s = this.state;
    if (s.phase === "confirming") return replace(this.buttons, confirming(s, () => this.session.cancel()));
    if (s.phase === "finished") return replace(this.buttons, h("button.btn.small", { onclick: () => endDialogs(s, true) }, "Resultados"), h("button.btn.small.outlined", { onclick: () => this.options.exit() }, "Salir"));
    if (s.phase !== "playing") return replace(this.buttons);
    replace(this.buttons,
      h("button.btn.small", { disabled: !this.canPlay() || !p.pending.size, onclick: () => this.play() }, "Jugar"),
      p.pending.size ? h("button.btn.small.tonal", { onclick: () => p.recall() }, "Recoger") : h("button.btn.small.tonal", { onclick: () => p.shuffle() }, "Mezclar"),
    );
  }

  liveScore() {
    const p = this.placer.placement();
    this.score = p && setting("liveScore") ? { index: at(p, p.tiles.length - 1), value: placementScore(this.state.cells, p) } : null;
    this.renderBoard();
  }

  async play() {
    const input = this.placer.placementInput();
    if (!input) return toast("Las fichas deben formar una sola palabra en línea.");
    const error = await this.session.propose(input);
    if (error) toast(error);
  }

  exchange() {}

  async resign() {
    if (await confirm(RESIGN_TEXT, "Abandonar", "Seguir")) this.options.resign();
  }

  togglePause() {
    if (this.state.paused) this.session.resume();
    else this.session.pause();
  }

  dragPending(e) {
    const el = e.target.closest(".tile.pending");
    if (!el || e.button !== 0 || !this.canPlay()) return;
    const from = Number(el.closest(".square").dataset.index);
    const t = this.placer.pending.get(from)?.tile;
    dragFrom(el, e, () => tile(t, "pending"), (target) => this.dropPending(from, target));
  }

  dropPending(from, target) {
    if (target.square) return this.placer.move(from, Number(target.square.dataset.index));
    if (target.rack) this.placer.take(from);
  }
}

function shapeKey(s) {
  return [s.phase, s.paused, s.rounds.length, s.boardText, s.rack.join(), s.invalidRack.join(), s.proposal, s.lastError].join("|");
}

function roundBar(round, remainingMs) {
  const low = remainingMs < LOW_ROUND_MS;
  return h("div.round-bar", {}, h("span.grow", {}, `Ronda ${round}`), h("span", { class: "round-clock" + (low ? " low" : "") }, formatClock(remainingMs)));
}

function invalidRack(title, rack) {
  return h("div.invalid-rack", {}, h("p.invalid-label", {}, title), h("div.row.invalid-tiles", {}, rack.map((l) => h("div.rack-tile.small", {}, face(l)))));
}

function confirming(s, cancel) {
  const left = Math.min(s.remainingMs, s.cancelMs);
  return h("div.stack.confirming", {}, h("b", {}, `Vas a jugar: ${s.proposal ?? ""}`), h("button.btn.block", { onclick: cancel }, `Cancelar (${formatClock(left)})`));
}
