import { call, readFile, whenReady } from "./engine.js";
import { load, save } from "./store.js";

const DB = "lexico-records";
const STORE = "games";

function db() {
  return new Promise((resolve, reject) => {
    const request = indexedDB.open(DB, 1);
    request.onupgradeneeded = () => request.result.createObjectStore(STORE, { keyPath: "id" });
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error);
  });
}

async function run(mode, work) {
  const base = await db();
  return new Promise((resolve, reject) => {
    const store = base.transaction(STORE, mode).objectStore(STORE);
    const request = work(store);
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error);
  });
}

export async function keepRecord(path, online = false, watched = false) {
  const text = await readFile(path);
  if (!text) return null;
  const gcg = await readFile(path.replace(/-log\.json$/, ".gcg"));
  const game = await call("reviewOpen", text);
  const { Turns, ...summary } = game;
  const id = String(Date.now());
  await run("readwrite", (s) => s.put({ id, text, gcg, online, watched, finishedAt: Date.now(), summary: { ...summary, turns: Turns.length } }));
  return id;
}

export async function keepMirrorRecord(save, watched = false) {
  await whenReady();
  const match = await call("classicMirror", save, true, false);
  const raw = await call("classicRead", match).catch(() => null);
  call("classicClose", match).catch(() => {});
  return raw?.Result?.RecordPath ? keepRecord(raw.Result.RecordPath, true, watched) : null;
}

export function listRecords() {
  return run("readonly", (s) => s.getAll()).then((all) => all.filter((r) => !r.watched).sort((a, b) => b.finishedAt - a.finishedAt)).catch(() => []);
}

export function getRecord(id) {
  return run("readonly", (s) => s.get(id));
}

export function countAbandoned(mode) {
  const all = load("abandoned", {});
  save("abandoned", { ...all, [mode]: (all[mode] ?? 0) + 1 });
}

export function abandoned(mode) {
  return load("abandoned", {})[mode] ?? 0;
}
