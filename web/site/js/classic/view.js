import { setting } from "../settings.js";
import { h, replace } from "../dom.js";
import { icon, bagIcon } from "../icons.js";
import { createBoard } from "../board.js";
import { confirm, themeDialog, toast } from "../dialogs.js";
import { at } from "../placement.js";
import { placementScore } from "../score.js";
import { Placer } from "./placer.js";
import { createRack } from "./rack.js";
import { playerCard } from "./cards.js";
import { askBlank, bagDialog, movesDialog, movesTable, turnsTable } from "./moves.js";
import { flip } from "./state.js";
import { dragFrom } from "./drag.js";
import { chatButton, watchersButton } from "../online/tell.js";
import { tile } from "../board.js";
import { moveButtons } from "../parts.js";
import { bindKeys } from "./keys.js";
import { lookUp } from "../study/lookup.js";
import { floatScore, landTiles } from "../motion.js";
import { recall, remember } from "../tabMemory.js";
import { invalidDialog } from "./invalid.js";

const BANNER_MS = 2200;
const RESIGN_TEXT = "¿Abandonar la partida? Se borra de partidas en curso y cuenta como abandonada en tus estadísticas.";

export function gameView(session, options) {
  const view = new GameView(session, options);
  return { el: view.el, destroy: () => view.destroy(), setNotice: (text) => view.setNotice(text), setWatchers: (list) => view.setWatchers(list) };
}

class GameView {
  constructor(session, options) {
    this.session = session;
    this.options = options;
    this.state = null;
    this.score = null;
    this.placer = new Placer(() => this.placerChanged());
    this.placer.askBlank = askBlank;
    this.board = createBoard({ onSquare: (i) => this.tapSquare(i) });
    this.rack = createRack(this.placer);
    this.cards = { opponent: playerCard(options.opponent), me: playerCard({ ...options.me, mine: !options.watching }) };
    this.el = this.layout();
    this.cover = h("div.handoff.hidden");
    this.el.append(this.cover);
    this.board.el.addEventListener("pointerdown", (e) => this.dragPending(e));
    this.unbindKeys = bindKeys(this);
    this.unsubscribe = session.subscribe((s) => this.show(s));
  }

  layout() {
    this.bar = h("div.game-bar");
    this.buttons = h("div.game-buttons");
    this.notice = h("div.game-notice");
    this.pauseCover = h("div.pause-cover.hidden", {}, h("div.card.pause-card", {}, h("p", {}, "En pausa"), h("button.btn", { onclick: () => this.session.resume() }, "Seguir")));
    this.top = h("div.game-opponent", {}, this.cards.opponent.el);
    this.bottom = h("div.game-me", {}, this.cards.me.el);
    return h("div.game", {},
      this.bar,
      this.top,
      h("div.game-board", {}, this.board.el, this.pauseCover),
      this.bottom,
      h("div.game-rack", {}, this.rack.el),
      this.buttons,
      this.notice,
      h("aside.game-side", {}, this.options.extra ?? null),
    );
  }

  destroy() {
    this.unsubscribe();
    this.unbindKeys();
  }

  show(raw) {
    const state = this.seen(raw);
    const first = !this.state;
    const moved = this.state?.moves.length !== state.moves.length;
    const turnChanged = moved || this.state?.result !== state.result;
    this.state = state;
    this.updateCards();
    this.handOff();
    if (!first && !turnChanged && this.state.paused === this.lastPaused) return;
    this.lastPaused = state.paused;
    const kept = first ? this.keptPlacement() : null;
    this.placer.sync(state.cells, state.rack);
    if (kept) this.placer.restore(kept);
    this.renderAll();
    if (moved && this.freshMove(first)) this.announceMove(first);
    if (state.result && !this.ended) this.end();
  }

  announceMove(first) {
    this.animateMove();
    const last = this.state.moves.at(-1);
    const unseen = !first || this.options.memory;
    if (last?.kind === "invalid" && unseen && !this.options.watching) invalidDialog(last);
    if (!first) this.banner(last);
  }

  banner(last) {
    const text = !last || last.byMe ? null : last.kind === "pass" ? `${this.names().opponent} pasó` : last.kind === "exchange" ? `${this.names().opponent} cambió ${last.count} fichas` : null;
    if (!text) return;
    const el = h("div.opponent-banner", {}, text);
    this.board.el.parentElement?.append(el);
    setTimeout(() => el.remove(), BANNER_MS);
  }

  freshMove(first) {
    const count = this.state.moves.length;
    const seen = this.options.memory ? recall(this.options.memory + ".seen") : null;
    if (this.options.memory) remember(this.options.memory + ".seen", count);
    return !first || seen === null || count > seen;
  }

  animateMove() {
    const last = this.state.moves.at(-1);
    const squares = [...this.state.latest];
    if (!last || !squares.length) return;
    const delay = last.byMe ? 0 : landTiles(this.board, squares);
    floatScore(this.board, this.state.cells, squares, last.score, delay);
  }

  keptPlacement() {
    const kept = this.options.memory ? recall(this.options.memory + ".placement") : null;
    return kept?.moves === this.state.moves.length ? kept : null;
  }

  keepPlacement() {
    if (this.options.memory) remember(this.options.memory + ".placement", { ...this.placer.snapshot(), moves: this.state.moves.length });
  }

  seen(state) {
    this.flipped = !!this.options.watching && !state.myTurn && !state.result;
    return this.flipped ? flip(state) : state;
  }

  tapSquare(i) {
    if (this.state?.result && this.state.cells[i] && !this.placer.pending.size) return lookUp(this.state.cells, i);
    this.placer.clickSquare(i);
  }

  handOff() {
    if (!this.options.local) return;
    const me = this.names().me;
    const covered = !this.state.result && this.shownTo !== me;
    this.cover.classList.toggle("hidden", !covered);
    if (!covered || this.cover.dataset.for === me) return;
    this.cover.dataset.for = me;
    const s = this.state;
    const initial = (n) => (n.trim()[0] ?? "?").toUpperCase();
    replace(this.cover, h("div.handoff-card", {},
      h("div.row.relay", {}, h("span.relay-from", {}, initial(this.names().opponent)), h("b.relay-dots", {}, "• • ›"), h("span.relay-to", {}, initial(me))),
      h("h2.handoff-title", {}, "Pásale el teléfono a ", h("span.accent", {}, me)),
      h("div.row.handoff-chips", {}, [[me, s.myScore], [this.names().opponent, s.opponentScore], ["Turno", s.moves.length + 1]].map(([l, v]) => h("span.handoff-chip", {}, `${l} `, h("b", {}, v)))),
      s.moves.length ? h("div.moves-scroll.handoff-moves", {}, movesTable(s, this.names())) : null,
      h("button.btn.block", { onclick: () => ((this.shownTo = me), this.handOff()) }, `Soy ${me}`),
    ));
  }

  renderAll() {
    this.renderBar();
    this.renderBoard();
    this.rack.render(this.canPlay());
    this.renderButtons();
    this.renderNotice();
    this.pauseCover.classList.toggle("hidden", !this.state.paused);
  }

  placerChanged() {
    if (!this.state) return;
    this.keepPlacement();
    this.renderBoard();
    this.rack.render(this.canPlay());
    this.renderButtons();
    this.liveScore();
  }

  canPlay() {
    return !!this.state && this.state.myTurn && !this.state.result && !this.state.paused && !this.options.watching;
  }

  names() {
    if (this.options.local && this.state) return { me: this.state.host, opponent: this.state.opponent };
    const { me, opponent } = this.options;
    return this.flipped ? { me: opponent.name, opponent: me.name } : { me: me.name, opponent: opponent.name };
  }

  updateCards() {
    const s = this.state;
    const timed = !!s.clocks;
    const thinking = !s.myTurn && !s.result && this.options.thinkingText;
    const names = this.names();
    const [top, bottom] = this.seats();
    top.update({ name: names.opponent, score: s.opponentScore, active: !s.myTurn && !s.result, ms: s.clocks?.opponentMs, timed, tileCount: s.opponentTiles, letters: this.options.watching ? s.opponentRack : null, status: thinking || "" });
    bottom.update({ name: names.me, score: s.myScore, active: s.myTurn && !s.result, ms: s.clocks?.myMs, timed, tileCount: this.options.watching ? 0 : undefined });
  }

  seats() {
    const seats = this.flipped ? [this.cards.me, this.cards.opponent] : [this.cards.opponent, this.cards.me];
    if (this.top.firstChild !== seats[0].el) (this.top.replaceChildren(seats[0].el), this.bottom.replaceChildren(seats[1].el));
    return seats;
  }

  renderBar() {
    const o = this.options;
    replace(this.bar,
      h("button.text-btn", { onclick: () => o.exit() }, "Salir"),
      o.adjourn && !this.state.result ? h("button.text-btn", { onclick: () => o.adjourn() }, "Aplazar") : null,
      o.resign && !this.state.result ? h("button.text-btn", { onclick: () => this.resign() }, "Abandonar") : null,
      h("span.grow"),
      o.pausable && this.state.clocks && !this.state.result ? h("button.icon-btn", { "aria-label": "Pausa", onclick: () => this.togglePause() }, icon(this.state.paused ? "play" : "pause")) : null,
      this.watchers?.length ? null : h("button.icon-btn", { "aria-label": "Tema", onclick: themeDialog }, icon("palette")),
      watchersButton(this.watchers),
      o.chat ? chatButton(o.chat) : null,
      o.adjourn && !this.state.result ? h("button.icon-btn", { "aria-label": "Movidas", onclick: () => movesDialog(this.state, this.names()) }, icon("moves")) : h("button.text-btn.moves-btn", { onclick: () => movesDialog(this.state, this.names()) }, "Movidas"),
      h("button.bag-btn", { "aria-label": "Fichas por salir", onclick: () => bagDialog(this.state) }, bagIcon(this.state.bag)),
    );
  }

  renderBoard() {
    const p = this.placer;
    this.board.render({ cells: this.state.cells, latest: this.state.latest, pending: new Map([...p.pending].map(([i, x]) => [i, x.tile])), arrow: this.canPlay() ? p.arrow : null, score: this.score });
  }

  renderButtons() {
    const p = this.placer;
    if (this.state.result && this.options.endPanel) return replace(this.buttons, this.options.endPanel(this.state, this.names()));
    if (this.options.watching) return this.watchPanel();
    if (p.exchanging) return replace(this.buttons, this.exchangeButtons());
    const enabled = this.canPlay();
    replace(this.buttons, moveButtons(
      h("button.btn", { disabled: !enabled || !p.pending.size, onclick: () => this.play() }, "Jugar"),
      p.pending.size ? h("button.btn.small.tonal", { onclick: () => p.recall() }, "Recoger") : h("button.btn.small.tonal", { onclick: () => p.shuffle() }, "Mezclar"),
      h("button.btn.small.outlined", { disabled: !enabled || !this.state.bag, onclick: () => p.startExchange(this.state.bag) }, "Cambiar"),
      h("button.btn.small.outlined", { disabled: !enabled, onclick: () => this.pass() }, "Pasar"),
    ));
  }

  watchPanel() {
    const s = this.state;
    const list = h("div.watch-moves", {}, turnsTable(s, this.names()));
    replace(this.buttons, h("div.watch-panel", {}, s.result ? h("b", {}, `${this.watchTitle()}   ${s.myScore} – ${s.opponentScore}`) : null, list));
    list.scrollTop = list.scrollHeight;
  }

  watchTitle() {
    const { outcome } = this.state.result;
    if (outcome === "tie") return "Empate";
    return `${outcome === "win" ? this.names().me : this.names().opponent} ganó`;
  }

  exchangeButtons() {
    const n = this.placer.exchanging.size;
    return [
      h("button.btn.small", { disabled: !n, onclick: () => this.exchange() }, `Cambiar ${n}`),
      h("button.btn.small.outlined", { onclick: () => this.placer.cancelExchange() }, "Cancelar"),
    ];
  }

  renderNotice() {
    const text = this.state.botError ?? this.noticeText;
    replace(this.notice, text ? h("p", { class: this.state.botError ? "error" : "game-message" }, text) : null);
  }

  setWatchers(list) {
    const key = (list ?? []).map((w) => w.name + w.country).join("|");
    if (key === this.watchersKey) return;
    this.watchersKey = key;
    this.watchers = list ?? [];
    if (this.state) this.renderBar();
  }

  setNotice(text) {
    if (this.noticeText === text) return;
    this.noticeText = text;
    if (this.state) this.renderNotice();
  }

  liveScore() {
    const p = this.placer.placement();
    this.score = p && setting("liveScore") ? { index: at(p, p.tiles.length - 1), value: placementScore(this.state.cells, p) } : null;
    this.renderBoard();
  }

  async play() {
    const input = this.placer.placementInput();
    if (!input) return toast("Las fichas deben formar una sola palabra en línea.");
    const error = await this.session.play(input);
    if (error) toast(error);
  }

  async exchange() {
    const input = this.placer.exchangeInput();
    if (!input) return;
    const error = await this.session.play(input);
    if (error) toast(error);
    else this.placer.cancelExchange();
  }

  async pass() {
    if (!(await confirm("¿Pasar el turno?", "Pasar"))) return;
    const error = await this.session.play("pasar");
    if (error) toast(error);
  }

  async resign() {
    if (await confirm(RESIGN_TEXT, "Abandonar", "Seguir")) this.options.resign();
  }

  togglePause() {
    if (this.state.paused) this.session.resume();
    else this.session.pause();
  }

  dragPending(e) {
    const el = e.target.closest(".tile.pending");
    if (!el || e.button !== 0) return;
    const from = Number(el.closest(".square").dataset.index);
    const t = this.placer.pending.get(from)?.tile;
    dragFrom(el, e, () => tile(t, "pending"), (target) => this.dropPending(from, target));
  }

  dropPending(from, target) {
    if (target.square) return this.placer.move(from, Number(target.square.dataset.index));
    if (target.rack) this.placer.take(from);
  }

  end() {
    this.ended = true;
    this.options.onEnd?.(this.state, this.names());
  }
}
