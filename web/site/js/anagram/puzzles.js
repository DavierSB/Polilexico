import { call, whenReady } from "../engine.js";
import { readWord, tokens } from "../tiles.js";
import { dictionaryText, fixedOptions, scramble, shuffle } from "../recall/words.js";

export const MIN_TILES = 4;
export const MAX_TILES = 9;
export const MAX_ANSWERS = 5;
const RACK_SIZE = 7;
const MIN_RACK = 3;
const MAX_TRIES = 300;

export const DIFFICULTIES = {
  easy: { label: "Fácil", findAll: false, accepts: (n) => n >= 2 },
  medium: { label: "Medio", findAll: false, accepts: (n) => n >= 1 && n <= MAX_ANSWERS },
  hard: { label: "Difícil", findAll: true, accepts: (n) => n >= 1 && n <= MAX_ANSWERS },
};

export async function nextPuzzle(difficulty) {
  await whenReady();
  for (let i = 0; i < MAX_TRIES; i++) {
    const puzzle = await fit(lettersOf(await call("randomWord", MIN_TILES, MAX_TILES)), difficulty);
    if (puzzle) return puzzle;
  }
  throw new Error("no se encontró una palabra para esta dificultad");
}

function lettersOf(word) {
  return readWord(word).filter(Boolean).map((t) => t.letter);
}

async function fit(word, difficulty) {
  const all = unique(tokens(await call("anagramTiles", dictionaryText(word))).map(lettersOf));
  if (!all.some((a) => a.join() === word.join())) return null;
  for (const count of fixedCounts(word.length, difficulty)) {
    const options = shuffle(await fixedOptions(word, count));
    const found = options.map((fixed) => puzzle(word, fixed, all)).find((p) => DIFFICULTIES[difficulty].accepts(p.answers.length));
    if (found) return found;
  }
  return null;
}

function unique(words) {
  const seen = new Set();
  return words.filter((w) => !seen.has(w.join()) && seen.add(w.join()));
}

function fixedCounts(size, difficulty) {
  const first = Math.max(0, size - RACK_SIZE);
  const last = difficulty === "easy" ? first : Math.max(first, size - MIN_RACK);
  return Array.from({ length: last - first + 1 }, (_, i) => first + i);
}

function puzzle(word, fixed, all) {
  const answers = all.filter((a) => [...fixed].every((i) => a[i] === word[i]));
  return { tiles: word, fixed, rack: scramble(word.filter((_, i) => !fixed.has(i))), answers };
}
