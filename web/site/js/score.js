import { SIZE, bonusAt, value } from "./tiles.js";
import { at } from "./placement.js";

const BINGO_TILES = 7;
const BINGO_BONUS = 50;
const LETTER_FACTOR = { dl: 2, tl: 3 };
const WORD_FACTOR = { dw: 2, tw: 3, center: 2 };

export function placementScore(cells, p) {
  const fresh = new Map();
  p.tiles.forEach((t, i) => t && fresh.set(at(p, i), t));
  if (!fresh.size) return 0;
  const words = wordsFormed(cells, fresh, p.vertical).filter((w) => w.length > 1);
  const total = words.reduce((sum, w) => sum + wordScore(cells, fresh, w), 0);
  return total + (fresh.size === BINGO_TILES ? BINGO_BONUS : 0);
}

function wordsFormed(cells, fresh, vertical) {
  const squares = [...fresh.keys()];
  if (squares.length === 1) return [wordThrough(cells, fresh, squares[0], false), wordThrough(cells, fresh, squares[0], true)];
  return [wordThrough(cells, fresh, squares[0], vertical), ...squares.map((s) => wordThrough(cells, fresh, s, !vertical))];
}

function wordThrough(cells, fresh, square, vertical) {
  const step = vertical ? SIZE : 1;
  const filled = (i) => i >= 0 && i < SIZE * SIZE && sameLine(i, square, vertical) && !!(cells[i] || fresh.has(i));
  let start = square;
  while (filled(start - step)) start -= step;
  const word = [];
  for (let i = start; filled(i); i += step) word.push(i);
  return word;
}

function sameLine(i, square, vertical) {
  return vertical ? i % SIZE === square % SIZE : Math.floor(i / SIZE) === Math.floor(square / SIZE);
}

function wordScore(cells, fresh, squares) {
  let letters = 0;
  let factor = 1;
  for (const i of squares) {
    const placed = fresh.get(i);
    const bonus = placed ? bonusAt(Math.floor(i / SIZE), i % SIZE) : null;
    letters += value(placed ?? cells[i]) * (LETTER_FACTOR[bonus] ?? 1);
    factor *= WORD_FACTOR[bonus] ?? 1;
  }
  return letters * factor;
}
