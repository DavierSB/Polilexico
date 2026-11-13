import { parseBoard, tokens } from "../tiles.js";
import { parsePlacement, placedSquares, spelled } from "../placement.js";

export function normalize(raw, flipped) {
  const cells = parseBoard(raw.Board);
  const moves = (raw.Moves ?? []).map((m) => playedMove(m, cells));
  const state = {
    boardText: raw.Board, cells, latest: latestSquares(moves), rack: tokens(raw.Rack),
    myTiles: raw.Status.HumanTiles ?? tokens(raw.Rack).length, opponentTiles: raw.Status.OpponentTiles, opponentRack: tokens(raw.OpponentRack),
    myScore: raw.Status.HumanScore, opponentScore: raw.Status.BotScore, myTurn: raw.Status.HumanToMove,
    bag: raw.Status.BagCount, unseen: tokens(raw.Unseen), moves, clocks: clocks(raw.Clocks),
    paused: raw.Paused, result: result(raw.Result), botError: raw.BotError || null,
    opponent: raw.Opponent, host: raw.Host, local: raw.Local, over: raw.Status.Over,
  };
  return flipped ? flip(state) : state;
}

function playedMove(m, cells) {
  const p = m.Kind === "play" ? parsePlacement(`${m.Coords} ${m.Tiles}`) : null;
  return {
    byMe: m.ByHuman, kind: m.Kind, coords: m.Coords, tiles: m.Tiles, count: m.TileCount, score: m.Score,
    myTotal: m.HumanTotal, opponentTotal: m.BotTotal, placement: p,
    word: p ? spelled(p, cells).split(" ")[1] : m.Tiles.replace(/[[\]]/g, ""),
    words: (m.Words ?? "").split(" ").filter(Boolean), letters: m.Kind === "invalid" ? usedLetters(m) : [],
  };
}

function usedLetters(m) {
  const p = parsePlacement(`${m.Coords} ${m.Tiles}`);
  return (p?.tiles ?? []).filter(Boolean).map((t) => (t.blank ? "?" : t.letter));
}

function latestSquares(moves) {
  const last = moves.at(-1);
  return new Set(last?.placement ? placedSquares(last.placement) : []);
}

export function clocks(c) {
  if (!c) return null;
  return { myMs: c.HumanMs, myOvertimeMs: c.HumanOvertimeMs, opponentMs: c.BotMs, opponentOvertimeMs: c.BotOvertimeMs, running: running(c.Running) };
}

function running(side) {
  if (side === "human") return "me";
  if (side === "bot") return "opponent";
  return null;
}

function result(r) {
  if (!r) return null;
  return {
    outcome: r.Outcome, lostOnTime: r.LostOnTime, rivalLostOnTime: r.RivalLostOnTime, endReason: r.EndReason,
    myDelta: r.HumanDelta, opponentDelta: r.BotDelta, myPenalty: r.HumanTime, opponentPenalty: r.BotTime,
  };
}

export function flip(s) {
  return {
    ...s, rack: s.opponentRack, opponentRack: s.rack, myTiles: s.opponentTiles, opponentTiles: s.myTiles, myScore: s.opponentScore,
    opponentScore: s.myScore, myTurn: !s.myTurn, moves: s.moves.map(flipMove), clocks: flipClocks(s.clocks),
    result: flipResult(s.result), opponent: s.host, host: s.opponent,
  };
}

function flipMove(m) {
  return { ...m, byMe: !m.byMe, myTotal: m.opponentTotal, opponentTotal: m.myTotal };
}

export function flipClocks(c) {
  if (!c) return null;
  const running = c.running === "me" ? "opponent" : c.running === "opponent" ? "me" : null;
  return { myMs: c.opponentMs, myOvertimeMs: c.opponentOvertimeMs, opponentMs: c.myMs, opponentOvertimeMs: c.myOvertimeMs, running };
}

function flipResult(r) {
  if (!r) return null;
  const outcome = r.outcome === "win" ? "loss" : r.outcome === "loss" ? "win" : r.outcome;
  return {
    ...r, outcome, lostOnTime: r.rivalLostOnTime, rivalLostOnTime: r.lostOnTime,
    myDelta: r.opponentDelta, opponentDelta: r.myDelta, myPenalty: r.opponentPenalty, opponentPenalty: r.myPenalty,
  };
}
