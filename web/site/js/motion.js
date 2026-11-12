import { h } from "./dom.js";
import { SIZE, bonusAt } from "./tiles.js";
import { LABELS } from "./theme.js";

const GAP_MS = 160;
const DROP_MS = 300;
const HIGH_SCORE = 40;
const RISE = 0.8;

export function landTiles(board, squares) {
  const order = [...squares].sort((a, b) => a - b);
  order.forEach((i, k) => drop(board.squares[i], i, k * GAP_MS));
  return order.length ? (order.length - 1) * GAP_MS + DROP_MS : 0;
}

export function floatScore(board, cells, squares, score, delayMs = 0) {
  board.el.querySelector(".score-float")?.remove();
  if (!squares.length || score == null) return;
  const vertical = isVertical(cells, squares);
  const anchor = vertical ? wordTop(cells, Math.min(...squares)) : Math.max(...squares);
  const el = scoreLabel(score, vertical, delayMs);
  board.squares[anchor].append(el);
  requestAnimationFrame(() => clamp(el, board.el));
}

function drop(square, i, delayMs) {
  const tile = square.querySelector(".tile");
  if (!tile) return;
  const bonus = bonusAt(Math.floor(i / SIZE), i % SIZE);
  if (bonus) square.prepend(LABELS[bonus]);
  tile.style.setProperty("--delay", `${delayMs}ms`);
  tile.classList.add("landing");
  tile.addEventListener("animationend", () => tile.classList.remove("landing"), { once: true });
}

function scoreLabel(score, vertical, delayMs) {
  const el = h("div", { class: "score-float" + (score >= HIGH_SCORE ? " high" : "") + (vertical ? " vertical" : "") }, `+${score}`);
  el.style.setProperty("--delay", `${delayMs}ms`);
  el.addEventListener("animationend", () => el.remove(), { once: true });
  return el;
}

function clamp(el, boardEl) {
  const box = el.getBoundingClientRect();
  const frame = boardEl.getBoundingClientRect();
  if (!frame.width) return;
  const dx = Math.min(0, frame.right - box.right) || Math.max(0, frame.left - box.left);
  const dy = Math.max(0, frame.top - box.top);
  const rise = el.parentElement.getBoundingClientRect().width * RISE;
  el.style.setProperty("--dx", `${dx}px`);
  el.style.setProperty("--dy", `${dy}px`);
  el.style.setProperty("--rise", `${Math.min(rise, box.top + dy - frame.top)}px`);
}

function isVertical(cells, squares) {
  if (squares.length > 1) return squares.every((i) => i % SIZE === squares[0] % SIZE);
  const i = squares[0];
  const column = filled(cells, i - SIZE) || filled(cells, i + SIZE);
  const row = (i % SIZE > 0 && filled(cells, i - 1)) || (i % SIZE < SIZE - 1 && filled(cells, i + 1));
  return column && !row;
}

function wordTop(cells, i) {
  let top = i;
  while (filled(cells, top - SIZE)) top -= SIZE;
  return top;
}

function filled(cells, i) {
  return i >= 0 && i < SIZE * SIZE && !!cells[i];
}
