import { h, replace } from "../dom.js";
import { lobbyRoute } from "./lobby.js";
import { go } from "../router.js";
import { openDialog, actions, toast, confirm } from "../dialogs.js";
import { watchAway, adjournConfirm, adjournDialog, rivalOf, watchingNotice } from "./adjourn.js";
import { account } from "./api.js";
import { online, ADJOURNED, GAME_GONE } from "./client.js";
import { gameView } from "../classic/view.js";
import { announceEnd, resultPanel } from "../classic/ending.js";
import { writeMessage } from "./messages.js";
import { photoUrl, seatProfile } from "./profile.js";
import { forget } from "../tabMemory.js";

const INVALID_MS = 5000;
const GONE_MS = 2500;
const CLOCK_RUNS = "Tu reloj sigue corriendo.";
const RIVAL_AWAY = "Tu rival se desconectó.";
const RIVAL_CLOCK = " Su reloj sigue corriendo.";

export function onlineGameScreen(app) {
  if (!account()) return go("cuenta", true);
  const holder = h("div.screen");
  app.append(holder);
  const screen = { holder, view: null, session: null, notice: null, ended: null, adjournAsk: null, cover: h("div.game-cover.hidden"), waits: 0 };
  online.acquire();
  replace(holder, h("div.card.empty-card", {}, h("p", {}, "Entrando en la sala…")));
  const stop = online.subscribe((c) => update(screen, c));
  return () => (stop(), screen.view?.destroy(), screen.stopInvalids?.(), forgetView(screen), clearTimeout(screen.awayTimer), clearTimeout(screen.goneTimer), screen.cover.remove(), online.release());
}

function forgetView(screen) {
  if (!screen.memory) return;
  forget(screen.memory + ".seen");
  forget(screen.memory + ".placement");
}

function update(screen, c) {
  if (!c.room && !c.ended) return go(lobbyRoute(), true);
  if (c.session && c.session !== screen.session) mount(screen, c);
  if (c.notice && c.notice !== screen.notice) toast(c.notice);
  screen.notice = c.notice;
  if (c.ended && c.ended !== screen.ended && c.ended !== ADJOURNED && c.ended !== GAME_GONE && !c.room?.watching) closedDialog(c.ended);
  screen.ended = c.ended;
  if (c.adjournAsk && c.adjournAsk !== screen.adjournAsk) adjournDialog(c.adjournAsk);
  screen.adjournAsk = c.adjournAsk;
  screen.view?.setWatchers(c.watchers);
  drawCover(screen, c);
  watchingNotice(c);
  screen.view?.setNotice(noticeOf(c));
  watchAway(screen, c);
}

function noticeOf(c) {
  if (c.status || c.ended || c.rivalOnline || c.finished() || c.room?.watching) return null;
  return c.room?.timed ? RIVAL_AWAY + RIVAL_CLOCK : RIVAL_AWAY;
}

function drawCover(screen, c) {
  const kind = c.ended === GAME_GONE ? "gone" : c.ended === ADJOURNED ? "adjourned" : c.status && !c.ended && c.room ? "offline" : null;
  if (!screen.cover.isConnected) document.getElementById("layer").append(screen.cover);
  screen.cover.classList.toggle("hidden", !kind);
  if (kind === screen.coverKind) return;
  screen.coverKind = kind;
  if (kind === "gone") screen.goneTimer = setTimeout(() => (online.leave(), go("", true)), GONE_MS);
  const back = (leave) => h("button.text-btn", { onclick: () => (leave && online.leave(), go(leave ? lobbyRoute() : "")) }, "Volver");
  const covers = {
    gone: () => [h("h2", {}, "La partida ya no está disponible"), h("p", {}, "Volviendo al menú…")],
    adjourned: () => [h("h2", {}, "Partida aplazada"), h("p", {}, "Se guarda 3 días para retomarla."), back(true)],
    offline: () => [h("h2", {}, "Te desconectaste"), h("div.row.reconnecting", {}, h("div.spinner.small"), h("span", {}, "Reconectando…")), c.room?.timed && !c.room?.watching ? h("p", {}, CLOCK_RUNS) : null, back(false)],
  };
  replace(screen.cover, kind ? h("div.cover-card", {}, covers[kind]()) : null);
}


function mount(screen, c) {
  screen.view?.destroy();
  screen.stopInvalids?.();
  screen.session = c.session;
  const sides = seats(c);
  screen.stopInvalids = c.room.watching ? announceInvalids(c.session, sides) : null;
  screen.memory = c.room.watching ? null : `online.${c.room.id}.${c.room.players.join(".")}`;
  screen.view = gameView(c.session, {
    me: sides.me,
    opponent: sides.opponent,
    watching: c.room.watching,
    thinkingText: "pensando…",
    memory: screen.memory,
    exit: () => exit(c),
    resign: c.room.watching ? null : () => (c.leave(), go(lobbyRoute())),
    adjourn: c.room.watching ? null : () => adjournConfirm(rivalOf(online), online.rivalOnline),
    onEnd: (state, names) => announceEnd(state, names, { watching: c.room.watching }, { recorded: () => (c.room.watching ? c.session.watchedRecord() : c.session.recorded) }),
    endPanel: (state, names) => resultPanel(state, names, { watching: c.room.watching }, { recorded: () => (c.room.watching ? c.session.watchedRecord() : c.session.recorded), menu: () => (online.leave(), go(lobbyRoute())) }),
    chat: () => writeMessage(),
  });
  (screen.holder.isConnected ? screen.holder : document.querySelector(".game"))?.replaceWith(screen.view.el);
}

function announceInvalids(session, sides) {
  let seen = session.state?.moves.length ?? 0;
  return session.subscribe((state) => {
    const fresh = state.moves.length > seen;
    seen = state.moves.length;
    const last = state.moves.at(-1);
    if (fresh && last.kind === "invalid") toast(invalidText(last, sides), INVALID_MS);
  });
}

function invalidText(move, sides) {
  const name = move.byMe ? sides.me.name : sides.opponent.name;
  return `${name} jugó una inválida: ${move.word}`;
}

function seats(c) {
  const [first, second] = c.room.players;
  const me = c.room.watching ? first : c.room.me;
  const other = c.room.watching ? second : c.room.players.find((p) => p !== me) ?? second;
  const side = (name, profile) => ({ name, country: countryOf(c, name), photo: photoUrl(name, countryOf(c, name)), profile });
  return { me: side(me, c.room.watching ? seatProfile : null), opponent: side(other, seatProfile) };
}

function countryOf(c, name) {
  if (name === account().name) return account().country;
  return c.lobby.people.find((p) => p.name === name)?.country ?? "";
}

function exit(c) {
  if (c.room?.watching || c.finished() || c.ended) c.leave();
  else c.parked = c.room?.id;
  go(lobbyRoute());
}

function closedDialog(reason) {
  openDialog((close) => [
    h("p", {}, reason),
    actions(h("button.text-btn", { onclick: () => (close(), online.leave(), go(lobbyRoute())) }, "Volver al lobby")),
  ], { dismissable: false });
}

