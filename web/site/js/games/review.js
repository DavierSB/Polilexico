import { h, replace } from "../dom.js";
import { go } from "../router.js";
import { screenHead, engineLoading } from "../parts.js";
import { call, whenReady } from "../engine.js";
import { createBoard } from "../board.js";
import { openDialog, actions } from "../dialogs.js";
import { parseBoard, tokens } from "../tiles.js";
import { parsePlacement, spelled, at } from "../placement.js";
import { face } from "../classic/rack.js";
import { getRecord } from "../records.js";
import { lookUp } from "../study/lookup.js";
import { alias, gameTitle, scoreText } from "./list.js";

const WHO = { "Tú": "Tu jugada", "Máster": "Jugada del Máster" };

export function reviewScreen(app, [id]) {
  const holder = h("div.screen");
  app.append(holder);
  const ui = { index: 0, selected: null, ranked: new Map() };
  const onKey = (e) => keys(ui, e);
  open(holder, id, ui).then((el) => el && document.addEventListener("keydown", onKey));
  return () => document.removeEventListener("keydown", onKey);
}

async function open(holder, id, ui) {
  const loading = engineLoading();
  replace(holder, loading.card);
  const record = await getRecord(id).catch(() => null);
  if (!record) return go("partidas", true);
  await whenReady();
  ui.text = record.text;
  ui.game = await call("reviewOpen", record.text);
  loading.stop();
  ui.classic = ui.game.Mode !== "duplicate";
  ui.opponent = ui.game.Mode === "duplicate" ? "Máster" : alias(ui.game.Opponent);
  ui.me = ui.game.Local ? ui.game.Me : "Tú";
  ui.index = Math.max(0, (ui.game.StartTurn ?? 0));
  ui.board = createBoard({ onSquare: (i) => lookUp(turn(ui).cells, i) });
  ui.top = h("div.stack");
  ui.bottom = h("div.stack");
  const el = h("div.review", {},
    h("div.review-head", {}, screenHead(`${gameTitle(ui.game)} · ${scoreText(ui.game)}`, "partidas")),
    h("div.review-board", {}, ui.top, ui.board.el),
    h("div.review-side", {}, ui.bottom),
  );
  holder.replaceWith(el);
  draw(ui);
  return el;
}

function turn(ui) {
  const t = ui.ranked.get(ui.index) ?? ui.game.Turns[ui.index];
  t.cells ??= parseBoard(t.Board);
  return t;
}

function draw(ui) {
  const t = turn(ui);
  const p = ui.selected?.placement;
  const pending = new Map();
  if (p) p.tiles.forEach((tile, i) => tile && !t.cells[at(p, i)] && pending.set(at(p, i), tile));
  ui.board.render({ cells: t.cells, pending });
  replace(ui.top, h("b", {}, `Turno ${t.Number}/${ui.game.Turns.length}${t.Player ? " · " + alias(t.Player) : ""}`), bar(ui.opponent, t.OpponentScore, !mine(ui, t)));
  replace(ui.bottom,
    bar(ui.me, t.MyScore, mine(ui, t)),
    h("div.tile-line.review-rack", {}, tokens(t.Rack).map((l) => h("div.rack-tile.line-tile", {}, face(l)))),
    h("div.row.turn-nav", {},
      h("button.btn.small.outlined", { disabled: ui.index === 0, onclick: () => move(ui, -1) }, "‹"),
      h("button.text-btn.grow", { onclick: () => listDialog(ui) }, "Ver lista de jugadas"),
      h("button.btn.small.outlined", { disabled: ui.index >= ui.game.Turns.length - 1, onclick: () => move(ui, 1) }, "›"),
    ),
    lines(ui, t),
  );
}

function mine(ui, t) {
  return t.Player === "Tú" || t.Player === ui.game.Me;
}

function bar(name, score, active) {
  return h("div", { class: "player-card review-bar" + (active ? " active" : "") }, h("span.player-name.grow", {}, name), h("span.score", {}, score));
}

function moveOf(t, m, equity) {
  const raw = m.Description.replace(/^\(Inválida (\S+ \S+):.*\)$/, "$1");
  const placement = parsePlacement(raw);
  return { text: placement ? spelled(placement, t.cells) : m.Description, score: m.Score, equity: m.HasEquity ? m.Equity : equity, placement };
}

function lines(ui, t) {
  const best = t.Candidates?.[0];
  const rows = [];
  if (best && ui.classic) rows.push(["Mejor jugada", "#1", moveOf(t, best)]);
  (t.Marks ?? []).forEach((m) => rows.push([WHO[m.Who] ?? `Jugada de ${alias(m.Who)}`, m.Rank >= 0 ? `#${m.Rank + 1}` : "", moveOf(t, m)]));
  return h("div.stack.review-lines", {}, rows.map(([label, rank, m]) => h("button", { class: "review-line" + (ui.selected?.text === m.text ? " active" : ""), onclick: () => select(ui, m) },
    h("div.grow.stack", {}, h("span.muted.small", {}, label), h("b", {}, m.text)),
    rank ? h("span.muted", {}, rank) : null,
    h("b.review-points", {}, m.score),
  )));
}

function select(ui, m) {
  ui.selected = ui.selected?.text === m.text ? null : m;
  draw(ui);
}

function move(ui, step) {
  const next = ui.index + step;
  if (next < 0 || next >= ui.game.Turns.length) return;
  ui.index = next;
  ui.selected = null;
  draw(ui);
}

async function listDialog(ui) {
  const t = turn(ui);
  const body = h("div.moves-scroll");
  const fill = (tt) => replace(body, candidatesTable(ui, tt, (m) => (close(), select(ui, m))));
  fill(t);
  const close = openDialog((done) => [
    h("h2", {}, "Jugadas"),
    body,
    actions(ui.ranked.has(ui.index) ? null : h("button.text-btn", { onclick: (e) => rank(ui, e.target, fill) }, "Todas las jugadas"), h("button.text-btn", { onclick: done }, "Cerrar")),
  ], { wide: true });
}

async function rank(ui, button, fill) {
  button.disabled = true;
  button.textContent = "Calculando…";
  const ranked = await call("reviewRank", ui.text, ui.index).catch(() => null);
  if (!ranked) return (button.textContent = "No se pudo");
  ui.ranked.set(ui.index, ranked);
  button.remove();
  fill(turn(ui));
}

function candidatesTable(ui, t, choose) {
  const list = (t.Candidates ?? []).map((m) => moveOf(t, m));
  if (!list.length) return h("p.muted", {}, "Sin jugadas.");
  return h("table.moves.ranked", {},
    h("thead", {}, h("tr", {}, h("th", {}, "#"), h("th", {}, "Jugada"), h("th.num", {}, "Puntos"), ui.classic ? h("th.num", {}, "Equity") : null)),
    h("tbody", {}, list.map((m, n) => h("tr", { onclick: () => choose(m) }, h("td.muted", {}, n + 1), h("td", {}, m.text), h("td.num", {}, m.score), ui.classic ? h("td.num", {}, m.equity == null ? "" : m.equity.toFixed(1)) : null))),
  );
}

function keys(ui, e) {
  if (document.querySelector(".scrim") || !ui.game) return;
  if (e.key === "ArrowLeft") move(ui, -1);
  if (e.key === "ArrowRight") move(ui, 1);
}
