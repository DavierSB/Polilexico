import { h, replace } from "../dom.js";
import { go } from "../router.js";
import { nameFlag, formatClock, flag } from "../parts.js";
import { openDialog, actions } from "../dialogs.js";
import { account, logOut } from "./api.js";
import { online } from "./client.js";
import { writeMessage } from "./messages.js";
import { onPresence } from "./presence.js";
import { face } from "../classic/rack.js";
import { photoAvatar, profileDialog } from "./profile.js";
import { load, save } from "../store.js";
import { fitArea } from "./fit.js";
import { blockedByGame } from "./involved.js";

export function lobbyRoute() {
  return load("lobbyTab", "classic") === "duplicate" ? "duplicada" : "clasica";
}

export function lobbyScreen() {
  go(lobbyRoute(), true);
}

export function onlinePanel(duplicate) {
  save("lobbyTab", duplicate ? "duplicate" : "classic");
  if (!account()) return noProfileCard();
  const lower = fitArea();
  const ui = { notice: h("div.stack.lobby-notice"), top: h("section.lobby-col.lobby-top"), lower, status: h("p.muted.lobby-status"), publish: h("div.lobby-publish"), duplicate };
  const el = h("div.online-panel", {}, ui.status, ui.notice, h("div.lobby", {}, ui.top, lower.el), ui.publish);
  online.acquire();
  const stop = online.subscribe((c) => draw(c, ui));
  return { el, destroy: () => (stop(), lower.stop(), online.release()) };
}

function noProfileCard() {
  const count = h("span.muted");
  const el = h("div.card.empty-card.no-profile", {}, h("span.glyph-big", {}, "👥"), h("b", {}, "Jugar en línea"), count,
    h("button.btn.block", { onclick: () => go("cuenta") }, "Crear perfil"));
  const stop = onPresence((n) => (count.textContent = n > 0 ? (n === 1 ? "1 persona conectada" : `${n} personas conectadas`) : ""));
  return { el, destroy: stop };
}

export function peopleDialog() {
  const body = h("div.stack.dialog-list");
  let stop = null;
  openDialog((close) => {
    online.acquire();
    stop = online.subscribe((c) => replace(body, peopleList(c, others(c), close)));
    return [h("h2", {}, "Conectados"), body, actions(h("button.text-btn", { onclick: close }, "Cerrar"))];
  }, { onClose: () => (stop?.(), online.release()), wide: true });
}

function others(c) {
  const me = account();
  return c.lobby.people.filter((p) => !(p.name === me?.name && p.country === me?.country));
}

function draw(c, ui) {
  const room = roomRoute(c);
  const live = room && !c.ended && !c.finished();
  if (live && c.parked !== c.room.id) return go(room);
  const dup = ui.duplicate;
  ui.status.textContent = c.refused ?? c.status ?? c.ended ?? "";
  replace(ui.notice,
    c.refused ? h("button.btn", { onclick: () => (logOut(), go("cuenta")) }, "Entrar de nuevo") : null,
    live ? h("button.btn.back-to-game", { onclick: () => ((c.parked = null), go(room)) }, "Volver a la partida") : null,
    waitingRoom(c),
  );
  replace(ui.top, modeOffers(c, dup));
  verdictNotice(c);
  ui.lower.show({ games: games(c, dup), people: dup ? null : challengeable(c) });
  replace(ui.publish, dup
    ? h("button.btn.block.lobby-seek", { onclick: () => go("crear-duplicada") }, "Crear duplicada")
    : h("div.row.lobby-buttons", {},
      h("button.btn.grow.lobby-seek", { onclick: () => go("buscar") }, "Buscar clásica"),
      h("button.btn.outlined.grow.lobby-seek", { onclick: () => go("terminadas/online") }, "Terminadas")));
}

function modeOffers(c, dup) {
  const list = c.lobby.seeks.filter((s) => (s.mode === "duplicate") === dup);
  const sent = list.filter((s) => s.mine && s.to);
  const offers = list.filter((s) => !(s.mine && s.to)).sort((a, b) => !!b.mine - !!a.mine);
  const pending = c.lobby.adjourned.filter((a) => !!a.duplicate === dup && !a.result);
  const gamesHere = c.lobby.games.some((g) => (g.mode === "duplicate") === dup);
  return [
    section("Retos enviados", sent.map((s) => seekRow(c, s, dup))),
    section("Ofertas de partidas", offers.map((s) => seekRow(c, s, dup))),
    section("Partidas aplazadas", pending.map((a) => adjournedRow(c, a))),
    sent.length || offers.length || pending.length || gamesHere ? null : dup ? noDuplicates(c) : emptyState("C", "No hay ofertas ni partidas en curso."),
  ];
}

export function roomRoute(c) {
  if (!c.room) return null;
  if (c.room.duplicate) return c.table || !c.room.watching ? "sala-duplicada" : null;
  return c.session || c.room.watching ? "sala" : null;
}


function waitingRoom(c) {
  if (!c.room?.duplicate || c.table || c.room.watching || c.ended) return null;
  const mine = c.lobby.seeks.find((s) => s.id === c.room.id);
  if (mine?.mine) return null;
  return h("div.card.offer-card", {},
    h("b", {}, `Sala de ${c.room.host}`), h("span.muted.small", {}, mine ? duplicateDetail(mine) : "Esperando que empiece"),
    h("div.row", {}, h("span.muted.small.grow", {}, "Esperando que empiece"), h("button.text-btn", { onclick: () => c.leave() }, "Salir de la sala")),
  );
}

export function duplicateDetail(s) {
  if (s.resume) return "Partida aplazada · duplicada";
  return `👥 ${s.players} en la sala · ${formatClock(s.turnMs)} por ronda`;
}

function section(title, rows) {
  if (!rows.length) return null;
  return h("div.lobby-section", {}, h("h3.side-title", {}, title), h("div.stack", {}, rows));
}

function emptyState(letter, ...text) {
  return h("div.empty-state", {}, letterTile(letter), h("span.empty-text", {}, text));
}

function letterTile(letter) {
  return h("div.rack-tile.small.empty-tile", {}, face(letter));
}

function playerRow(name, country, detail, mine, ...buttons) {
  return h("div", { class: "player-row" + (mine ? " mine" : "") },
    h("button.avatar-btn.row.grow.identity", { "aria-label": `Perfil de ${name}`, onclick: () => profileDialog({ name, country, playing: false }) },
      photoAvatar(name, country, 40),
      h("div.grow.identity-text", {}, h("div", {}, nameFlag(name, country)), detail ? h("div.muted.small.ellipsis", {}, detail) : null)),
    h("div.row.row-actions", {}, buttons),
  );
}

function chatButton(p) {
  return h("button.icon-btn.chat-glyph", { "aria-label": `Escribir a ${p.name}`, onclick: () => writeMessage(p) }, "💬");
}

export function classicTime(s) {
  return s.untimed ? "Sin tiempo" : `${formatClock(s.timeMs)} + ${formatClock(s.overtimeMs)}`;
}

function seekDetail(s) {
  const mode = s.void ? "Void" : "Single";
  if (s.mine && s.to) return `Reto a ${s.to} · ${classicTime(s)} · ${mode}`;
  return `Clásica · ${classicTime(s)} · ${mode}`;
}

function seekRow(c, s, dup) {
  return playerRow(s.host, s.country, dup ? duplicateDetail(s) : seekDetail(s), s.mine, ...seekButtons(c, s, dup));
}

function seekButtons(c, s, dup) {
  if (!s.mine) return [h("button.btn.small", { disabled: dup && c.room?.id === s.id, onclick: () => c.accept(s) }, dup && c.room?.id === s.id ? "Dentro" : "Jugar")];
  return [
    dup ? h("button.btn.small", { disabled: s.players < 2, onclick: () => c.startSeek(s) }, "Empezar") : null,
    h("button.text-btn", { onclick: () => c.roomAction("cancel", s.id) }, "Cancelar"),
  ];
}

function noDuplicates(c) {
  const idle = others(c).filter((p) => !p.playing).length;
  if (idle <= 0) return emptyState("D", "No hay duplicadas publicadas.");
  return emptyState("D", idle === 1 ? "Si creas una duplicada, la recibirá " : "Si creas una duplicada, la recibirán ", h("b", {}, idle === 1 ? "1 jugador" : `${idle} jugadores`), " en línea.");
}


function adjournedRow(c, a) {
  const detail = !a.online ? "Desconectado · aplazada" : a.busy ? "Jugando otra partida · aplazada" : "En línea · aplazada";
  const available = (a.online || a.untimed) && !a.busy;
  return playerRow(a.opponent, a.country, detail, false, h("button.btn.small", { disabled: !available, onclick: () => c.resumeAdjourned(a.id) }, "Retomar"));
}

const VERDICTS = {
  win: ["¡Ganaste!", "se adjudicó a tu favor."],
  loss: ["Perdiste", "se adjudicó en tu contra."],
  void: ["Partida anulada", "se anuló al pasar los 3 días."],
};
let verdictShown = null;

function verdictNotice(c) {
  const a = c.lobby.adjourned.find((x) => x.result);
  if (!a || verdictShown === a.id) return;
  verdictShown = a.id;
  const [title, text] = VERDICTS[a.result] ?? VERDICTS.void;
  openDialog((close) => [
    h("div.resume-icon", {}, photoAvatar(a.opponent, a.country, 56)),
    h("h2.resume-title", {}, title),
    h("p", {}, `Tu partida aplazada con ${a.opponent} ${flag(a.country)} ${text}`),
    actions(h("button.text-btn", { onclick: close }, "Entendido")),
  ], { onClose: () => c.seen(a.id) });
}

function games(c, dup) {
  const items = c.lobby.games.filter((g) => (g.mode === "duplicate") === dup);
  return { title: "Partidas en curso", items, row: (g) => (dup ? duplicateGame(c, g) : classicGame(c, g)), empty: null };
}

function classicGame(c, g) {
  if (g.players?.length !== 2) return h("button.compact-game", { onclick: () => c.watch(g.id) }, h("span.grow.ellipsis", {}, (g.players ?? []).join("  vs  ")));
  const side = (i, end) => h("span", { class: "game-side-cell" + (end ? " end" : "") }, nameFlag(g.players[i], g.countries?.[i]));
  return h("button.compact-game", { onclick: () => c.watch(g.id) },
    side(0, false),
    h("b.game-score-cell", {}, g.scores?.length === 2 ? `${g.scores[0]} – ${g.scores[1]}` : "vs"),
    side(1, true),
  );
}

function duplicateGame(c, g) {
  const round = g.leader ? g.round : 0;
  const i = g.players.indexOf(g.leader);
  const summary = !round ? [h("b", {}, "Ronda 1"), `  ·  ${g.players.length} jugadores`]
    : g.tied > 1 ? [h("b", {}, `Ronda ${round}`), "  ·  ", h("b.accent", {}, `${g.tied} empatados`)]
    : [h("b", {}, `Ronda ${round}`), "  ·  gana ", h("b.accent.ellipsis.leader-name", {}, g.leader), ` ${flag(g.countries?.[i] ?? "")}`];
  return h("button.compact-game", { onclick: () => c.watch(g.id) },
    h("span.grow.dup-summary", {}, summary),
    h("b.dup-efficiency", {}, round ? `${g.efficiency} %` : "–"),
  );
}

function compactPerson(p) {
  return h("button.compact-person", { onclick: () => profileDialog(p) }, nameFlag(p.name, p.country));
}

function challengeable(c) {
  const items = others(c).filter((p) => !p.playing);
  return { title: "Retar a", items, row: (p) => personRow(c, p), compact: compactPerson };
}

function peopleList(c, people, close) {
  if (!people.length) return h("div.empty-state", {}, h("span.glyph-big", {}, "👥"), h("span.empty-text", {}, "No hay nadie en línea."));
  return people.map((p) => personRow(c, p, close));
}

function personRow(c, p, close = () => {}) {
  const game = c.lobby.games.find((g) => g.players?.includes(p.name));
  const action = p.playing
    ? h("button.btn.small.outlined", { disabled: !game, onclick: () => (close(), c.watch(game.id)) }, "Ver")
    : challengeButton(c, p, close);
  return playerRow(p.name, p.country, null, false, chatButton(p), action);
}

function challengeButton(c, p, close) {
  const game = adjournedWith(c, p);
  if (game) return h("button.btn.small", { disabled: !!game.busy, onclick: () => (close(), resumeWith(c, game)) }, "Retomar");
  return h("button.btn.small", { onclick: () => (close(), challenge(p)) }, "Retar");
}

export function adjournedWith(c, p) {
  return c.lobby.adjourned.find((a) => !a.duplicate && !a.result && a.opponent === p.name && a.country === p.country) ?? null;
}

export function resumeWith(c, game) {
  if (blockedByGame()) return;
  save("tab.classic", "online");
  c.resumeAdjourned(game.id);
  go("clasica");
}

export function challenge(p) {
  if (!blockedByGame()) go("buscar/" + encodeURIComponent(p.name) + "/" + p.country);
}
