export const SIZE = 15;
export const BLANK = "?";

const VALUES = {
  A: 1, B: 3, C: 3, CH: 5, D: 2, E: 1, F: 4, G: 2, H: 4, I: 1, J: 8, L: 1, LL: 8, M: 3, N: 1, Ñ: 8, O: 1, P: 3,
  Q: 5, R: 1, RR: 8, S: 1, T: 1, U: 1, V: 4, X: 8, Y: 4, Z: 10,
};

export const LETTERS = Object.keys(VALUES);

const COUNTS = {
  [BLANK]: 2, A: 12, B: 2, C: 4, CH: 1, D: 5, E: 12, F: 1, G: 2, H: 2, I: 6, J: 1, L: 4, LL: 1, M: 2, N: 5, Ñ: 1, O: 9, P: 2,
  Q: 1, R: 5, RR: 1, S: 6, T: 4, U: 5, V: 1, X: 1, Y: 1, Z: 1,
};

export function fullBag() {
  return Object.entries(COUNTS).flatMap(([letter, n]) => Array(n).fill(letter));
}

const QUADRANT = ["=  '   =", " -   \"  ", "  -   ' ", "'  -   '", "    -   ", " \"   \"  ", "  '   ' ", "=  '   *"];

const SYMBOLS = { "=": "tw", "-": "dw", '"': "tl", "'": "dl", "*": "center" };

export function value(tile) {
  return tile.blank ? 0 : VALUES[tile.letter] ?? 0;
}

export function rackValue(letter) {
  return letter === BLANK ? 0 : VALUES[letter] ?? 0;
}

export function bonusAt(row, col) {
  const fold = (i) => (i > 7 ? 14 - i : i);
  return SYMBOLS[QUADRANT[fold(row)][fold(col)]] ?? null;
}

export function tokens(text) {
  return (text ?? "").trim().split(/\s+/).filter(Boolean);
}

export function parseBoard(text) {
  const cells = tokens(text).map(squareTile);
  return cells.length === SIZE * SIZE ? cells : Array(SIZE * SIZE).fill(null);
}

function squareTile(token) {
  if (token === ".") return null;
  return { letter: token.toUpperCase(), blank: token !== token.toUpperCase() };
}

export function readWord(text) {
  const tiles = [];
  for (let i = 0; i < text.length; i++) {
    if (text[i] === "[") {
      const end = text.indexOf("]", i);
      tiles.push(squareTile(text.slice(i + 1, end)));
      i = end;
    } else tiles.push(text[i] === "." ? null : squareTile(text[i]));
  }
  return tiles;
}

export function blankFace(tile) {
  return tile.blank && tile.letter !== "L" ? tile.letter.toLowerCase() : tile.letter;
}

export function written(tile) {
  if (!tile) return ".";
  const text = tile.blank ? tile.letter.toLowerCase() : tile.letter;
  return tile.letter.length > 1 ? `[${text}]` : text;
}

export function plain(text) {
  return text.replace(/[[\]]/g, "");
}

export function emptyBoard() {
  return Array(SIZE * SIZE).fill(null);
}
