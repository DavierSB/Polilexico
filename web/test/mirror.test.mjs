import path from "node:path";
import { call, exit, write, argv, JS, rackText, bestInput } from "./harness.mjs";

const { readSave, parseSave } = await import(path.join(JS, "online", "mirror.js"));
const { normalize } = await import(path.join(JS, "classic", "state.js"));
const { tokens, fullBag, BLANK } = await import(path.join(JS, "tiles.js"));

const GAMES = Number(argv[2] ?? 6);
const fails = [];
let views = 0;

const sorted = (list) => [...list].sort().join(" ");

function summary(s) {
  return {
    board: s.boardText, rack: s.rack.join(" "), myTiles: s.myTiles, opponentTiles: s.opponentTiles, myScore: s.myScore,
    opponentScore: s.opponentScore, myTurn: s.myTurn, bag: s.bag, over: s.over, opponent: s.opponent, host: s.host,
    moves: JSON.stringify(s.moves.map((m) => [m.byMe, m.kind, m.coords, m.tiles, m.score, m.myTotal, m.opponentTotal])),
    result: JSON.stringify(s.result), running: s.clocks?.running ?? null,
    claimable: s.claimable, finalPass: s.finalPass, claim: JSON.stringify(s.claim),
  };
}

async function compare(m, first, label) {
  const view = await call("classicView", m, first);
  const mirror = await call("classicMirror", view, false, false);
  const engine = normalize(await call("classicRead", mirror), !first);
  await call("classicClose", mirror);
  const js = normalize(readSave(parseSave(view), !first, 0), !first);
  const [a, b] = [summary(engine), summary(js)];
  views++;
  for (const k of Object.keys(a)) if (a[k] !== b[k]) fails.push({ label, first, k, engine: a[k], js: b[k] });
  if (engine.clocks && Math.abs(engine.clocks.myMs - js.clocks.myMs) > 2000) fails.push({ label, first, k: "clocks" });
  if (sorted(js.unseen) !== sorted(unseenOf(js))) fails.push({ label, first, k: "unseen", js: sorted(js.unseen) });
}

function unseenOf(s) {
  const left = fullBag();
  const take = (t) => left.splice(left.indexOf(t), 1);
  s.cells.forEach((t) => t && take(t.blank ? BLANK : t.letter));
  s.rack.forEach(take);
  return left;
}

async function nextInput(s) {
  const rack = s.Status.HumanToMove ? s.Rack : s.OpponentRack;
  const r = Math.random();
  if (s.FinalPass) return r < 0.5 ? "reclamar" : "pasar";
  if (s.Claimable && r < 0.3) return "reclamar";
  if (r < 0.2) return phony(await bestInput(s.Board, rack, tokens));
  if (r < 0.06 && s.Status.BagCount >= 7) return "cambiar " + rackText(rack, tokens).slice(0, 2);
  if (r < 0.1) return "pasar";
  if (r < 0.14) return "H8 ZZZQ";
  return bestInput(s.Board, rack, tokens);
}

function phony(input) {
  const [coords, word] = input.split(" ");
  return word ? `${coords} ${word.match(/\[[^\]]+\]|./g).reverse().join("")}` : input;
}

for (let g = 0; g < GAMES; g++) {
  const timed = g % 2 === 1;
  const m = await call("classicNewLocal", "Ana", "Beto", 0, timed ? 1500000 : 0, timed ? 600000 : 0, g % 3 === 0);
  if (g % 3 === 0) await call("classicClaims", m, true);
  for (let turn = 0; turn < 80; turn++) {
    await compare(m, true, `p${g}t${turn}`);
    await compare(m, false, `p${g}t${turn}`);
    const s = await call("classicRead", m);
    if (s.Status.Over) break;
    await call("classicPlay", m, await nextInput(s)).catch(() => call("classicPlay", m, "pasar").catch(() => {}));
  }
  await call("classicClose", m);
  write(`partida ${g + 1}: ${views} vistas, ${fails.length} fallos\n`);
}
if (fails.length) console.log(JSON.stringify(fails.slice(0, 5), null, 1));
exit(fails.length ? 1 : 0);
