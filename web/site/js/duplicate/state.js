import { parseBoard, tokens } from "../tiles.js";
import { parsePlacement, placedSquares, spelled } from "../placement.js";

const BINGO_TILES = 7;
const MINE_TEXT = { invalid: "inválida", timeout: "tiempo agotado" };

export function normalizeDuplicate(raw) {
  const cells = parseBoard(raw.Board);
  const finished = raw.Phase === "finished";
  const rounds = (raw.Turns ?? []).map((t) => roundOf(t, cells));
  const shown = raw.RevealAtEnd && !finished ? rounds.map(assumedRound) : rounds.map(realRound);
  return {
    boardText: raw.Board, cells, latest: latest(raw.Turns), phase: raw.Phase, finished,
    rack: tokens(raw.Rack), invalidRack: tokens(raw.InvalidRack), proposal: proposalText(raw.Proposal, cells),
    turnMs: raw.TurnMs, remainingMs: raw.RemainingMs, cancelMs: raw.CancelMs, lastError: raw.LastError || null,
    paused: raw.Paused && !finished, bag: tokens(raw.Unseen), round: finished ? (raw.Turns ?? []).length : raw.Status.Turn, revealAtEnd: raw.RevealAtEnd,
    rounds: shown, invalidPlays: rounds.filter((r) => r.assumed.invalid).map((r) => ({ number: r.number, ...r.assumed })),
  };
}

export function totals(rounds) {
  const mine = rounds.reduce((s, r) => s + r.mine.score, 0);
  const master = rounds.reduce((s, r) => s + r.master.score, 0);
  return { mine, master, hits: rounds.filter((r) => r.hit).length, efficiency: master ? (mine * 100) / master : 0 };
}

function roundOf(t, cells) {
  const master = play(t.MasterCoords, t.MasterTiles, cells);
  const mine = t.HumanKind === "play" ? play(t.HumanCoords, t.HumanTiles, cells) : { text: MINE_TEXT[t.HumanKind] ?? "pase", bingo: false };
  const assumed = t.HumanKind === "invalid" ? { ...play(t.HumanCoords, t.HumanTiles, cells), score: t.AssumedScore, hit: t.AssumedHit, invalid: true } : null;
  return {
    number: t.Number, master: { ...master, score: t.MasterScore }, mine: { ...mine, score: t.HumanScore }, hit: t.Hit,
    assumed: assumed ?? { ...mine, score: t.HumanScore, hit: t.Hit, invalid: false },
  };
}

function play(coords, tiles, cells) {
  const p = parsePlacement(`${coords} ${tiles}`);
  if (!p) return { text: tiles || "pase", bingo: false };
  return { text: spelled(p, cells), bingo: placedSquares(p).length === BINGO_TILES };
}

function realRound(r) {
  return { number: r.number, master: r.master, mine: r.mine, hit: r.hit };
}

function assumedRound(r) {
  return { number: r.number, master: r.master, mine: r.assumed, hit: r.assumed.hit };
}

function latest(turns) {
  const last = turns?.at(-1);
  const p = last && parsePlacement(`${last.MasterCoords} ${last.MasterTiles}`);
  return new Set(p ? placedSquares(p) : []);
}

function proposalText(a, cells) {
  if (!a) return null;
  const p = parsePlacement(`${a.Coords} ${a.Tiles}`);
  return `${p ? spelled(p, cells) : a.Tiles} (${a.Score})`;
}
