import { BLANK, emptyBoard, fullBag, readWord, written } from "../tiles.js";
import { applyPlacement, parsePlacement } from "../placement.js";

const RACK_SIZE = 7;
const PENALTY_PER_MINUTE = 10;

export function parseSave(text) {
  const match = JSON.parse(text);
  const game = JSON.parse(match.Game);
  const history = typeof game.History === "string" ? JSON.parse(game.History) : game.History;
  return { match, game, history };
}

export function readSave(saved, guest, elapsedMs = 0) {
  const { match, game, history } = saved;
  const human = game.HumanIdx;
  const moves = game.Moves ?? [];
  const cells = boardOf(moves);
  const racks = (history.lastKnownRacks ?? ["", ""]).map(rackTokens);
  const over = isOver(game, history);
  const humanToMove = !over && moves.length % 2 === human;
  const clocks = clocksOf(match, humanToMove, over, elapsedMs);
  const penalties = clocks ? [penalty(match, clocks.humanSpent), penalty(match, clocks.botSpent)] : [0, 0];
  const points = pointsOf(game, moves);
  const scores = [points[0] - penalties[0], points[1] - penalties[1]];
  const onBoard = cells.filter(Boolean).length;
  const viewer = guest ? 1 - human : human;
  const known = racks[viewer];
  const counts = [0, 1].map((i) => (racks[i].length || i === viewer ? racks[i].length : Math.min(RACK_SIZE, 100 - onBoard - known.length)));
  return {
    Opponent: game.BotName, Host: game.Log?.human_name ?? "", Local: true,
    Board: boardText(cells), Rack: racks[human].join(" "), OpponentRack: racks[1 - human].join(" "),
    Unseen: unseen(cells, racks.flat()).join(" "),
    Status: {
      Over: over, HumanToMove: humanToMove, HumanScore: scores[0], BotScore: scores[1],
      BagCount: 100 - onBoard - counts[0] - counts[1], OpponentTiles: counts[1 - human], HumanTiles: counts[human],
    },
    Moves: moves, Clocks: clocks?.view ?? null, Paused: false,
    Result: over ? result(game, scores, penalties) : null, BotError: "",
  };
}

function rackTokens(text) {
  return readWord(text ?? "").filter(Boolean).map((t) => (t.blank ? BLANK : t.letter));
}

function boardOf(moves) {
  return moves.filter((m) => m.Kind === "play").reduce((cells, m) => {
    const p = parsePlacement(`${m.Coords} ${m.Tiles}`);
    return p ? applyPlacement(cells, p) : cells;
  }, emptyBoard());
}

function boardText(cells) {
  return cells.map((t) => (t ? written(t).replace(/[[\]]/g, "") : ".")).join(" ");
}

function isOver(game, history) {
  return !!(game.LostOnTime || game.RivalLostOnTime || game.Ending || history.playState === "GAME_OVER");
}

function pointsOf(game, moves) {
  const last = moves.at(-1);
  const totals = last ? [last.HumanTotal, last.BotTotal] : [0, 0];
  if (!game.Ending) return totals;
  return [totals[0] + game.Ending.HumanDelta, totals[1] + game.Ending.BotDelta];
}

function clocksOf(match, humanToMove, over, elapsedMs) {
  if (!(match.TimeMs > 0)) return null;
  const running = !over;
  const humanSpent = match.HumanSpentMs + (running && humanToMove ? elapsedMs : 0);
  const botSpent = match.BotSpentMs + (running && !humanToMove ? elapsedMs : 0);
  const side = (spent) => [match.TimeMs - spent, Math.max(match.OvertimeMs + Math.min(match.TimeMs - spent, 0), 0)];
  const [hm, ho] = side(humanSpent);
  const [bm, bo] = side(botSpent);
  const view = { HumanMs: hm, HumanOvertimeMs: ho, BotMs: bm, BotOvertimeMs: bo, Running: running ? (humanToMove ? "human" : "bot") : "" };
  return { view, humanSpent, botSpent };
}

function penalty(match, spent) {
  const over = spent - match.TimeMs;
  if (over <= 0) return 0;
  const minutes = Math.floor(Math.ceil(over / 1000) / 60) + 1;
  return PENALTY_PER_MINUTE * Math.min(minutes, Math.ceil(match.OvertimeMs / 60000));
}

function unseen(cells, rack) {
  const left = fullBag();
  const take = (letter) => {
    const i = left.indexOf(letter);
    if (i >= 0) left.splice(i, 1);
  };
  cells.forEach((t) => t && take(t.blank ? BLANK : t.letter));
  rack.forEach(take);
  return left;
}

function result(game, scores, penalties) {
  const outcome = game.LostOnTime ? "loss" : game.RivalLostOnTime ? "win" : scores[0] > scores[1] ? "win" : scores[0] < scores[1] ? "loss" : "tie";
  return {
    Outcome: outcome, LostOnTime: !!game.LostOnTime, RivalLostOnTime: !!game.RivalLostOnTime,
    EndReason: game.Ending?.Reason ?? "", HumanDelta: game.Ending?.HumanDelta ?? 0, BotDelta: game.Ending?.BotDelta ?? 0,
    HumanTime: penalties[0], BotTime: penalties[1], HumanScore: scores[0], BotScore: scores[1],
  };
}
