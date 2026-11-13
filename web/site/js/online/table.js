import { parseBoard, tokens } from "../tiles.js";
import { parsePlacement, placedSquares, spelled } from "../placement.js";

export const MASTER = "Máster";

export function readTable(json, me) {
  const o = JSON.parse(json);
  const cells = parseBoard(o.Board);
  const rounds = (o.Rounds ?? []).map((r) => ({ number: r.Number, entries: (r.Plays ?? []).map((p) => entry(p, cells)) }));
  return {
    me, boardText: o.Board, cells, latest: latest(o.Rounds), rack: tokens(o.Rack), phase: o.Phase, round: o.Round,
    remainingMs: o.RemainingMs >= 0 ? o.RemainingMs : null, receivedAt: performance.now(), proposal: o.Proposal ? proposal(o.Proposal, cells) : null,
    ready: o.Ready, waiting: o.Waiting, iAmReady: o.IAmReady, live: o.Live, players: o.Players ?? [], rounds, unseen: tokens(o.Unseen),
  };
}

export function standings(t) {
  return [...(t.rounds.at(-1)?.entries ?? [])].sort((a, b) => b.total - a.total);
}

export function placeOf(sorted, i) {
  return sorted.findIndex((e) => e.total === sorted[i].total) + 1;
}

export function leftMs(t) {
  return t.remainingMs === null ? null : Math.max(0, t.remainingMs - (performance.now() - t.receivedAt));
}

function entry(p, cells) {
  return { name: p.Name, text: playText(p.Kind, p.Coords, p.Tiles, cells), points: p.Points, total: p.Total, placement: p.Kind === "play" ? parsePlacement(`${p.Coords} ${p.Tiles}`) : null };
}

function proposal(a, cells) {
  return { text: playText(a.Kind, a.Coords, a.Tiles, cells), points: a.Score };
}

function playText(kind, coords, tiles, cells) {
  const p = parsePlacement(`${coords} ${tiles}`);
  const text = p ? spelled(p, cells) : `${coords} ${tiles}`;
  if (kind === "play") return text;
  if (kind === "invalid") return "inválida: " + text;
  return kind === "pass" ? "pase" : "sin jugada";
}

function latest(rounds) {
  const master = rounds?.at(-1)?.Plays?.[0];
  const p = master && parsePlacement(`${master.Coords} ${master.Tiles}`);
  return new Set(p ? placedSquares(p) : []);
}
