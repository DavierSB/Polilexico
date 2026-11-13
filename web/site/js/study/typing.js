import { BLANK, LETTERS } from "../tiles.js";

const SHIFTED = { C: "CH", L: "LL", R: "RR" };

export function typedTile(e) {
  if (e.key === "?" || e.key === " ") return BLANK;
  if (e.key.length !== 1) return null;
  if (e.shiftKey && SHIFTED[e.key]) return SHIFTED[e.key];
  const letter = e.key.toUpperCase();
  return LETTERS.includes(letter) && letter.length === 1 ? letter : null;
}
