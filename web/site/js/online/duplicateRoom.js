import { setting } from "../settings.js";
import { fitAround } from "../fit.js";
import { h, replace } from "../dom.js";
import { lobbyRoute } from "./lobby.js";
import { icon, bagIcon } from "../icons.js";
import { go } from "../router.js";
import { createBoard, tile } from "../board.js";
import { confirm, openDialog, actions, themeDialog, toast } from "../dialogs.js";
import { at } from "../placement.js";
import { placementScore } from "../score.js";
import { formatClock, moveButtons, initialAvatar } from "../parts.js";
import { photoAvatar } from "./profile.js";
import { Placer } from "../classic/placer.js";
import { createRack } from "../classic/rack.js";
import { askBlank, duplicateBagDialog } from "../classic/moves.js";
import { movesDialog } from "../duplicate/dialogs.js";
import { dragFrom } from "../classic/drag.js";
import { bindKeys } from "../classic/keys.js";
import { account } from "./api.js";
import { online } from "./client.js";
import { MASTER, standings, placeOf, leftMs } from "./table.js";
import { chatButton, watchersButton } from "./tell.js";
import { adjournConfirm, adjournDialog, watchAway, rivalOf, watchingNotice } from "./adjourn.js";
import { percentChartDialog } from "./percentChart.js";
import { writeMessage, ROOM_CONTACT } from "./messages.js";
import { confetti } from "../confetti.js";
import { exportTable } from "../games/export.js";
import { play } from "../sound.js";

const LOW_ROUND_MS = 10000;
const TICK_MS = 250;

export function duplicateRoomScreen(app) {
  if (!account()) return go("cuenta", true);
  const view = new RoomView();
  const waiting = h("div.screen.room-lobby");
  app.append(waiting);
  online.acquire();
  const stop = online.subscribe((c) => {
    if (!c.table && c.room && !c.ended) return c.room.watching ? replace(waiting, h("header.screen-head", {}, h("h1", {}, "Observando")), h("div.row.room-status", {}, h("div.spinner.small"), h("b", {}, "Conectando…"))) : drawWaiting(waiting, c);
    if (waiting.isConnected) waiting.replaceWith(view.el);
    view.update(c);
  });
  const ticker = setInterval(() => view.tick(), TICK_MS);
  return () => (stop(), clearInterval(ticker), view.destroy(), online.release());
}

class RoomView {
  constructor() {
    this.away = { waits: 0 };
    this.placer = new Placer(() => this.placerChanged());
    this.placer.askBlank = askBlank;
    this.board = createBoard({ onSquare: (i) => this.canPlay() && this.placer.clickSquare(i) });
    this.rack = createRack(this.placer);
    this.bar = h("div.game-bar");
    this.panel = h("div.dup-panel");
    this.buttons = h("div.game-buttons");
    this.notice = h("div.game-notice");
    this.side = h("div.stack");
    this.el = h("div.game.duplicate-game", {}, this.bar, h("div.game-board", {}, this.board.el), this.panel,
      h("div.game-rack", {}, this.rack.el), this.buttons, this.notice,
      h("aside.game-side", {}, this.side));
    this.stopFit = fitAround(this.el, this.el.querySelector(".game-board"));
    this.board.el.addEventListener("pointerdown", (e) => this.dragPending(e));
    this.unbindKeys = bindKeys(this);
  }

  destroy() {
    this.stopFit?.();
    this.unbindKeys();
    clearTimeout(this.away.awayTimer);
  }

  celebrate(t) {
    const won = standings(t).find((e) => e.name !== MASTER)?.name === t.me;
    play(won ? "victory" : "game_over");
    if (won || (this.watching && standings(t).filter((e) => e.name !== MASTER).filter((e, i, all) => e.total === all[0].total).length === 1)) confetti();
  }

  update(c) {
    if (!c.room && !c.ended) return go(lobbyRoute(), true);
    this.client = c;
    this.watching = !!c.room?.watching;
    if (c.notice && c.notice !== this.lastNotice) toast(c.notice);
    this.lastNotice = c.notice;
    if (c.ended && c.ended !== this.lastEnded) closedDialog(c.ended);
    this.lastEnded = c.ended;
    watchingNotice(c);
    if (c.adjournAsk && c.adjournAsk !== this.adjournAsk) adjournDialog(c.adjournAsk);
    this.adjournAsk = c.adjournAsk;
    if (c.room?.players.length === 2) watchAway(this.away, c);
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
    const pair = (this.client?.room?.players.length ?? 0) === 2;
    const adjourn = live && pair;
    const watchers = this.client?.watchers ?? [];
    const moves = () => movesDialog(roundsOf(t, this.watching), this.watching ? "Mejor" : "Tú");
    replace(this.bar,
      h("button.text-btn", { onclick: () => this.exit() }, "Salir"),
      adjourn ? h("button.text-btn", { onclick: () => adjournConfirm(rivalOf(online), online.rivalOnline) }, "Aplazar") : null,
      live && !pair ? h("button.text-btn", { onclick: () => this.abandon() }, "Abandonar") : null,
      h("span.grow"),
      watchers.length ? null : h("button.icon-btn", { "aria-label": "Tema", onclick: themeDialog }, icon("palette")),
      watchersButton(watchers),
      this.watching ? chatButton(() => writeMessage()) : null,
      h("button.icon-btn", { "aria-label": "Evolución", onclick: () => percentChartDialog(t, t.me, true) }, icon("chart")),
      t.phase !== "playing" && !this.watching ? h("button.text-btn", { onclick: () => this.standingsDialog() }, "Posiciones") : null,
      adjourn ? h("button.icon-btn", { "aria-label": "Movidas", onclick: moves }, icon("moves")) : h("button.text-btn.moves-btn", { onclick: moves }, "Movidas"),
      h("button.bag-btn", { "aria-label": "Quedan en la bolsa", onclick: () => duplicateBagDialog(t.unseen) }, bagIcon(t.unseen.length)),
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
      const proposal = !this.watching ? proposalCard(t.proposal) : null;
      return replace(this.panel, h("div.round-bar", {}, h("span.grow", {}, `Ronda ${t.round}`), h("span.round-clock", {}, formatClock(leftMs(t) ?? 0))), proposal);
    }
    if (t.phase === "finished") return replace(this.panel, h("b.phase-title", {}, "Partida terminada"), this.watching ? leaderboard(t) : standingsTable(t),
      h("button.btn.outlined.block", { onclick: () => this.exportTable() }, "Exportar"), h("button.btn.block", { onclick: () => this.exit() }, "Salir"));
    replace(this.panel,
      h("b.phase-title", {}, `Resultados de la ronda ${t.round}`),
      h("button.btn.outlined.block", { onclick: () => this.resultsDialog() }, "Ver resultados"),
      this.watching ? null : t.iAmReady ? h("span.muted", {}, `Esperando la nueva ronda: ${t.ready} de ${t.waiting} listos`) : h("button.btn.block", { onclick: () => online.ready() }, "Comenzar nueva ronda"),
    );
  }

  renderButtons() {
    if (!this.canPlay()) return replace(this.buttons);
    const p = this.placer;
    replace(this.buttons, moveButtons(
      h("button.btn", { disabled: !p.pending.size, onclick: () => this.play() }, "Jugar"),
      h("button.btn.small.outlined.chat-small", { "aria-label": "Mensaje", onclick: () => writeMessage() }, "💬"),
      p.pending.size ? h("button.btn.small.outlined", { onclick: () => p.recall() }, "Recoger") : h("button.btn.small.outlined", { onclick: () => p.shuffle() }, "Mezclar"),
      h("button.btn.small.outlined", { onclick: () => this.standingsDialog() }, "Posiciones"),
    ));
  }

  renderSide() {
    replace(this.side, this.watching && this.t.phase === "playing" && this.t.live ? leaderboard(this.t) : null);
  }

  exportTable() {
    exportTable(this.t);
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
      this.watching ? leaderboard(t, null, true) : standingsTable(t),
      actions(canStart ? h("button.text-btn", { onclick: close }, "Cerrar") : null, canStart ? h("button.btn.small", { onclick: () => (close(), online.ready()) }, "Comenzar nueva ronda") : h("button.text-btn", { onclick: close }, "Cerrar")),
    ], { wide: true });
  }

  standingsDialog() {
    openDialog((close) => [h("h2", {}, "Posiciones"), leaderboard(this.t, this.t.me, true), actions(h("button.text-btn", { onclick: close }, "Cerrar"))], { wide: true });
  }

  verdictDialog() {
    const t = this.t;
    const players = standings(t).filter((e) => e.name !== MASTER);
    const top = players[0];
    if (!top) return;
    const winners = players.filter((e) => e.total === top.total);
    const title = winners.length !== 1 ? "Empate" : !this.watching && winners[0].name === t.me ? "¡Ganaste!" : `${winners[0].name} ganó`;
    const tone = winners.length === 1 && (this.watching || winners[0].name === t.me) ? "win" : "neutral";
    const layer = h("div.verdict-layer", { onclick: (e) => e.target === layer && layer.remove() },
      h("div.verdict-content.final-standings", {},
        h("div", { class: `verdict-title small ${tone}` }, title),
        h("div.final-table", {}, leaderboard(t, this.watching ? null : t.me, true)),
        h("button.btn.block", { onclick: () => (layer.remove(), online.leave(), go(lobbyRoute())) }, "Volver al lobby"),
      ),
    );
    document.getElementById("layer").append(layer);
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

function leaderboard(t, me = null, scroll = false) {
  const sorted = standings(t);
  const master = sorted.find((e) => e.name === MASTER)?.total ?? 0;
  const known = sorted.filter((e) => e.name !== MASTER);
  const rows = known.length ? known : t.players.map((name) => ({ name, text: "", points: -1, total: -1 }));
  return h("div", { class: "stack leaderboard" + (scroll ? " scrolls" : "") }, rows.map((e, i) => {
    const place = placeOf(rows, i);
    const tone = e.name === me ? " mine" : place === 1 && e.total > 0 ? " leader" : "";
    return h("div", { class: "leader-card" + tone },
      h("b.leader-place", {}, place), seatAvatar(e.name),
      h("div.grow", {}, h("b.ellipsis", {}, e.name), h("div.muted.small.ellipsis", {}, `Ronda ${e.points < 0 ? "—" : "+" + e.points}`)),
      h("div.leader-total", {}, h("b", {}, e.total < 0 ? "—" : e.total), h("span.muted.small", {}, e.total < 0 || master <= 0 ? "—" : `${((e.total * 100) / master).toLocaleString("es", { maximumFractionDigits: 1, minimumFractionDigits: 1 })} %`)),
    );
  }));
}

function seatAvatar(name) {
  const country = online.lobby.people.find((p) => p.name === name)?.country ?? "";
  return photoAvatar(name, country, 32);
}

function proposalCard(p) {
  if (!p) return h("div.app-proposal", {}, h("span.muted", {}, "Aún no has jugado"));
  const space = p.text.indexOf(" ");
  const coords = space > 0 ? p.text.slice(0, space) : "";
  return h("div.app-proposal", {},
    h("div.grow.stack", {}, h("span.small.muted", {}, "Tu jugada"), h("div.row.proposal-play", {}, coords ? h("span.muted", {}, coords) : null, h("b", {}, p.text.slice(space + 1)))),
    h("b.points-pill", {}, `${p.points} pts`));
}

function standingsTable(t) {
  const sorted = standings(t);
  return h("div.standings-table", {},
    h("div.standings-row.head", {}, h("span", {}, "#"), h("span", {}, "Jugador"), h("span", {}, "Jugada"), h("span.num", {}, "Pts"), h("span.num", {}, "Total")),
    sorted.map((e, i) => h("div", { class: "standings-row" + (e.name === t.me ? " mine" : "") },
      h("span", {}, placeOf(sorted, i)), h("span", { class: e.name === t.me || e.name === MASTER ? "bold" : "" }, e.name), h("span", {}, e.text), h("span.num", {}, e.points), h("b.num", {}, e.total))),
  );
}

function closedDialog(reason) {
  openDialog((close) => [h("p", {}, reason), actions(h("button.text-btn", { onclick: () => (close(), online.leave(), go(lobbyRoute())) }, "Volver al lobby"))], { dismissable: false });
}

const CANCEL_ROOM = ["Cancelar partida", "¿Seguro que quieres cancelar la partida?"];
const LEAVE_ROOM = ["Salir de la sala", "¿Seguro que quieres salir de la sala?"];

function drawWaiting(el, c) {
  const host = !!c.room.first;
  const players = c.room.players ?? [];
  const tiles = [...players.map((p) => playerTile(p, p === c.room.host, p === c.room.me)), host ? h("div.room-tile.waiting", {}, h("div.spinner.small"), h("span", {}, "Esperando…")) : null].filter(Boolean);
  const [label, question] = host ? CANCEL_ROOM : LEAVE_ROOM;
  const exit = async () => {
    if (!(await confirm(question, label, "Seguir en la sala"))) return;
    c.leave();
    go(lobbyRoute(), true);
  };
  replace(el,
    h("header.screen-head", {}, h("h1", {}, `Sala de ${c.room.host}`)),
    h("p.section-title", {}, `Jugadores · ${players.length}`),
    h("div.room-grid", {}, tiles),
    host ? h("button.btn.block.room-start", { disabled: players.length < 2, onclick: () => c.startSeek({ id: c.room.id }) }, "Empezar la partida")
      : h("div.row.room-status", {}, h("div.spinner.small"), h("b", {}, c.notice ?? `${c.room.host} empezará la partida en breve`)),
    h("button.btn.outlined.block", { onclick: () => writeMessage(ROOM_CONTACT) }, "💬  Mensaje a la sala"),
    h("button.text-btn.block.danger", { onclick: exit }, label),
  );
}

function playerTile(name, host, mine) {
  return h("div", { class: "room-tile" + (mine ? " mine" : "") }, initialAvatar(name, 40), h("div.stack", {}, h("b.ellipsis", {}, name), h("span.small.muted", {}, host ? "Crea la sala" : "Conectado")));
}
