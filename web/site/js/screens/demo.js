import { createBoard } from "../board.js";
import { call, readyLater } from "../engine.js";
import { applyPlacement, parsePlacement, placedSquares } from "../placement.js";
import { emptyBoard } from "../tiles.js";

const PAUSE_MS = 1600;
const FINAL_PAUSE_MS = 6000;

export function startDemo() {
  const board = createBoard();
  let running = true;
  board.render({ cells: emptyBoard() });
  readyLater.then(() => loop(board, () => running));
  return { el: board.el, stop: () => (running = false) };
}

async function loop(board, running) {
  let next = call("demo");
  while (running()) {
    const game = await next.catch(() => []);
    next = call("demo");
    await replay(board, game, running);
  }
}

async function replay(board, game, running) {
  let cells = emptyBoard();
  board.render({ cells });
  for (const text of game) {
    await wait(PAUSE_MS);
    if (!running()) return;
    const p = parsePlacement(text);
    if (!p) continue;
    cells = applyPlacement(cells, p);
    board.render({ cells, latest: new Set(placedSquares(p)) });
  }
  await wait(FINAL_PAUSE_MS);
}

function wait(ms) {
  return new Promise((r) => setTimeout(r, ms));
}
