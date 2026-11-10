import { applyPlacement, parsePlacement, at } from "../placement.js";
import { emptyBoard } from "../tiles.js";
import { call } from "../engine.js";

const MIN_TILES = 3;
const RACK_SIZE = 7;
const SHUFFLE_TRIES = 20;

export function dictionaryText(tiles) {
  return tiles.map((t) => (t.length > 1 ? `[${t}]` : t)).join("");
}

export function mainWords(game) {
  let cells = emptyBoard();
  return game.flatMap(({ placement, score }) => {
    const p = parsePlacement(placement);
    if (!p) return [];
    cells = applyPlacement(cells, p);
    return [{ tiles: p.tiles.map((_, i) => cells[at(p, i)].letter), score }];
  });
}

export async function pick(game, count) {
  const seen = new Set();
  const chosen = mainWords(game)
    .filter((w) => w.tiles.length >= MIN_TILES)
    .sort((a, b) => b.score - a.score)
    .filter((w) => !seen.has(w.tiles.join()) && seen.add(w.tiles.join()))
    .slice(0, count);
  const words = shuffle(chosen);
  return Promise.all(words.map(toRecall));
}

async function toRecall({ tiles, score }) {
  const fixed = await chooseFixed(tiles);
  const rack = tiles.filter((_, i) => !fixed.has(i));
  return { tiles, score, fixed, scrambled: scramble(rack), text: tiles.join("") };
}

async function chooseFixed(tiles) {
  const count = tiles.length - RACK_SIZE;
  if (count <= 0) return new Set();
  const options = await fixedOptions(tiles, count);
  return options.length ? options[Math.floor(Math.random() * options.length)] : new Set();
}

export async function fixedOptions(tiles, count) {
  const candidates = subsets(tiles.length, count);
  const runsOf = (fixed) => runs(fixed).filter((r) => r.length > 1).map((r) => dictionaryText(r.map((i) => tiles[i])));
  const all = [...new Set(candidates.flatMap(runsOf))];
  const valid = new Map(all.map((w, i) => [w, false]));
  if (all.length) (await call("areWords", all)).forEach((ok, i) => valid.set(all[i], ok));
  return candidates.filter((fixed) => runsOf(fixed).every((w) => valid.get(w)));
}

function runs(fixed) {
  return [...fixed].sort((a, b) => a - b).reduce((out, i) => {
    const last = out.at(-1);
    if (last && last.at(-1) === i - 1) last.push(i);
    else out.push([i]);
    return out;
  }, []);
}

function subsets(n, size) {
  const out = [];
  for (let mask = 0; mask < 1 << n; mask++) {
    let bits = 0;
    for (let m = mask; m; m &= m - 1) bits++;
    if (bits === size) out.push(new Set([...Array(n).keys()].filter((i) => mask & (1 << i))));
  }
  return out;
}

export function scramble(tiles) {
  let out = shuffle(tiles);
  for (let i = 0; i < SHUFFLE_TRIES && out.join() === tiles.join() && tiles.length > 1; i++) out = shuffle(tiles);
  return out;
}

export function shuffle(list) {
  const out = list.slice();
  for (let i = out.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [out[i], out[j]] = [out[j], out[i]];
  }
  return out;
}
