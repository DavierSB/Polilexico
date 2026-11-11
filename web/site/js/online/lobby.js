import { h, replace } from "../dom.js";
import { icon } from "../icons.js";
import { go } from "../router.js";
import { screenHead, nameFlag, formatClock } from "../parts.js";
import { account, logOut } from "./api.js";
import { online } from "./client.js";
import { tellDialog } from "./tell.js";
import { photoAvatar, profileDialog } from "./profile.js";
import { load, save } from "../store.js";
import { choiceRow } from "../classic/options.js";

export function lobbyScreen(app) {
  if (!account()) return signInFirst(app);
  const ui = { body: h("div.lobby"), status: h("p.muted.lobby-status"), tabs: h("div"), publish: h("div"), duplicate: load("lobbyTab", "classic") === "duplicate" };
  app.append(h("div.screen.wide", {}, screenHead("En línea", "", accountChip()), ui.tabs, ui.status, ui.body, ui.publish));
  online.acquire();
  const stop = online.subscribe((c) => draw(c, ui));
  return () => (stop(), online.release());
}

function signInFirst(app) {
  app.append(h("div.screen", {}, screenHead("En línea"),
    h("div.card.empty-card", {}, icon("globe", 40), h("p", {}, "Juega contra personas de todo el mundo."),
      h("button.btn", { onclick: () => go("cuenta") }, "Entrar o crear cuenta")),
  ));
}

function accountChip() {
  const s = account();
  return h("button.account-chip", { onclick: () => go("perfil") }, icon("person", 20), nameFlag(s.name, s.country));
}


function draw(c, ui) {
  const room = roomRoute(c);
  const live = room && !c.ended && !c.finished();
  if (live && c.parked !== c.room.id) return go(room);
  const dup = ui.duplicate;
  ui.status.textContent = c.refused ?? c.status ?? c.ended ?? "";
  replace(ui.tabs, choiceRow(["Clásica", "Duplicada"], dup ? 1 : 0, (i) => ((ui.duplicate = i === 1), save("lobbyTab", i ? "duplicate" : "classic"), draw(c, ui))));
  replace(ui.body,
    c.refused ? h("button.btn", { onclick: () => (logOut(), go("cuenta")) }, "Entrar de nuevo") : null,
    live ? h("button.btn.tonal.back-to-game", { onclick: () => ((c.parked = null), go(room)) }, "Volver a la partida") : null,
    offerCard(c),
    waitingRoom(c),
    h("section.lobby-col", {}, dup ? null : invites(c), seeks(c, dup), adjourned(c, dup)),
    h("section.lobby-col", {}, games(c, dup), people(c, dup)),
  );
  replace(ui.publish, dup
    ? h("button.btn.block.lobby-seek", { onclick: () => go("crear-duplicada") }, "Crear duplicada")
    : h("button.btn.block.lobby-seek", { onclick: () => go("buscar") }, "Buscar clásica"));
}

function roomRoute(c) {
  if (!c.room) return null;
  if (c.room.duplicate) return c.table ? "sala-duplicada" : null;
  return c.session || c.room.watching ? "sala" : null;
}

function offerCard(c) {
  const seek = c.offer && c.lobby.seeks.find((s) => s.id === c.offer);
  if (!seek || c.room) return null;
  return h("div.card.offer-card", {},
    h("b", {}, `${seek.host} creó una duplicada`), h("span.muted.small", {}, duplicateDetail(seek)),
    h("div.row", {}, h("button.text-btn", { onclick: () => c.dismissOffer() }, "Ahora no"), h("button.btn.small", { onclick: () => (c.dismissOffer(), c.accept(seek)) }, "Unirse")),
  );
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

function duplicateDetail(s) {
  if (s.resume) return "Partida aplazada · duplicada";
  return `👥 ${s.players} en la sala · ${formatClock(s.turnMs)} por ronda`;
}

function section(title, rows, empty = null) {
  if (!rows.length && !empty) return null;
  return h("div.lobby-section", {}, h("h3.side-title", {}, title), rows.length ? h("div.stack", {}, rows) : empty);
}

function emptyState(iconName, text) {
  return h("div.empty-state", {}, icon(iconName, 32), h("span", {}, text));
}

function playerRow(name, country, detail, ...buttons) {
  return h("div.player-row", {},
    h("button.avatar-btn", { "aria-label": `Perfil de ${name}`, onclick: () => profileDialog({ name, country, playing: false }) }, photoAvatar(name, country, 40)),
    h("div.grow", {}, h("div", {}, nameFlag(name, country)), detail ? h("div.muted.small.ellipsis", {}, detail) : null),
    h("div.row", {}, buttons),
  );
}

function classicTime(s) {
  return s.untimed ? "Sin tiempo" : `${formatClock(s.timeMs)} + ${formatClock(s.overtimeMs)}`;
}

function seekDetail(s) {
  const mode = s.void ? "Void" : "Single";
  if (s.mine && s.to) return `Reto a ${s.to} · ${classicTime(s)} · ${mode}`;
  return `Clásica · ${classicTime(s)} · ${mode}`;
}

function seeks(c, dup) {
  const list = c.lobby.seeks.filter((s) => (s.mode === "duplicate") === dup).sort((a, b) => !!b.mine - !!a.mine);
  const rows = list.map((s) => playerRow(s.host, s.country, dup ? duplicateDetail(s) : seekDetail(s), ...seekButtons(c, s, dup)));
  return section("Ofertas de partidas", rows, emptyState("globe", dup ? noDuplicates(c) : "No hay clásicas publicadas."));
}

function seekButtons(c, s, dup) {
  if (!s.mine) return [h("button.btn.small", { disabled: dup && c.room?.id === s.id, onclick: () => c.accept(s) }, dup && c.room?.id === s.id ? "Dentro" : "Jugar")];
  return [
    dup ? h("button.btn.small", { disabled: s.players < 2, onclick: () => c.startSeek(s) }, "Empezar") : null,
    h("button.text-btn", { onclick: () => c.roomAction("cancel", s.id) }, "Cancelar"),
  ];
}

function noDuplicates(c) {
  const idle = c.lobby.people.filter((p) => !p.playing).length - 1;
  if (idle <= 0) return "No hay duplicadas publicadas.";
  return `Si creas una duplicada, la recibirá${idle === 1 ? " 1 jugador" : `n ${idle} jugadores`} en línea.`;
}

function invites(c) {
  const rows = c.lobby.invites.filter((s) => s.mode !== "duplicate").map((s) => playerRow(s.host, s.country, `Te reta · ${classicTime(s)} · ${s.void ? "Void" : "Single"}`,
    h("button.text-btn", { onclick: () => c.roomAction("decline", s.id) }, "Rechazar"),
    h("button.btn.small", { onclick: () => c.accept(s) }, "Aceptar"),
  ));
  return section("Retos", rows);
}

function adjourned(c, dup) {
  const rows = c.lobby.adjourned.filter((a) => !!a.duplicate === dup && !a.result).map((a) => playerRow(a.opponent, a.country, "Partida aplazada",
    h("button.btn.small", { disabled: !(a.online || a.untimed) || a.busy, onclick: () => c.resumeAdjourned(a.id) }, "Retomar"),
  ));
  return section("Aplazadas", rows);
}

function games(c, dup) {
  if (dup) return section("Partidas en curso", c.lobby.games.filter((g) => g.mode === "duplicate").map((g) => duplicateGame(c, g)));
  const rows = c.lobby.games.filter((g) => g.mode !== "duplicate").map((g) => h("div.player-row.game-row", {},
    h("div.grow", {}, h("div.row", {}, nameFlag(g.players[0], g.countries?.[0]), h("b", {}, g.scores?.length === 2 ? `${g.scores[0]} – ${g.scores[1]}` : "–"), nameFlag(g.players[1], g.countries?.[1])),
      h("div.muted.small", {}, `${g.bag} en la bolsa${g.watchers ? ` · ${g.watchers} mirando` : ""}`)),
    h("button.btn.small.outlined", { onclick: () => c.watch(g.id) }, "Ver"),
  ));
  return section("Partidas en curso", rows);
}

function duplicateGame(c, g) {
  const detail = !g.round ? `Duplicada · ${g.players.length} jugadores` : g.efficiency ? `Ronda ${g.round} · líder ${g.efficiency} %` : `Ronda ${g.round}`;
  return h("div.player-row.game-row", {},
    h("div.grow", {}, h("b.ellipsis", {}, g.players.join(", ")), h("div.muted.small", {}, detail + (g.watchers ? ` · ${g.watchers} mirando` : ""))),
    h("button.btn.small.outlined", { onclick: () => c.watch(g.id) }, "Ver"),
  );
}

function people(c, dup) {
  const me = account();
  const others = c.lobby.people.filter((p) => !(p.name === me.name && p.country === me.country));
  const rows = others.map((p) => playerRow(p.name, p.country, p.playing ? "Jugando" : "",
    h("button.icon-btn", { "aria-label": `Escribir a ${p.name}`, onclick: () => tellDialog(p) }, icon("chat", 22)),
    p.playing || dup ? null : h("button.btn.small", { onclick: () => go("buscar/" + encodeURIComponent(p.name) + "/" + p.country) }, "Retar"),
  ));
  return section("En línea", rows, emptyState("person", "No hay nadie más en línea."));
}
