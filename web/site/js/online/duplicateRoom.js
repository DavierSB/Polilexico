import { setting } from "../settings.js";
import { h, replace } from "../dom.js";
import { lobbyRoute } from "./lobby.js";
import { icon, bagIcon } from "../icons.js";
import { go } from "../router.js";
import { createBoard, tile } from "../board.js";
import { confirm, openDialog, actions, themeDialog, toast } from "../dialogs.js";
import { at } from "../placement.js";
import { placementScore } from "../score.js";
import { formatClock } from "../parts.js";
import { photoAvatar } from "./profile.js";
import { Placer } from "../classic/placer.js";
import { createRack } from "../classic/rack.js";
import { askBlank, bagDialog } from "../classic/moves.js";
import { movesDialog } from "../duplicate/dialogs.js";
import { dragFrom } from "../classic/drag.js";
import { bindKeys } from "../classic/keys.js";
import { account } from "./api.js";
import { online } from "./client.js";
import { MASTER, standings, placeOf, leftMs } from "./table.js";
import { chatInput, chatLine, chatButton } from "./tell.js";
import { confetti } from "../confetti.js";
import { play } from "../sound.js";

const LOW_ROUND_MS = 10000;
const TICK_MS = 250;

export function duplicateRoomScreen(app) {
  if (!account()) return go("cuenta", true);
  const view = new RoomView();
  app.append(view.el);
  online.acquire();
  const stop = online.subscribe((c) => view.update(c));
  const ticker = setInterval(() => view.tick(), TICK_MS);
  return () => (stop(), clearInterval(ticker), view.destroy(), online.release());
}

class RoomView {
  constructor() {
    this.placer = new Placer(() => this.placerChanged());
    this.placer.askBlank = askBlank;
    this.board = createBoard({ onSquare: (i) => this.canPlay() && this.placer.clickSquare(i) });
    this.rack = createRack(this.placer);
    this.bar = h("div.game-bar");
    this.panel = h("div.dup-panel");
    this.buttons = h("div.game-buttons");
    this.notice = h("div.game-notice");
    this.side = h("div.stack");
    this.chat = h("div.chat-lines.room-chat");
    this.chatPanel = h("div.chat-panel", {}, h("h3.side-title", {}, "Chat"), this.chat, chatInput((t) => online.sayInRoom(t)));
    this.el = h("div.game.duplicate-game", {}, this.bar, h("div.game-board", {}, this.board.el), this.panel,
      h("div.game-rack", {}, this.rack.el), this.buttons, this.notice,
      h("aside.game-side", {}, this.side, this.chatPanel));
    this.board.el.addEventListener("pointerdown", (e) => this.dragPending(e));
    this.unbindKeys = bindKeys(this);
  }

  destroy() {
    this.unbindKeys();
  }

  celebrate(t) {
    const won = standings(t).find((e) => e.name !== MASTER)?.name === t.me;
    play(won ? "victory" : "game_over");
    if (won && !this.watching) confetti();
  }

  update(c) {
    if (!c.room && !c.ended) return go(lobbyRoute(), true);
    this.client = c;
    this.watching = !!c.room?.watching;
    if (c.notice && c.notice !== this.lastNotice) toast(c.notice);
    this.lastNotice = c.notice;
    if (c.ended && c.ended !== this.lastEnded) closedDialog(c.ended);
    this.lastEnded = c.ended;
    this.drawChat(c);
    if (!c.table || c.table === this.t) return;
    const previous = this.t;
    this.t = c.table;
    this.placer.sync(this.t.cells, this.t.phase === "playing" ? this.t.rack : []);
    this.renderAll();
    this.announce(previous, this.t);
  }

  announce(previous, t) {
    if (previous && t.rounds.length > previous.rounds.length && !this.watching) {
      const last = t.rounds.at(-1).entries;
      const mine = last.find((e) => e.name === t.me)?.points ?? -1;
      play(mine === last.find((e) => e.name === MASTER)?.points ? "correct" : "miss");
    }
    if (previous && t.phase === "finished" && previous.phase !== "finished") this.celebrate(t);
    if (t.phase === "results" && (previous?.phase !== "results" || previous.round !== t.round)) this.resultsDialog();
    if (t.phase === "finished" && previous?.phase !== "finished") this.verdictDialog();
  }

  canPlay() {
    return !!this.t && this.t.phase === "playing" && !this.watching;
  }

  renderAll() {
    this.renderBar();
    this.renderBoard();
    this.renderPanel();
    this.rack.el.classList.toggle("hidden", this.t.phase !== "playing");
    this.rack.render(this.canPlay());
    this.renderButtons();
    this.renderSide();
  }

  placerChanged() {
    if (!this.t) return;
    this.renderBoard();
    this.rack.render(this.canPlay());
    this.renderButtons();
  }

  tick() {
    const clock = this.panel.querySelector(".round-clock");
    if (!clock || !this.t) return;
    const left = leftMs(this.t) ?? 0;
    clock.textContent = formatClock(left);
    clock.classList.toggle("low", left < LOW_ROUND_MS);
  }

  renderBar() {
    const t = this.t;
    const live = t.phase !== "finished" && !this.watching;
    replace(this.bar,
      h("button.text-btn", { onclick: () => this.exit() }, "Salir"),
      live ? h("button.text-btn", { onclick: () => this.abandon() }, "Abandonar") : null,
      h("span.grow"),
      h("button.icon-btn", { "aria-label": "Tema", onclick: themeDialog }, icon("palette")),
      chatButton(this.chatPanel),
      t.phase !== "playing" || this.watching ? h("button.text-btn", { onclick: () => this.standingsDialog() }, "Posiciones") : null,
      h("button.text-btn.moves-btn", { onclick: () => movesDialog(roundsOf(t, this.watching), this.watching ? "Mejor" : "Tú") }, "Movidas"),
      h("button.bag-btn", { "aria-label": "Quedan en la bolsa", onclick: () => bagDialog({ unseen: t.unseen }, "Quedan en la bolsa") }, bagIcon(t.unseen.length)),
    );
  }

  renderBoard() {
    const p = this.placer;
    const pending = new Map([...p.pending].map(([i, x]) => [i, x.tile]));
    const placement = p.placement();
    const score = placement && setting("liveScore") ? { index: at(placement, placement.tiles.length - 1), value: placementScore(this.t.cells, placement) } : null;
    this.board.render({ cells: this.t.cells, latest: this.t.latest, pending, arrow: this.canPlay() ? p.arrow : null, score });
  }

  renderPanel() {
    const t = this.t;
    if (t.phase === "playing") {
      const proposal = !this.watching ? h("div.proposal-card", {}, t.proposal ? [h("span.muted.small", {}, "Tu jugada"), h("div.row", {}, h("b.grow", {}, t.proposal.text), h("b", {}, t.proposal.points))] : h("span.muted", {}, "Aún no has jugado")) : null;
      return replace(this.panel, h("div.round-bar", {}, h("span.grow", {}, `Ronda ${t.round}`), h("span.round-clock", {}, formatClock(leftMs(t) ?? 0))), proposal);
    }
    if (t.phase === "finished") return replace(this.panel, h("p.section-title", {}, "Partida terminada"), h("button.btn.block", { onclick: () => this.exit() }, "Salir"));
    replace(this.panel,
      h("b", {}, `Resultados de la ronda ${t.round}`),
      h("button.btn.outlined.block", { onclick: () => this.resultsDialog() }, "Ver resultados"),
      this.watching ? null : t.iAmReady ? h("span.muted", {}, `Esperando la nueva ronda: ${t.ready} de ${t.waiting} listos`) : h("button.btn.block", { onclick: () => online.ready() }, "Comenzar nueva ronda"),
    );
  }

  renderButtons() {
    if (!this.canPlay()) return replace(this.buttons);
    const p = this.placer;
    replace(this.buttons,
      h("button.btn.small", { disabled: !p.pending.size, onclick: () => this.play() }, "Jugar"),
      p.pending.size ? h("button.btn.small.tonal", { onclick: () => p.recall() }, "Recoger") : h("button.btn.small.tonal", { onclick: () => p.shuffle() }, "Mezclar"),
    );
  }

  renderSide() {
    const showTable = this.t.phase !== "playing" || this.t.live;
    replace(this.side, showTable && this.t.rounds.length ? [h("h3.side-title", {}, "Posiciones"), leaderboard(this.t)] : null);
  }

  drawChat(c) {
    const lines = c.chat.filter((l) => !l.private);
    replace(this.chat, lines.length ? lines.map((l) => chatLine(l.mine ? "Tú" : l.from, l.text, l.mine)) : h("p.muted.small", {}, "Sin mensajes."));
    this.chat.scrollTop = this.chat.scrollHeight;
  }

  play() {
    const input = this.placer.placementInput();
    if (!input) return toast("Las fichas deben formar una sola palabra en línea.");
    online.move(input);
    this.placer.recall();
  }

  exchange() {}

  async abandon() {
    if (await confirm("¿Seguro que quieres abandonar la partida?", "Abandonar", "Seguir")) (online.leave(), go(lobbyRoute()));
  }

  exit() {
    const c = this.client;
    if (this.watching || c.finished() || c.ended) c.leave();
    else c.parked = c.room?.id;
    go(lobbyRoute());
  }

  resultsDialog() {
    const t = this.t;
    const canStart = !this.watching && !t.iAmReady && t.phase === "results";
    openDialog((close) => [
      h("h2", {}, `Resultados de la ronda ${t.round}`),
      leaderboard(t),
      actions(h("button.text-btn", { onclick: close }, "Cerrar"), canStart ? h("button.text-btn", { onclick: () => (close(), online.ready()) }, "Comenzar nueva ronda") : null),
    ], { wide: true });
  }

  standingsDialog() {
    openDialog((close) => [h("h2", {}, "Posiciones"), leaderboard(this.t), actions(h("button.text-btn", { onclick: close }, "Cerrar"))], { wide: true });
  }

  verdictDialog() {
    const t = this.t;
    const players = standings(t).filter((e) => e.name !== MASTER);
    const top = players[0];
    if (!top) return;
    const winners = players.filter((e) => e.total === top.total);
    const title = winners.length !== 1 ? "Empate" : !this.watching && winners[0].name === t.me ? "¡Ganaste!" : `${winners[0].name} ganó`;
    openDialog((close) => [
      h("h2.verdict", {}, title),
      leaderboard(t),
      actions(h("button.text-btn", { onclick: close }, "Ver tablero"), h("button.text-btn", { onclick: () => (close(), online.leave(), go(lobbyRoute())) }, "Volver al lobby")),
    ], { wide: true });
  }

  dragPending(e) {
    const el = e.target.closest(".tile.pending");
    if (!el || e.button !== 0 || !this.canPlay()) return;
    const from = Number(el.closest(".square").dataset.index);
    const t = this.placer.pending.get(from)?.tile;
    dragFrom(el, e, () => tile(t, "pending"), (target) => {
      if (target.square) return this.placer.move(from, Number(target.square.dataset.index));
      if (target.rack) this.placer.take(from);
    });
  }
}

function roundsOf(t, watching) {
  return t.rounds.map((r) => roundOf(r, watching ? bestOf(r) : r.entries.find((e) => e.name === t.me))).filter(Boolean);
}

function roundOf(r, other) {
  const master = r.entries.find((e) => e.name === MASTER);
  if (!master || !other) return null;
  const play = (e) => ({ text: e.text, score: e.points });
  return { number: r.number, master: play(master), mine: play(other), hit: other.points > 0 && other.points === master.points };
}

function bestOf(r) {
  return r.entries.filter((e) => e.name !== MASTER).reduce((best, e) => (!best || e.points > best.points ? e : best), null);
}

function leaderboard(t) {
  const sorted = standings(t);
  const master = sorted.find((e) => e.name === MASTER)?.total ?? 0;
  const known = sorted.filter((e) => e.name !== MASTER);
  const rows = known.length ? known : t.players.map((name) => ({ name, text: "", points: -1, total: -1 }));
  return h("div.stack.leaderboard", {}, rows.map((e, i) => {
    const place = placeOf(rows, i);
    const tone = e.name === t.me ? " mine" : place === 1 && e.total > 0 ? " leader" : "";
    return h("div", { class: "leader-card" + tone },
      h("b.leader-place", {}, place), photoAvatar(e.name, "", 32),
      h("div.grow", {}, h("b.ellipsis", {}, e.name), h("div.muted.small.ellipsis", {}, `Ronda ${e.points < 0 ? "—" : "+" + e.points}${e.text ? " · " + e.text : ""}`)),
      h("div.leader-total", {}, h("b", {}, e.total < 0 ? "—" : e.total), h("span.muted.small", {}, e.total < 0 || master <= 0 ? "—" : `${((e.total * 100) / master).toLocaleString("es", { maximumFractionDigits: 1, minimumFractionDigits: 1 })} %`)),
    );
  }), master ? h("div.leader-card.master", {}, h("b.leader-place", {}, "·"), h("div.grow", {}, h("b", {}, MASTER)), h("div.leader-total", {}, h("b", {}, master))) : null);
}

function closedDialog(reason) {
  openDialog((close) => [h("p", {}, reason), actions(h("button.text-btn", { onclick: () => (close(), online.leave(), go(lobbyRoute())) }, "Volver al lobby"))], { dismissable: false });
}
