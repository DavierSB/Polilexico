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
import { alias, folderOf } from "./list.js";
const BUDGET_MS = 30000;
import { endgamePage } from "./endgamePage.js";
import { fitBoard } from "./boardFit.js";
import { spreadOf, spreadDialog } from "./spread.js";
import { efficiencyOf, efficiencyDialog } from "./efficiency.js";
import { exportDialog } from "./export.js";
import { icon, bagIcon } from "../icons.js";
import { showBag } from "../classic/moves.js";
import { movesTable, reviewMovesDialog, turnBag, moveOf, pageOf, candidatesTable, turnTitle, scoreBar } from "./reviewParts.js";

let pendingPosition = null;

export function reviewPosition(position) {
  pendingPosition = position;
  go("analisis-posicion");
}

export function reviewScreen(app, [id, at]) {
  return reviewOf(app, (holder, ui) => openRecord(holder, id, ui, at === "final"));
}

export function positionReviewScreen(app) {
  const position = pendingPosition;
  if (!position) return go("analizador", true);
  return reviewOf(app, (holder, ui) => openPosition(holder, position, ui));
}

function reviewOf(app, open) {
  const holder = h("div.screen");
  app.append(holder);
  const ui = { index: 0, selected: null, ranked: new Map(), ranking: new Set(), pre: new Map(), preRunning: new Set(), back: "" };
  const onKey = (e) => keys(ui, e);
  open(holder, ui).then((el) => el && document.addEventListener("keydown", onKey));
  return () => (document.removeEventListener("keydown", onKey), closeExplorer(ui), ui.stopFit?.());
}

async function openRecord(holder, id, ui, atEndgame) {
  const loading = engineLoading();
  replace(holder, loading.card);
  const record = await getRecord(id).catch(() => null);
  if (!record) return go("", true);
  ui.back = "terminadas/" + folderOf(record);
  await whenReady();
  ui.text = record.text;
  ui.record = record;
  ui.game = await call("reviewOpen", record.text);
  loading.stop();
  ui.classic = ui.game.Mode !== "duplicate";
  ui.opponent = ui.game.Mode === "duplicate" ? "Máster" : alias(ui.game.Opponent);
  ui.me = ui.game.Local ? ui.game.Me : "Tú";
  const endgame = ui.game.Turns.findIndex((t) => t.Endgame);
  ui.index = Math.max(0, atEndgame && endgame >= 0 ? endgame : (ui.game.StartTurn ?? 0));
  ui.explore = { open: "exploreReview", args: (i) => [ui.text, i] };
  ui.deep = (i) => call("reviewDeep", ui.text, i, BUDGET_MS);
  const el = mount(holder, ui);
  if (ui.game.Turns.length && !atEndgame) openingDialog(ui);
  return el;
}

async function openPosition(holder, p, ui) {
  const loading = engineLoading();
  replace(holder, loading.card);
  await whenReady();
  const args = [p.board, p.rack, p.mine, p.theirs];
  const turn = await call("positionReview", ...args).catch(() => null);
  loading.stop();
  if (!turn) return go("analizador", true);
  ui.game = { Mode: "classic", Turns: [turn], Opponent: "Rival", HumanStarts: true };
  ui.classic = true;
  ui.position = true;
  ui.opponent = "Rival";
  ui.me = "Tú";
  ui.back = "analizador";
  ui.explore = { open: "explorePosition", args: () => args };
  ui.deep = () => call("positionDeep", ...args, BUDGET_MS);
  return mount(holder, ui);
}

function mount(holder, ui) {
  ui.board = createBoard({ onSquare: (i) => ui.onSquare?.(i) });
  ui.goTo = (i) => goTo(ui, i);
  ui.moveOf = moveOf;
  ui.nameOf = (name) => (name === "Tú" ? ui.me : alias(name));
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
  ui.stopFit = fitBoard(el, ui.board.el, [ui.head, ui.top], [ui.bottom]);
  return el;
}

function reviewSpread(ui) {
  const g = ui.game;
  const totals = [...g.Turns.map((t) => [t.MyScore, t.OpponentScore]), [g.MyScore, g.OpponentScore]];
  return spreadOf(ui.me, ui.opponent, g.HumanStarts, totals);
}

function reviewEfficiency(ui) {
  const points = (mine) => ui.game.Turns.map((t) => t.Marks?.find((m) => (m.Who === "Tú") === mine)?.Score ?? 0);
  return efficiencyOf(points(true), points(false));
}

function chartDialog(ui, extra = null) {
  const options = { selected: ui.index, pick: (i) => goTo(ui, i), extra };
  if (ui.classic) spreadDialog(reviewSpread(ui), options);
  else efficiencyDialog(reviewEfficiency(ui), options);
}

function openingDialog(ui) {
  const endgame = ui.classic ? ui.game.Turns.findIndex((t) => t.Endgame) : -1;
  chartDialog(ui, (close) => h("div.stack.opening-choices", {},
    endgame >= 0 ? h("button.btn.block", { onclick: () => (close(), goTo(ui, ui.game.StartTurn ?? 0)) }, "Analizar partida") : null,
    endgame >= 0 ? h("button.btn.block", { onclick: () => (close(), goTo(ui, endgame)) }, "Analizar final") : null,
    h("button.btn.block.outlined", { onclick: () => (close(), exportDialog(null, ui.record)) }, "Exportar"),
  ));
}

function goTo(ui, i) {
  ui.index = Math.min(Math.max(i, 0), ui.game.Turns.length - 1);
  ui.selected = null;
  draw(ui);
}

function turn(ui) {
  const t = ui.game.Turns[ui.index];
  t.cells ??= parseBoard(t.Board);
  return t;
}

function draw(ui) {
  const t = turn(ui);
  replace(ui.head, screenHead(titleOf(ui, t), ui.back, headActions(ui, t)));
  if (t.Endgame) return drawEndgame(ui, t);
  closeExplorer(ui);
  const page = pageFor(ui, t);
  ui.page = page;
  ui.onSquare = (i) => lookUp(t.cells, i);
  const p = ui.selected?.placement;
  const pending = new Map();
  if (p) p.tiles.forEach((tile, i) => tile && !t.cells[at(p, i)] && pending.set(at(p, i), tile));
  ui.board.render({ cells: t.cells, pending });
  replace(ui.top, turnTitle(t, ui.game.Turns.length), scoreBar(ui.opponent, t.OpponentScore, null, t.Player && !mine(ui, t)));
  replace(ui.bottom,
    scoreBar(ui.me, t.MyScore, null, mine(ui, t)),
    h("div.tile-line.review-rack", {}, tokens(t.Rack).map((l) => h("div.rack-tile.line-tile", {}, face(l)))),
    h("div.row.turn-nav", {},
      h("button.btn.small.outlined", { disabled: ui.index === 0, onclick: () => move(ui, -1) }, "‹"),
      h("button.text-btn.grow", { onclick: () => listDialog(ui) }, "Ver lista de jugadas"),
      h("button.btn.small.outlined", { disabled: ui.index >= ui.game.Turns.length - 1, onclick: () => move(ui, 1) }, "›"),
    ),
    page.status ? h("p.review-status", {}, page.status) : null,
    movesTable(ui, page, (m) => select(ui, m)),
  );
}

function titleOf(ui, t) {
  if (t.Endgame) return "Análisis del final";
  return ui.position ? "Análisis del pre-final" : "Análisis";
}

function pageFor(ui, t) {
  if (t.PreEndgame && ui.classic) return preEndgamePage(ui, t);
  if (ui.position) return pageOf(t, true);
  const ranked = ui.ranked.get(ui.index);
  if (ranked) return ranked;
  rankTurn(ui, ui.index);
  return pageOf(t, ui.classic);
}

async function rankTurn(ui, index) {
  if (ui.ranking.has(index)) return;
  ui.ranking.add(index);
  const ranked = await call("reviewRank", ui.text, index).catch(() => null);
  if (!ranked) return;
  ranked.cells = parseBoard(ranked.Board);
  ui.ranked.set(index, pageOf(ranked, ui.classic));
  if (ui.index === index && !ui.selected) draw(ui);
}

function preEndgamePage(ui, t) {
  const known = ui.pre.get(ui.index);
  if (known) return known;
  analyzePreEndgame(ui, ui.index, t);
  return { ...pageOf(t, ui.classic), status: "Pensando el pre-final…" };
}

async function analyzePreEndgame(ui, index, t) {
  if (ui.preRunning.has(index)) return;
  ui.preRunning.add(index);
  const view = await ui.deep(index).catch(() => null);
  ui.pre.set(index, view ? preEndgameOf(t, view, ui.classic) : { ...pageOf(t, ui.classic), status: PRE_STATUS.failed });
  if (ui.index === index && !ui.selected) draw(ui);
}

const PRE_STATUS = {
  bingo: "Este scrabble gana el final aun en el peor caso: va primero.",
  pass: "Pasando ya se gana el final: va primero.",
  failed: "No se pudo analizar el pre-final de este turno.",
};

function preEndgameOf(t, view, classic) {
  const who = t.Marks?.[0]?.Who ?? "";
  const rated = (m, playedBy = "") => ({ ...moveOf(t, { Description: m.Description, Score: m.Score }), equity: m.Value, adjusted: true, note: noteOf(m), playedBy });
  const candidates = (view.Moves ?? []).map((m, i) => rated(m, i === view.PlayedRank ? alias(who) : ""));
  const marks = view.Played ? [{ who, rank: view.PlayedRank >= 0 ? view.PlayedRank : null, move: rated(view.Played) }] : pageOf(t, classic).marks;
  return { candidates, marks, status: PRE_STATUS[view.Verdict] ?? null };
}

function noteOf(m) {
  if (m.Verdict === "bingo") return "scrabble ganador";
  if (m.Verdict === "pass") return "pasar gana";
  if (m.Adjustment) return `Q ${m.Adjustment > 0 ? "+" : ""}${m.Adjustment.toFixed(1)}`;
  return "";
}

function drawEndgame(ui, t) {
  if (ui.explorer?.index !== ui.index) {
    closeExplorer(ui);
    const isMine = mine(ui, t);
    const players = isMine ? [ui.me, ui.opponent] : [ui.opponent, ui.me];
    ui.explorer = endgamePage(ui, ui.index, players, isMine ? 0 : 1);
  }
  ui.onSquare = (i) => ui.explorer.onSquare(i);
  ui.explorer.draw();
}

function closeExplorer(ui) {
  ui.explorer?.close();
  ui.explorer = null;
}

function mine(ui, t) {
  return t.Player === "Tú" || t.Player === ui.game.Me;
}

function headActions(ui, t) {
  const bag = turnBag(t, ui.classic);
  const bagButton = h("button.bag-btn", { "aria-label": "Bolsa", onclick: () => showBag(bag.count, bag.rival, bag.tiles) }, bagIcon(bag.count));
  if (ui.position) return h("div.row.review-actions", {}, bagButton);
  return h("div.row.review-actions", {},
    h("button.text-btn", { onclick: () => reviewMovesDialog(ui, (i) => goTo(ui, i)) }, "Movidas"),
    h("button.icon-btn", { "aria-label": "Tema", onclick: themeDialog }, icon("palette")),
    h("button.icon-btn", { "aria-label": ui.classic ? "Ventaja" : "Eficiencia", onclick: () => chartDialog(ui) }, icon("chart")),
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
  goTo(ui, next);
}

function listDialog(ui) {
  openDialog((close) => [
    h("h2", {}, "Jugadas"),
    h("div.moves-scroll", {}, candidatesTable(ui.page.candidates, ui.selected, (m) => (close(), (ui.selected = m), draw(ui)))),
    actions(h("button.text-btn", { onclick: close }, "Cerrar")),
  ], { wide: true });
}

function keys(ui, e) {
  if (document.querySelector(".scrim") || !ui.game || ui.explorer) return;
  if (e.key === "ArrowLeft") move(ui, -1);
  if (e.key === "ArrowRight") move(ui, 1);
}
