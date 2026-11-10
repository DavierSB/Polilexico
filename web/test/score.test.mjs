import path from "node:path";
import { call, exit, write, argv, JS, rackText, bestInput } from "./harness.mjs";

const { placementScore } = await import(path.join(JS, "score.js"));
const { parsePlacement, writePlacement, placementText } = await import(path.join(JS, "placement.js"));
const { parseBoard, tokens, LETTERS } = await import(path.join(JS, "tiles.js"));

const GAMES = Number(argv[2] ?? 5);
const fails = [];
let best = 0;
let random = 0;

const pick = (list) => list[Math.floor(Math.random() * list.length)];

async function checkBest(board, rack) {
  const cells = parseBoard(board);
  for (const c of await call("bestMoves", board, rackText(rack, tokens), 40)) {
    if (c.description.startsWith("(")) continue;
    const text = c.description.replace(/ \(.*/, "");
    best++;
    const js = placementScore(cells, parsePlacement(text));
    if (js !== c.score) fails.push({ board, text, js, engine: c.score });
  }
}

function randomPending(cells, pool) {
  const pending = new Map();
  const vertical = Math.random() < 0.5;
  const start = Math.floor(Math.random() * 225);
  const count = 1 + Math.floor(Math.random() * 7);
  for (let sq = start; pending.size < count && sq < 225 && (vertical || Math.floor(sq / 15) === Math.floor(start / 15)); sq += vertical ? 15 : 1) {
    if (cells[sq]) continue;
    const letter = pick(pool);
    pending.set(sq, { letter: letter === "?" ? pick(LETTERS) : letter, blank: letter === "?" || Math.random() < 0.08 });
  }
  return pending;
}

async function checkRandom(board, pool) {
  const cells = parseBoard(board);
  for (let k = 0; k < 25; k++) {
    const p = writePlacement(cells, randomPending(cells, pool));
    if (!p) continue;
    const engine = await call("placementScore", board, placementText(p)).catch(() => null);
    if (engine === null) continue;
    random++;
    const js = placementScore(cells, p);
    if (js !== engine) fails.push({ board, text: placementText(p), js, engine });
  }
}

for (let g = 0; g < GAMES; g++) {
  const m = await call("classicNew", "HastyBot", 1, 0, 0, false);
  for (let turn = 0; turn < 60; turn++) {
    const s = await call("classicRead", m);
    if (s.Status.Over) break;
    if (!s.Status.HumanToMove) {
      await new Promise((r) => setTimeout(r, 200));
      continue;
    }
    await checkBest(s.Board, s.Rack);
    await checkRandom(s.Board, tokens(s.Unseen).concat(tokens(s.Rack)));
    await call("classicPlay", m, await bestInput(s.Board, s.Rack, tokens)).catch(() => call("classicPlay", m, "pasar"));
  }
  await call("classicClose", m);
  write(`partida ${g + 1}: ${best} mejores, ${random} al azar, ${fails.length} fallos\n`);
}
if (fails.length) console.log(JSON.stringify(fails.slice(0, 5), null, 1));
exit(fails.length ? 1 : 0);
