import { h, replace } from "../dom.js";
import { icon } from "../icons.js";
import { go } from "../router.js";
import { screenHead, nameFlag, initialAvatar, formatClock } from "../parts.js";
import { confirm } from "../dialogs.js";
import { account, logOut } from "./api.js";
import { online } from "./client.js";
import { tellDialog } from "./tell.js";

export function lobbyScreen(app) {
  if (!account()) return signInFirst(app);
  const body = h("div.lobby");
  const status = h("p.muted.lobby-status");
  app.append(h("div.screen.wide", {}, screenHead("En línea", "", accountChip()), status, body,
    h("button.btn.block.lobby-seek", { onclick: () => go("buscar") }, "Buscar clásica")));
  online.acquire();
  const stop = online.subscribe((c) => draw(c, body, status));
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
  return h("button.account-chip", { onclick: () => askLogOut() }, icon("person", 20), nameFlag(s.name, s.country));
}

async function askLogOut() {
  if (!(await confirm("¿Seguro que quieres cerrar sesión?", "Cerrar sesión"))) return;
  online.shutDown();
  logOut();
  go("", true);
}

function draw(c, body, status) {
  const live = c.room && !c.ended && !c.finished() && (c.session || c.room.watching);
  if (live && c.parked !== c.room.id) return go("sala");
  status.textContent = c.refused ?? c.status ?? c.ended ?? "";
  replace(body,
    c.refused ? h("button.btn", { onclick: () => (logOut(), go("cuenta")) }, "Entrar de nuevo") : null,
    live ? h("button.btn.tonal.back-to-game", { onclick: () => ((c.parked = null), go("sala")) }, "Volver a la partida") : null,
    h("section.lobby-col", {}, invites(c), seeks(c), adjourned(c)),
    h("section.lobby-col", {}, games(c), people(c)),
  );
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
    initialAvatar(name, 40),
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

function seeks(c) {
  const list = c.lobby.seeks.filter((s) => s.mode !== "duplicate").sort((a, b) => !!b.mine - !!a.mine);
  const rows = list.map((s) => playerRow(s.host, s.country, seekDetail(s),
    s.mine ? h("button.text-btn", { onclick: () => c.roomAction("cancel", s.id) }, "Cancelar") : h("button.btn.small", { onclick: () => c.accept(s) }, "Jugar"),
  ));
  return section("Ofertas de partidas", rows, emptyState("globe", "No hay clásicas publicadas."));
}

function invites(c) {
  const rows = c.lobby.invites.filter((s) => s.mode !== "duplicate").map((s) => playerRow(s.host, s.country, `Te reta · ${classicTime(s)} · ${s.void ? "Void" : "Single"}`,
    h("button.text-btn", { onclick: () => c.roomAction("decline", s.id) }, "Rechazar"),
    h("button.btn.small", { onclick: () => c.accept(s) }, "Aceptar"),
  ));
  return section("Retos", rows);
}

function adjourned(c) {
  const rows = c.lobby.adjourned.filter((a) => !a.duplicate && !a.result).map((a) => playerRow(a.opponent, a.country, "Partida aplazada",
    h("button.btn.small", { disabled: !(a.online || a.untimed) || a.busy, onclick: () => c.resumeAdjourned(a.id) }, "Retomar"),
  ));
  return section("Aplazadas", rows);
}

function games(c) {
  const rows = c.lobby.games.filter((g) => g.mode !== "duplicate").map((g) => h("div.player-row.game-row", {},
    h("div.grow", {}, h("div.row", {}, nameFlag(g.players[0], g.countries?.[0]), h("b", {}, g.scores?.length === 2 ? `${g.scores[0]} – ${g.scores[1]}` : "–"), nameFlag(g.players[1], g.countries?.[1])),
      h("div.muted.small", {}, `${g.bag} en la bolsa${g.watchers ? ` · ${g.watchers} mirando` : ""}`)),
    h("button.btn.small.outlined", { onclick: () => c.watch(g.id) }, "Ver"),
  ));
  return section("Partidas en curso", rows);
}

function people(c) {
  const me = account();
  const others = c.lobby.people.filter((p) => !(p.name === me.name && p.country === me.country));
  const rows = others.map((p) => playerRow(p.name, p.country, p.playing ? "Jugando" : "",
    h("button.icon-btn", { "aria-label": `Escribir a ${p.name}`, onclick: () => tellDialog(p) }, icon("chat", 22)),
    p.playing ? null : h("button.btn.small", { onclick: () => go("buscar/" + encodeURIComponent(p.name) + "/" + p.country) }, "Retar"),
  ));
  return section("En línea", rows, emptyState("person", "No hay nadie más en línea."));
}
