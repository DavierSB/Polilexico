import path from "node:path";
import { call, exit, write, JS } from "./harness.mjs";

const { reviewPlayers } = await import(path.join(JS, "games", "reviewFaces.js"));

const turns = `"turns":[{"turn_num":1,"player_name":"Ana","rack":"AANNYCH","gen_list":[],"actual_play":{"coords":"H8","description":"H8 NANAY (24 pts)","score":24}}]`;
const log = (extra) => `{"started_at":"2026-11-17T10:00:00","human_name":"Ana","bot_name":"Beto","final_scores":[24,0],"winner":"Ana","mode":"local",${extra}${turns}}`;
const identified = log(`"countries":["CU","ES"],"ids":["a-1","b-2"],`);
const fails = [];

function check(label, got, want) {
  if (JSON.stringify(got) !== JSON.stringify(want)) fails.push(`${label}: ${JSON.stringify(got)} != ${JSON.stringify(want)}`);
}

const players = async (text, record) => {
  const game = await call("reviewOpen", text);
  const { me, faces } = reviewPlayers(game, record, game.Opponent);
  return { me, faces: Object.fromEntries(faces) };
};

const game = await call("reviewOpen", identified);
check("identidad", [game.MeCountry, game.MeID, game.OpponentCountry, game.OpponentID], ["CU", "a-1", "ES", "b-2"]);
check("observada", await players(identified, { watched: true, online: true }), { me: "Ana", faces: { Ana: { country: "CU", id: "a-1" }, Beto: { country: "ES", id: "b-2" } } });
check("en línea", await players(identified, { online: true }), { me: "Tú", faces: { Beto: { country: "ES", id: "b-2" } } });
check("local", await players(log(""), {}), { me: "Ana", faces: {} });
check("vieja en línea", await players(log(""), { online: true }), { me: "Tú", faces: { Beto: { country: "", id: "" } } });

if (fails.length) {
  write(fails.join("\n") + "\n");
  exit(1);
}
write("reviewFaces: ok\n");
exit(0);
