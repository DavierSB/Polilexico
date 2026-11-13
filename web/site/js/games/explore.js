import { call, watch, unwatch } from "../engine.js";
import { parseBoard, tokens } from "../tiles.js";
import { parsePlacement, spelled, applyPlacement } from "../placement.js";

const BUDGET_MS = 30000;

export function describe(description, cells) {
  const raw = (description ?? "").replace(/ \(\d+ pts\)$/, "");
  const placement = parsePlacement(raw);
  return { text: placement ? spelled(placement, cells) : raw.replace(/^\((.*)\)$/, "$1"), placement };
}

export function stepsFrom(cells, moves, players, first) {
  let board = cells;
  return moves.map((m, i) => {
    const move = { ...describe(m.Description, board), score: m.Score };
    const step = { who: players[(first + i) % 2], move, board };
    if (move.placement) board = applyPlacement(board, move.placement);
    return step;
  });
}

export class Exploration {
  constructor(open, players, me, redraw) {
    this.players = players;
    this.me = me;
    this.redraw = redraw;
    this.position = null;
    this.lines = null;
    this.analyzing = false;
    this.failed = false;
    this.notice = null;
    this.finished = new Map();
    this.seq = 0;
    this.closed = false;
    this.start(open);
  }

  async start(open) {
    this.id = await open().catch(() => null);
    if (this.closed && this.id) return call("exploreClose", this.id);
    if (!this.id) return ((this.failed = true), this.redraw());
    watch(this.id, (value) => value && this.receive(value));
    this.refresh();
  }

  close() {
    this.closed = true;
    if (!this.id) return;
    unwatch(this.id);
    call("exploreClose", this.id).catch(() => {});
  }

  async refresh() {
    const p = await call("exploreState", this.id).catch(() => null);
    if (!p || this.closed) return;
    this.position = this.positionOf(p);
    this.key = this.position.path.map((s) => s.move.text).join("|");
    this.lines = this.finished.get(this.key) ?? null;
    this.analyzing = !this.lines && !p.Over;
    this.redraw();
    if (this.analyzing) this.seq = await call("exploreAnalyze", this.id, BUDGET_MS).catch(() => 0);
  }

  positionOf(p) {
    if (!this.startCells) this.startCells = parseBoard(p.Board);
    return {
      cells: parseBoard(p.Board), racks: p.Racks.map(tokens), scores: p.Scores, onTurn: p.OnTurn, over: p.Over, me: this.me,
      path: stepsFrom(this.startCells, p.Path ?? [], this.players, 0), ending: endingRows(p.Ending, this.players),
    };
  }

  receive(value) {
    if (value.Seq !== this.seq || !this.position) return;
    this.lines = linesOf(value, this.position, this.players);
    this.analyzing = value.Analyzing;
    if (!value.Analyzing) this.finished.set(this.key, this.lines);
    this.redraw();
  }

  async play(input) {
    const error = await call("explorePlay", this.id, input ?? "").catch((e) => e.message);
    if (error) return this.flash(error);
    this.refresh();
  }

  async back(steps = 1) {
    await call("exploreBack", this.id, steps).catch(() => {});
    this.refresh();
  }

  flash(text) {
    this.notice = text;
    this.redraw();
    clearTimeout(this.noticeTimer);
    this.noticeTimer = setTimeout(() => ((this.notice = null), this.redraw()), 3000);
  }
}

function linesOf(value, position, players) {
  const lines = (value.Lines ?? []).map((l) => lineOf(l, position, players));
  return { lines, depth: value.Depth, exact: value.Exact };
}

function lineOf(l, position, players) {
  const mover = position.onTurn;
  const steps = stepsFrom(position.cells, [{ Description: l.Description, Score: l.Score }, ...(l.Steps ?? [])], players, mover);
  const pending = l.Depth === 0;
  return { move: { ...steps[0].move, final: pending ? null : l.Spread }, spread: l.Spread, pending, mover: players[mover], steps, ending: endingRows(l, players) };
}

function endingRows(e, players) {
  if (!e) return [];
  return [{ who: players[0], left: tokens(e.MoverLeft), delta: e.MoverDelta }, { who: players[1], left: tokens(e.OtherLeft), delta: e.OtherDelta }];
}
