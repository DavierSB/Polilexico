import { h, replace } from "../dom.js";
import { go } from "../router.js";
import { screenHead, engineLoading } from "../parts.js";
import { call, whenReady } from "../engine.js";
import { createBoard } from "../board.js";
import { openDialog, actions, themeDialog } from "../dialogs.js";
import { parseBoard, tokens } from "../tiles.js";
import { at } from "../placement.js";
import { face } from "../classic/rack.js";
import { getRecord } from "../records.js";
import { lookUp } from "../study/lookup.js";
import { alias } from "./list.js";
import { deepView, DEEP_TITLES, BUDGET_MS } from "../study/deep.js";
import { spreadOf, spreadDialog } from "./spread.js";
import { exportDialog } from "./export.js";
import { icon, bagIcon } from "../icons.js";
import { showBag } from "../classic/moves.js";
import { movesTable, reviewMovesDialog, turnBag, moveOf } from "./reviewParts.js";

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
  ui.record = record;
  ui.game = await call("reviewOpen", record.text);
  loading.stop();
  ui.classic = ui.game.Mode !== "duplicate";
  ui.opponent = ui.game.Mode === "duplicate" ? "Máster" : alias(ui.game.Opponent);
  ui.me = ui.game.Local ? ui.game.Me : "Tú";
  ui.index = Math.max(0, (ui.game.StartTurn ?? 0));
  ui.board = createBoard({ onSquare: (i) => lookUp(turn(ui).cells, i) });
  ui.top = h("div.stack.review-top");
  ui.bottom = h("div.stack.review-bottom");
  ui.head = h("div.review-head");
  const el = h("div.review", {},
    ui.head,
    h("div.review-board", {}, ui.top, ui.board.el),
    h("div.review-side", {}, ui.bottom),
  );
  holder.replaceWith(el);
  draw(ui);
  if (ui.classic && ui.game.Turns.length) openingDialog(ui);
  return el;
}

function reviewSpread(ui) {
  const g = ui.game;
  const totals = [...g.Turns.map((t) => [t.MyScore, t.OpponentScore]), [g.MyScore, g.OpponentScore]];
  return spreadOf(ui.me, ui.opponent, g.HumanStarts, totals);
}

function chartDialog(ui, extra = null) {
  spreadDialog(reviewSpread(ui), { selected: ui.index, pick: (i) => goTo(ui, i), extra });
}

function openingDialog(ui) {
  const endgame = ui.game.Turns.findIndex((t) => t.Endgame);
  chartDialog(ui, (close) => h("div.stack.opening-choices", {},
    endgame >= 0 ? h("button.btn.block", { onclick: () => (close(), goTo(ui, ui.game.StartTurn ?? 0)) }, "Analizar partida") : null,
    endgame >= 0 ? h("button.btn.block", { onclick: () => (close(), goTo(ui, endgame)) }, "Analizar final") : null,
    ui.record.gcg ? h("button.btn.block.outlined", { onclick: () => (close(), exportDialog(null, ui.record)) }, "Exportar") : null,
  ));
}

function goTo(ui, i) {
  ui.index = Math.min(Math.max(i, 0), ui.game.Turns.length - 1);
  ui.selected = null;
  draw(ui);
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
  replace(ui.head, screenHead(t.Endgame ? "Análisis del final" : "Análisis", "partidas", headActions(ui, t)));
  replace(ui.top, h("b", {}, `Turno ${t.Number}/${ui.game.Turns.length}${t.Player ? " · " + alias(t.Player) : ""}`), bar(ui.opponent, t.OpponentScore, !mine(ui, t)));
  replace(ui.bottom,
    bar(ui.me, t.MyScore, mine(ui, t)),
    h("div.tile-line.review-rack", {}, tokens(t.Rack).map((l) => h("div.rack-tile.line-tile", {}, face(l)))),
    h("div.row.turn-nav", {},
      h("button.btn.small.outlined", { disabled: ui.index === 0, onclick: () => move(ui, -1) }, "‹"),
      h("button.text-btn.grow", { onclick: () => listDialog(ui) }, "Ver lista de jugadas"),
      h("button.btn.small.outlined", { disabled: ui.index >= ui.game.Turns.length - 1, onclick: () => move(ui, 1) }, "›"),
    ),
    movesTable(ui, t, (m) => select(ui, m)),
    deepSection(ui, t),
  );
}

function deepSection(ui, t) {
  if (!ui.classic || (!t.Endgame && !t.PreEndgame)) return null;
  const phase = t.Endgame ? "endgame" : "preendgame";
  const known = ui.deep?.get(ui.index);
  if (known) return h("div.stack", {}, h("b", {}, DEEP_TITLES[phase]), deepView(known, t.cells, (m) => select(ui, { text: m.label, placement: m.placement })));
  const failed = ui.deepFailed?.has(ui.index);
  if (!failed) analyzeDeep(ui, ui.index);
  return h("div.row.deep-status", {}, failed ? null : h("div.spinner.small"), h("span.muted", {}, failed ? "No se pudo analizar" : t.Endgame ? "Analizando el final…" : "Analizando el pre-final…"));
}

async function analyzeDeep(ui, index) {
  ui.deepRunning ??= new Set();
  if (ui.deepRunning.has(index)) return;
  ui.deepRunning.add(index);
  const view = await call("reviewDeep", ui.text, index, BUDGET_MS).catch(() => null);
  ui.deepRunning.delete(index);
  if (view) (ui.deep ??= new Map()).set(index, view);
  else (ui.deepFailed ??= new Set()).add(index);
  if (ui.index === index) draw(ui);
}

function mine(ui, t) {
  return t.Player === "Tú" || t.Player === ui.game.Me;
}

function bar(name, score, active) {
  return h("div", { class: "player-card review-bar" + (active ? " active" : "") }, h("span.player-name.grow", {}, name), h("span.score", {}, score));
}

function headActions(ui, t) {
  const bag = turnBag(t, ui.classic);
  return h("div.row.review-actions", {},
    h("button.text-btn", { onclick: () => reviewMovesDialog(ui, (i) => goTo(ui, i)) }, "Movidas"),
    h("button.icon-btn", { "aria-label": "Tema", onclick: themeDialog }, icon("palette")),
    ui.classic ? h("button.icon-btn", { "aria-label": "Ventaja", onclick: () => chartDialog(ui) }, icon("chart")) : null,
    h("button.bag-btn", { "aria-label": "Bolsa", onclick: () => showBag(bag.count, bag.rival, bag.tiles) }, bagIcon(bag.count)),
  );
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
