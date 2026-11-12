import { setting } from "../settings.js";
import { h, replace } from "../dom.js";
import { icon } from "../icons.js";
import { createBoard, tile } from "../board.js";
import { confirm, themeDialog, toast } from "../dialogs.js";
import { at, applyPlacement, parsePlacement, placedSquares } from "../placement.js";
import { placementScore } from "../score.js";
import { formatClock } from "../parts.js";
import { Placer } from "../classic/placer.js";
import { createRack } from "../classic/rack.js";
import { askBlank } from "../classic/moves.js";
import { dragFrom } from "../classic/drag.js";
import { bindKeys } from "../classic/keys.js";
import { rulesIconButton } from "../classic/options.js";
import { resultCard, outcomeBanner, resultSection, wordRow, livesRow } from "../results.js";
import { SPRINT_RULES } from "./rules.js";
import { lookUp } from "../study/lookup.js";

const LOW_TIME_MS = 30000;
const BANNERS = {
  solved: (a) => ["¡SCRABBLE!", a?.word ?? "", a ? `· ${a.at} · ${a.score} pts` : "", true],
  timeout: () => ["SE ACABÓ EL TIEMPO", "Sin scrabble", "", false],
  gave_up: () => ["TE RENDISTE", "Pierdes una vida", "", false],
  invalid: () => ["PALABRA NO VÁLIDA", "Pierdes una vida", "", false],
};

export function sprintView(session, options) {
  const view = new SprintView(session, options);
  return { el: view.el, destroy: () => view.destroy() };
}

class SprintView {
  constructor(session, options) {
    this.session = session;
    this.options = options;
    this.state = null;
    this.shown = null;
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
    this.boardArea = h("div.game-board");
    this.clock = h("div.turn-clock");
    this.buttons = h("div.game-buttons");
    this.results = h("div.stack.sprint-results");
    this.lives = h("div.dup-score.sprint-lives");
    this.pauseCover = h("div.pause-cover.hidden", {}, h("div.card.pause-card", {}, h("p", {}, "En pausa"), h("button.btn", { onclick: () => this.session.resume() }, "Seguir")));
    return h("div.game.sprint-game", {},
      this.bar,
      this.boardArea,
      h("div.dup-panel", {}, this.clock),
      h("div.game-rack", {}, this.rack.el),
      this.buttons,
      h("aside.game-side", {}, this.results),
      this.lives,
    );
  }

  destroy() {
    this.unsubscribe();
    this.unbindKeys();
  }

  show(state) {
    const previous = this.state;
    this.state = state;
    const key = [state.phase, state.paused, state.boardText, state.rack.join(), state.outcome, state.lives, state.solved, state.lastError].join("|");
    if (key === this.key) return this.tick();
    this.key = key;
    if (previous?.boardText !== state.boardText) this.shown = null;
    this.placer.sync(state.cells ?? [], state.phase === "solving" ? state.rack : []);
    this.renderAll();
    if (state.lastError && state.lastError !== previous?.lastError) toast(state.lastError);
  }

  tick() {
    this.clock.textContent = formatClock(this.state.remainingMs);
    this.clock.classList.toggle("low", this.state.remainingMs < LOW_TIME_MS);
  }

  renderAll() {
    this.renderBar();
    this.renderBoardArea();
    this.tick();
    this.clock.classList.toggle("hidden", this.state.phase !== "solving");
    this.rack.el.classList.toggle("hidden", this.state.phase !== "solving");
    this.rack.render(this.canPlay());
    this.renderButtons();
    this.renderResults();
    replace(this.lives, livesRow(this.state.lives, this.state.maxLives, "Resueltas", this.state.solved, this.state.best));
  }

  placerChanged() {
    if (!this.state?.cells) return;
    this.renderBoard();
    this.rack.render(this.canPlay());
    this.renderButtons();
    this.liveScore();
  }

  tapSquare(i) {
    if (this.canPlay()) return this.placer.clickSquare(i);
    const p = this.shown && parsePlacement(this.shown.placement);
    const cells = p ? applyPlacement(this.state.cells, p) : this.state.cells;
    if (cells?.[i]) lookUp(cells, i);
  }

  canPlay() {
    return !!this.state && this.state.phase === "solving" && !this.state.paused;
  }

  renderBar() {
    const live = this.state.phase !== "finished";
    replace(this.bar,
      h("button.text-btn", { onclick: () => this.options.exit() }, "Salir"),
      h("span.bar-title.grow", {}, "Scrabble Sprint"),
      live ? h("button.icon-btn", { "aria-label": "Pausa", onclick: () => (this.state.paused ? this.session.resume() : this.session.pause()) }, icon(this.state.paused ? "play" : "pause")) : null,
      rulesIconButton("Reglas de Scrabble Sprint", SPRINT_RULES),
      h("button.icon-btn", { "aria-label": "Tema", onclick: themeDialog }, icon("palette")),
    );
  }

  renderBoardArea() {
    if (!this.state.cells) return replace(this.boardArea, h("div.searching", {}, h("div.spinner"), h("span", {}, "HastyBot busca un scrabble…")));
    if (!this.boardArea.contains(this.board.el)) replace(this.boardArea, this.board.el, this.pauseCover);
    this.pauseCover.classList.toggle("hidden", !this.state.paused);
    this.renderBoard();
  }

  renderBoard() {
    if (!this.state.cells) return;
    const p = this.shown && parsePlacement(this.shown.placement);
    if (p) return this.board.render({ cells: applyPlacement(this.state.cells, p), latest: new Set(placedSquares(p)) });
    const pending = new Map([...this.placer.pending].map(([i, x]) => [i, x.tile]));
    this.board.render({ cells: this.state.cells, pending, arrow: this.canPlay() ? this.placer.arrow : null, score: this.score });
  }

  renderButtons() {
    const p = this.placer;
    if (this.state.phase !== "solving") return replace(this.buttons);
    replace(this.buttons,
      h("button.btn.small", { disabled: !this.canPlay() || !p.pending.size, onclick: () => this.play() }, "Jugar"),
      p.pending.size ? h("button.btn.small.tonal", { onclick: () => p.recall() }, "Recoger") : h("button.btn.small.tonal", { onclick: () => p.shuffle() }, "Mezclar"),
      h("button.btn.small.outlined", { disabled: !this.canPlay(), onclick: () => this.giveUp() }, "Rendirse"),
    );
  }

  renderResults() {
    const s = this.state;
    if (s.phase === "revealed") return replace(this.results, banner(s), h("button.btn.block", { onclick: () => this.session.next() }, "Siguiente mano"), this.bingoList(s.bingos.length === 1 ? "Había 1 scrabble" : `Había ${s.bingos.length} scrabbles`));
    if (s.phase === "finished") return replace(this.results, this.finished(s));
    replace(this.results);
  }

  finished(s) {
    const why = s.outcome === "timeout" ? "¡TIEMPO!" : "SIN VIDAS";
    const chips = s.solved > s.best ? [["¡Nuevo récord!", true]] : s.best ? [[`Récord: ${s.best}`, false]] : [];
    return [
      resultCard(why, String(s.solved), s.solved === 1 ? "scrabble resuelto" : "scrabbles resueltos", chips),
      h("div.row.result-buttons", {}, h("button.btn.grow", { onclick: () => this.options.restart() }, "Otra serie"), h("button.btn.outlined.grow", { onclick: () => this.options.exit() }, "Salir")),
      s.played.length ? [resultSection("Tus scrabbles"), s.played.map((b) => wordRow(b.word, { score: b.score }))] : null,
      s.bingos.length ? this.bingoList("La última mano") : null,
    ];
  }

  bingoList(title) {
    return [resultSection(title, "Toca para verlo en el tablero"), this.state.bingos.map((b) => wordRow(b.word, { tag: b.at, score: b.score, selected: this.shown?.placement === b.placement, onclick: () => this.showBingo(b) }))];
  }

  showBingo(b) {
    this.shown = this.shown?.placement === b.placement ? null : b;
    this.renderBoard();
    this.renderResults();
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

  async giveUp() {
    if (await confirm("¿Rendirse? Pierdes una vida.", "Rendirse", "Seguir")) this.session.giveUp();
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

function banner(s) {
  const [title, main, detail, good] = (BANNERS[s.outcome] ?? BANNERS.timeout)(s.answer);
  return outcomeBanner(title, main, detail, good);
}
