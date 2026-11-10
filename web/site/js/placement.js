import { SIZE, readWord, written } from "./tiles.js";

const HORIZONTAL = /^([A-Oa-o])([0-9]{1,2})$/;
const VERTICAL = /^([0-9]{1,2})([A-Oa-o])$/;

export function coordinates(p) {
  const row = String.fromCharCode(65 + p.row);
  return p.vertical ? `${p.col + 1}${row}` : `${row}${p.col + 1}`;
}

export function placementText(p) {
  return coordinates(p) + " " + p.tiles.map(written).join("");
}

export function spelled(p, cells) {
  return coordinates(p) + " " + p.tiles.map((t, i) => (t ?? cells[at(p, i)])?.letter ?? ".").join("");
}

export function parsePlacement(text) {
  const [coords, word] = (text ?? "").trim().split(/\s+/);
  const origin = parseCoords(coords ?? "");
  if (!origin || !word) return null;
  return { ...origin, tiles: readWord(word) };
}

function parseCoords(text) {
  const h = HORIZONTAL.exec(text);
  if (h) return { row: rowOf(h[1]), col: Number(h[2]) - 1, vertical: false };
  const v = VERTICAL.exec(text);
  if (v) return { row: rowOf(v[2]), col: Number(v[1]) - 1, vertical: true };
  return null;
}

function rowOf(letter) {
  return letter.toUpperCase().charCodeAt(0) - 65;
}

export function at(p, i) {
  return p.vertical ? (p.row + i) * SIZE + p.col : p.row * SIZE + p.col + i;
}

export function placedSquares(p) {
  return p.tiles.map((t, i) => (t ? at(p, i) : -1)).filter((i) => i >= 0);
}

export function applyPlacement(cells, p) {
  const next = cells.slice();
  p.tiles.forEach((t, i) => t && (next[at(p, i)] = t));
  return next;
}

export function writePlacement(cells, pending) {
  const squares = [...pending.keys()];
  if (!squares.length || squares.some((i) => cells[i])) return null;
  const vertical = direction(cells, squares);
  if (vertical === null) return null;
  return along(cells, pending, squares, vertical);
}

function direction(cells, squares) {
  const rows = new Set(squares.map((i) => Math.floor(i / SIZE)));
  const cols = new Set(squares.map((i) => i % SIZE));
  if (squares.length === 1) return singleVertical(cells, squares[0]);
  if (rows.size === 1) return false;
  if (cols.size === 1) return true;
  return null;
}

function singleVertical(cells, i) {
  const r = Math.floor(i / SIZE), c = i % SIZE;
  const filled = (rr, cc) => rr >= 0 && rr < SIZE && cc >= 0 && cc < SIZE && cells[rr * SIZE + cc];
  return !(filled(r, c - 1) || filled(r, c + 1)) && !!(filled(r - 1, c) || filled(r + 1, c));
}

function along(cells, pending, squares, vertical) {
  const fixed = vertical ? squares[0] % SIZE : Math.floor(squares[0] / SIZE);
  const square = (k) => (vertical ? k * SIZE + fixed : fixed * SIZE + k);
  const indexOf = (i) => (vertical ? Math.floor(i / SIZE) : i % SIZE);
  const occupied = (k) => k >= 0 && k < SIZE && (cells[square(k)] || pending.has(square(k)));
  let start = Math.min(...squares.map(indexOf));
  let end = Math.max(...squares.map(indexOf));
  while (occupied(start - 1)) start--;
  while (occupied(end + 1)) end++;
  for (let k = start; k <= end; k++) if (!occupied(k)) return null;
  const tiles = [];
  for (let k = start; k <= end; k++) tiles.push(pending.get(square(k)) ?? null);
  const origin = square(start);
  return { row: Math.floor(origin / SIZE), col: origin % SIZE, vertical, tiles };
}
