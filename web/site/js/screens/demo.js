import { createBoard } from "../board.js";
import { call, readyLater } from "../engine.js";
import { applyPlacement, parsePlacement, placedSquares } from "../placement.js";
import { openAt } from "../study/lookup.js";
import { emptyBoard } from "../tiles.js";

const PAUSE_MS = 1600;
const FINAL_PAUSE_MS = 6000;

const shared = { board: null, cells: emptyBoard(), users: 0, looping: false };

export function startDemo() {
  shared.board ??= createDemoBoard();
  shared.users++;
  if (!shared.looping) readyLater.then(() => loop(shared.board, () => shared.users > 0));
  shared.looping = true;
  let stopped = false;
  return { el: shared.board.el, stop: () => stopped || ((stopped = true), shared.users--) };
}

function createDemoBoard() {
  const board = createBoard({ onSquare: (i) => openAt(shared.cells, i) });
  board.render({ cells: shared.cells });
  return board;
}

async function loop(board, running) {
  let next = call("demo");
  while (running()) {
    const game = await next.catch(() => []);
    next = call("demo");
    await replay(board, game, running);
  }
  shared.looping = false;
}

async function replay(board, game, running) {
  show(board, emptyBoard());
  for (const text of game) {
    await wait(PAUSE_MS);
    if (!running()) return;
    const p = parsePlacement(text);
    if (!p) continue;
    show(board, applyPlacement(shared.cells, p), new Set(placedSquares(p)));
  }
  await wait(FINAL_PAUSE_MS);
}

function show(board, cells, latest) {
  shared.cells = cells;
  board.render({ cells, latest });
}

function wait(ms) {
  return new Promise((r) => setTimeout(r, ms));
}
