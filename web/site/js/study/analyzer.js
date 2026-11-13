import { h, replace } from "../dom.js";
import { typedTile } from "./typing.js";
import { screenHead } from "../parts.js";
import { call, whenReady } from "../engine.js";
import { createBoard } from "../board.js";
import { openDialog, actions } from "../dialogs.js";
import { BLANK, LETTERS, SIZE, emptyBoard, written } from "../tiles.js";
import { applyPlacement, parsePlacement, at, spelled } from "../placement.js";
import { face } from "../classic/rack.js";
import { askBlank } from "../classic/moves.js";
import { choiceRow, sectionTitle } from "../classic/options.js";
import { reviewPosition } from "../games/review.js";

const MAX_RACK = 7;
const BEST = 15;
const EQUITY_INFO = "El valor de la jugada, considerando su puntuación y lo que queda en la mano.";
const TARGET_INFO = "Tablero: toca una casilla para poner la flecha (otra vez: vertical) y luego letras; o elige una letra y toca casillas. Toca una ficha del tablero para quitarla.\n\nAtril: toca letras para añadirlas (máx. 7).";

let kept = null;

export function analyzerScreen(app) {
  const ed = kept ?? { cells: emptyBoard(), rack: [], target: "board", letter: null, arrow: null, result: null, selected: null, running: false, message: null, scores: null, };
  const board = createBoard({ onSquare: (i) => tapSquare(ui, i) });
  const rackRow = h("div.analysis-rack");
  const notice = h("p.muted.small");
  const panel = h("div.analysis-panel");
  kept = ed;
  const ui = { ed, board, rackRow, notice, panel };
  ui.redraw = () => draw(ui);
  app.append(h("div.analyzer", {},
    h("div.analyzer-head", {}, screenHead("Analizador", "estudiar", h("button.text-btn", { onclick: () => scoreDialog(ui) }, "Marcador"), h("button.text-btn", { onclick: () => clearAll(ui) }, "Vaciar"))),
    h("div.analyzer-rack", {}, rackRow, notice),
    h("div.analyzer-board", {}, board.el),
    h("div.analyzer-panel", {}, panel),
  ));
  const onKey = (e) => keys(ui, e);
  document.addEventListener("keydown", onKey);
  ui.redraw();
  return () => document.removeEventListener("keydown", onKey);
}

function draw(ui) {
  const ed = ui.ed;
  const preview = ed.selected?.placement;
  const pending = new Map();
  if (preview) preview.tiles.forEach((t, i) => t && !ed.cells[at(preview, i)] && pending.set(at(preview, i), t));
  ui.board.render({ cells: ed.cells, pending, arrow: !ed.result && ed.target === "board" ? ed.arrow : null });
  replace(ui.rackRow,
    h("span.section-title", {}, "Atril"),
    h("div.row.grow.analysis-tiles", {}, ed.rack.map((l, i) => h("button.rack-tile.palette-key", { onclick: () => removeFromRack(ui, i) }, keyFace(l)))),
    h("button.btn.small", { disabled: !ed.rack.length || ed.running, onclick: () => analyze(ui) }, ed.running ? "…" : "Analizar"),
  );
  ui.notice.textContent = ed.message ?? "";
  ui.panel.classList.toggle("editing", !ed.result);
  replace(ui.panel, ed.result ? results(ui) : editPanel(ui));
}

function keyFace(letter) {
  return letter === BLANK ? h("span.rack-letter.blank-mark", {}, "?") : face(letter);
}

function editPanel(ui) {
  const ed = ui.ed;
  return [
    h("div.row", {}, sectionTitle("Poner en:", TARGET_INFO), choiceRow(["Tablero", "Atril"], ed.target === "rack" ? 1 : 0, (i) => selectTarget(ui, i ? "rack" : "board"))),
    h("div.palette", {}, [...LETTERS, BLANK].map((l) => h("button", { class: "rack-tile palette-key" + (ed.target === "board" && ed.letter === l ? " selected" : ""), onclick: () => tapLetter(ui, l) }, keyFace(l)))),
  ];
}

function results(ui) {
  const ed = ui.ed;
  return [
    h("div.row", {},
      h("b.grow", {}, "Mejores jugadas"),
      ed.selected?.placement ? h("button.text-btn", { onclick: () => placeCandidate(ui) }, "Ponerla") : null,
      h("button.text-btn", { onclick: () => edit(ui) }, "Editar"),
    ),
    h("div.moves-scroll", {}, h("table.moves.ranked", {},
      h("thead", {}, h("tr", {}, h("th", {}, "#"), h("th", {}, "Jugada"), h("th.num", {}, "Puntos"), h("th.num", {}, h("button.text-btn.th-info", { onclick: equityInfo }, "Equity ⓘ")))),
      h("tbody", {}, ed.result.map((m, n) => h("tr", { class: m === ed.selected ? "active" : "", onclick: () => ((ed.selected = m), ui.redraw()) },
        h("td.muted", {}, n + 1), h("td", {}, m.text), h("td.num", {}, m.score), h("td.num", {}, m.equity.toFixed(1)),
      ))),
    )),
  ];
}

function equityInfo() {
  openDialog((close) => [h("h2", {}, "Equity"), h("p", {}, EQUITY_INFO), actions(h("button.text-btn", { onclick: close }, "Entendido"))]);
}

function selectTarget(ui, target) {
  ui.ed.target = target;
  if (target === "rack") ui.ed.letter = null;
  ui.redraw();
}

function tapLetter(ui, l) {
  const ed = ui.ed;
  if (ed.target === "rack") return addToRack(ui, l);
  if (ed.arrow) return put(ui, l, ed.arrow.index, true);
  ed.letter = ed.letter === l ? null : l;
  ui.redraw();
}

function tapSquare(ui, i) {
  const ed = ui.ed;
  if (ed.result || ed.target === "rack") return;
  if (ed.cells[i]) return update(ui, (cells) => (cells[i] = null));
  if (ed.letter && !ed.arrow) return put(ui, ed.letter, i, false);
  if (ed.arrow?.index !== i) ed.arrow = { index: i, vertical: false };
  else ed.arrow = ed.arrow.vertical ? null : { index: i, vertical: true };
  ui.redraw();
}

async function put(ui, l, i, fromArrow) {
  const blank = l === BLANK;
  const letter = blank ? await askBlank() : l;
  if (!letter) return;
  update(ui, (cells) => (cells[i] = { letter, blank }));
  if (fromArrow) advance(ui, i);
}

function advance(ui, from) {
  const a = ui.ed.arrow;
  const step = a.vertical ? SIZE : 1;
  let next = from + step;
  const inLine = (n) => n < SIZE * SIZE && (a.vertical || Math.floor(n / SIZE) === Math.floor(from / SIZE));
  while (inLine(next) && ui.ed.cells[next]) next += step;
  ui.ed.arrow = inLine(next) ? { ...a, index: next } : null;
  ui.redraw();
}

function update(ui, change) {
  const cells = ui.ed.cells.slice();
  change(cells);
  ui.ed.cells = cells;
  resetAnalysis(ui);
}

function addToRack(ui, l) {
  if (ui.ed.rack.length >= MAX_RACK) return;
  ui.ed.rack.push(l);
  resetAnalysis(ui);
}

function removeFromRack(ui, i) {
  ui.ed.rack.splice(i, 1);
  resetAnalysis(ui);
}

function scoreDialog(ui) {
  const s = ui.ed.scores ?? { mine: 0, theirs: 0 };
  const field = (label, key) => h("div.field", {}, h("label", {}, label), h("input", { type: "number", min: 0, max: 9999, value: s[key], oninput: (e) => ((s[key] = Number(e.target.value) || 0), (s.touched = true)) }));
  openDialog((close) => [
    h("h2", {}, "Marcador"), field("Tú", "mine"), field("Rival", "theirs"),
    h("p.muted.small", {}, "Con marcador, en el pre-final y en el final se analiza esa fase (pasar o el scrabble pueden ganar). Sin marcador, solo equity."),
    actions(h("button.text-btn", { onclick: close }, "Listo")),
  ], { onClose: () => ((ui.ed.scores = s.touched ? s : ui.ed.scores), resetAnalysis(ui)) });
}

function resetAnalysis(ui) {
  Object.assign(ui.ed, { result: null, selected: null, message: null });
  ui.redraw();
}

function edit(ui) {
  resetAnalysis(ui);
}

function clearAll(ui) {
  Object.assign(ui.ed, { cells: emptyBoard(), rack: [], letter: null, arrow: null });
  resetAnalysis(ui);
}

function placeCandidate(ui) {
  const p = ui.ed.selected?.placement;
  if (!p) return;
  ui.ed.cells = applyPlacement(ui.ed.cells, p);
  resetAnalysis(ui);
}

async function analyze(ui) {
  const ed = ui.ed;
  ed.running = true;
  ui.redraw();
  try {
    await whenReady();
    const boardText = ed.cells.map((t) => (t ? written(t).replace(/[[\]]/g, "") : ".")).join(" ");
    const rackText = ed.rack.map((t) => (t.length > 1 ? `[${t}]` : t)).join("");
    if (ed.scores && (await deepAnalysis(ui, boardText, rackText))) return;
    const found = await call("bestMoves", boardText, rackText, BEST);
    ed.result = found.map((c) => candidate(c, ed.cells));
    ed.selected = ed.result[0] ?? null;
    ed.message = ed.result.length ? null : "No hay jugadas posibles.";
    if (!ed.result.length) ed.result = null;
  } catch (e) {
    ed.message = e.message;
  }
  ed.running = false;
  ui.redraw();
}

async function deepAnalysis(ui, boardText, rackText) {
  const ed = ui.ed;
  const phase = await call("positionPhase", boardText, rackText, ed.scores.mine, ed.scores.theirs);
  if (phase === "midgame") return false;
  ed.running = false;
  reviewPosition({ board: boardText, rack: rackText, mine: ed.scores.mine, theirs: ed.scores.theirs });
  return true;
}

function candidate(c, cells) {
  const text = c.description.replace(/ \(\d+ pts\)$/, "");
  const placement = text.startsWith("(") ? null : parsePlacement(text);
  return { text: placement ? spelled(placement, cells) : text, score: c.score, equity: c.equity, placement };
}

function keys(ui, e) {
  if (e.ctrlKey || e.metaKey || e.altKey || e.target instanceof HTMLInputElement || document.querySelector(".scrim") || ui.ed.result) return;
  if (e.key === "Enter" && ui.ed.rack.length) return analyze(ui);
  if (e.key === "Escape") return ((ui.ed.arrow = null), ui.redraw());
  if (e.key === "Backspace" && !ui.ed.arrow && ui.ed.rack.length) return (e.preventDefault(), removeFromRack(ui, ui.ed.rack.length - 1));
  const letter = typedTile(e);
  if (!letter) return;
  e.preventDefault();
  if (ui.ed.arrow && ui.ed.target === "board") return put(ui, letter, ui.ed.arrow.index, true);
  addToRack(ui, letter);
}
