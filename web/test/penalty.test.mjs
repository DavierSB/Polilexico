import path from "node:path";
import { call, exit, JS } from "./harness.mjs";

const { readSave, parseSave } = await import(path.join(JS, "online", "mirror.js"));
const { normalize } = await import(path.join(JS, "classic", "state.js"));

const TIME_MS = 120000;
const OFFSETS = [-5000, 0, 1, 999, 1000, 1001, 59000, 59999, 60000, 60001, 61000, 119999, 120000, 120001, 180500, 299999, 300000, 300001, 400000];

const m = await call("classicNewLocal", "Ana", "Beto", 1, TIME_MS, 300000, false);
await call("classicPlay", m, "pasar");
const base = JSON.parse(await call("classicView", m, true));
const fails = [];
for (const offset of OFFSETS) {
  for (const side of ["HumanSpentMs", "BotSpentMs"]) {
    const text = JSON.stringify({ ...base, HumanSpentMs: 0, BotSpentMs: 0, [side]: TIME_MS + offset });
    const mirror = await call("classicMirror", text, true, false);
    const engine = normalize(await call("classicRead", mirror), false);
    await call("classicClose", mirror);
    const js = normalize(readSave(parseSave(text), false, 0), false);
    if (engine.myScore !== js.myScore || engine.opponentScore !== js.opponentScore) fails.push({ side, offset, engine: [engine.myScore, engine.opponentScore], js: [js.myScore, js.opponentScore] });
  }
}
console.log(`${OFFSETS.length * 2} casos de descuento, ${fails.length} fallos`);
if (fails.length) console.log(JSON.stringify(fails));
exit(fails.length ? 1 : 0);
