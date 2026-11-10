import fs from "node:fs";
import vm from "node:vm";
import path from "node:path";
import { fileURLToPath } from "node:url";

const here = path.dirname(fileURLToPath(import.meta.url));
export const SITE = path.join(here, "..", "build", "site");
export const JS = path.join(here, "..", "site", "js");
const ENGINE = path.join(SITE, "engine");
const nodeProcess = process;

export const argv = nodeProcess.argv;
export const exit = (code) => nodeProcess.exit(code);
export const write = (text) => nodeProcess.stdout.write(text);

let handler = () => {};
let lastId = 0;
const pending = new Map();

globalThis.location = { href: "file:///engine/worker.js" };
globalThis.postMessage = (m) => handler(m);
globalThis.importScripts = (...names) => names.forEach((n) => vm.runInThisContext(fs.readFileSync(path.join(ENGINE, n), "utf8")));
globalThis.fetch = async (url) => new Response(fs.readFileSync(path.join(ENGINE, new URL(url, location.href).pathname.replace(/^\/engine\//, ""))));
delete globalThis.process;
vm.runInThisContext(fs.readFileSync(path.join(ENGINE, "worker.js"), "utf8"));
handler = (m) => m.id && settle(m);

function settle(m) {
  pending.get(m.id)(m);
  pending.delete(m.id);
}

export function call(name, ...args) {
  return new Promise((resolve, reject) => {
    const id = ++lastId;
    pending.set(id, (m) => (m.ok ? resolve(m.value) : reject(new Error(m.value))));
    globalThis.onmessage({ data: { id, name, args } });
  });
}

export function rackText(rack, tokens) {
  return tokens(rack).map((t) => (t.length > 1 ? `[${t}]` : t)).join("");
}

export async function bestInput(board, rack, tokens) {
  const found = await call("bestMoves", board, rackText(rack, tokens), 1);
  const d = found[0]?.description ?? "(pasar)";
  return d.startsWith("(") ? "pasar" : d.replace(/ \(.*/, "");
}

await call("init", "/data", "/saves");
