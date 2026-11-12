import { h, replace } from "../dom.js";
import { lobbyRoute } from "./lobby.js";
import { go } from "../router.js";
import { openDialog, actions, toast } from "../dialogs.js";
import { account } from "./api.js";
import { online } from "./client.js";
import { gameView } from "../classic/view.js";
import { verdict, endDetail } from "../classic/verdict.js";
import { chatInput, chatLine } from "./tell.js";
import { endTools } from "../games/endTools.js";
import { confetti } from "../confetti.js";

const INVALID_MS = 5000;

export function onlineGameScreen(app) {
  if (!account()) return go("cuenta", true);
  const holder = h("div.screen");
  app.append(holder);
  const screen = { holder, view: null, session: null, notice: null, ended: null, adjournAsk: null };
  online.acquire();
  replace(holder, h("div.card.empty-card", {}, h("p", {}, "Entrando en la sala…")));
  const stop = online.subscribe((c) => update(screen, c));
  return () => (stop(), screen.view?.destroy(), screen.stopInvalids?.(), online.release());
}

function update(screen, c) {
  if (!c.room && !c.ended) return go(lobbyRoute(), true);
  if (c.session && c.session !== screen.session) mount(screen, c);
  if (c.notice && c.notice !== screen.notice) toast(c.notice);
  screen.notice = c.notice;
  if (c.ended && c.ended !== screen.ended) closedDialog(c.ended);
  screen.ended = c.ended;
  if (c.adjournAsk && c.adjournAsk !== screen.adjournAsk) adjournDialog(c.adjournAsk);
  screen.adjournAsk = c.adjournAsk;
  screen.chat && drawChat(screen, c);
}

function mount(screen, c) {
  screen.view?.destroy();
  screen.stopInvalids?.();
  screen.session = c.session;
  const sides = seats(c);
  screen.stopInvalids = announceInvalids(c.session, sides);
  screen.chat = h("div.chat-lines.room-chat");
  screen.watchers = h("p.muted.small");
  const chat = chatPanel(screen);
  screen.view = gameView(c.session, {
    me: sides.me,
    opponent: sides.opponent,
    watching: c.room.watching,
    exit: () => exit(c),
    resign: c.room.watching ? null : () => (c.leave(), go(lobbyRoute())),
    onEnd: (state) => endDialog(state, sides, c.room.watching, c.session),
    extra: chat,
    chat,
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
  return { me: { name: me, country: countryOf(c, me) }, opponent: { name: other, country: countryOf(c, other) } };
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

function chatPanel(screen) {
  return h("div.chat-panel", {},
    h("div.row", {}, h("h3.side-title.grow", {}, "Chat"), screen.watchers),
    screen.chat,
    chatInput((text) => online.sayInRoom(text)),
  );
}

function drawChat(screen, c) {
  const lines = c.chat.filter((l) => !l.private);
  replace(screen.chat, lines.length ? lines.map((l) => chatLine(l.mine ? "Tú" : l.from, l.text, l.mine)) : h("p.muted.small", {}, "Sin mensajes."));
  screen.chat.scrollTop = screen.chat.scrollHeight;
  screen.watchers.textContent = c.watchers.length ? `${c.watchers.length} mirando` : "";
  if (!c.rivalOnline && !c.finished()) screen.watchers.textContent = "Rival desconectado";
}

function endDialog(state, sides, watching, session) {
  const rival = sides.opponent.name;
  if (!watching && state.result.outcome === "win") confetti();
  openDialog((close) => [
    h("h2.verdict", {}, verdict(state, rival, watching ? sides.me.name : null)),
    h("p.final-score", {}, `${state.myScore} – ${state.opponentScore}`),
    endDetail(state, rival),
    watching ? null : endTools(session.recorded, state, { me: sides.me.name, opponent: rival }, () => (close(), online.leave())),
    actions(
      h("button.text-btn", { onclick: close }, "Ver tablero"),
      h("button.text-btn", { onclick: () => (close(), online.leave(), go(lobbyRoute())) }, "Volver al lobby"),
    ),
  ]);
}

function closedDialog(reason) {
  openDialog((close) => [
    h("p", {}, reason),
    actions(h("button.text-btn", { onclick: () => (close(), online.leave(), go(lobbyRoute())) }, "Volver al lobby")),
  ], { dismissable: false });
}

function adjournDialog(name) {
  openDialog((close) => [
    h("p", {}, `${name} propone aplazar la partida.`),
    actions(
      h("button.text-btn", { onclick: () => (close(), online.answerAdjourn(false)) }, "Rechazar"),
      h("button.text-btn", { onclick: () => (close(), online.answerAdjourn(true)) }, "Aceptar"),
    ),
  ], { dismissable: false });
}
