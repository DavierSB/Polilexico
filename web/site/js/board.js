import { h, svg } from "./dom.js";
import { SIZE, bonusAt, blankFace, value } from "./tiles.js";
import { LABELS } from "./theme.js";

const ARROW = `<svg viewBox="0 0 100 100"><rect class="frame-edge" x="9.5" y="9.5" width="81" height="81" rx="12" stroke-width="13"/><rect class="frame" x="9.5" y="9.5" width="81" height="81" rx="12" stroke-width="9"/><g class="halo"><line x1="20" y1="50" x2="54" y2="50" stroke-width="19" stroke-linecap="round"/><path d="M80 50L54 29.2V70.8Z" stroke-width="8" stroke-linejoin="round"/></g><g class="shaft"><line x1="20" y1="50" x2="54" y2="50" stroke-width="11" stroke-linecap="round"/><path d="M80 50L54 29.2V70.8Z" stroke="none"/></g></svg>`;

export function createBoard(events = {}) {
  const squares = [];
  const grid = h("div.board-grid", {}, corner(), columns(), rows(squares, events));
  const el = h("div.board", {}, grid);
  return { el, squares, render: (view) => squares.forEach((sq, i) => paint(sq, i, view)) };
}

function corner() {
  return h("div.coord");
}

function columns() {
  return Array.from({ length: SIZE }, (_, c) => h("div.coord", {}, c + 1));
}

function rows(squares, events) {
  return Array.from({ length: SIZE }, (_, r) => [
    h("div.coord", {}, String.fromCharCode(65 + r)),
    Array.from({ length: SIZE }, (_, c) => square(r * SIZE + c, squares, events)),
  ]);
}

function square(i, squares, events) {
  const bonus = bonusAt(Math.floor(i / SIZE), i % SIZE);
  const el = h("div.square", { class: bonus ?? "", "data-index": i });
  el.addEventListener("click", () => events.onSquare?.(i));
  squares[i] = el;
  return el;
}

function paint(el, i, view) {
  const key = paintKey(i, view);
  if (el.dataset.key === key) return;
  el.dataset.key = key;
  el.replaceChildren(...content(i, view));
}

function paintKey(i, view) {
  const t = view.cells[i] ?? view.pending?.get(i);
  const arrow = view.arrow?.index === i ? (view.arrow.vertical ? "v" : "h") : "";
  const score = view.score?.index === i ? view.score.value : "";
  return [t?.letter, t?.blank, view.pending?.has(i), view.latest?.has(i), arrow, score].join("|");
}

function content(i, view) {
  const placed = view.cells[i];
  const pending = view.pending?.get(i);
  const parts = [];
  if (placed) parts.push(tile(placed, view.latest?.has(i) ? "latest" : ""));
  else if (pending) parts.push(tile(pending, "pending"));
  else parts.push(label(i));
  if (!placed && !pending && view.arrow?.index === i) parts.push(arrow(view.arrow.vertical));
  if (view.score?.index === i) parts.push(bubble(view.score.value));
  return parts;
}

function label(i) {
  const bonus = bonusAt(Math.floor(i / SIZE), i % SIZE);
  return bonus ? LABELS[bonus] : "";
}

export function tile(t, extra = "") {
  const classes = ["tile", extra, t.blank ? "blank" : "", t.letter.length > 1 ? "digraph" : ""].join(" ");
  return h("div", { class: classes }, blankFace(t), t.blank ? null : h("span.value", {}, value(t)));
}

function arrow(vertical) {
  return h("div", { class: "arrow" + (vertical ? " vertical" : "") }, svg(ARROW));
}

function bubble(score) {
  return h("div", { class: "score-bubble" + (score >= 50 ? " high" : "") }, score);
}
