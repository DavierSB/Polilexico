import { call, whenReady } from "./engine.js";
import { load, save } from "./store.js";
import { countAbandoned } from "./records.js";

const KEY = "savedGames";
const PROGRESS = { classic: "classicProgress", endgame: "classicProgress", duplicate: "duplicateProgress" };
const LEGACY = { classicGame: (setup) => (setup?.endgame ? "endgame" : "classic"), duplicateGame: () => "duplicate" };

let migrated = false;

export function listSaved(mode) {
  return Object.entries(all())
    .map(([id, game]) => ({ id, ...game }))
    .filter((game) => game.mode === mode)
    .sort((a, b) => b.updatedAt - a.updatedAt);
}

export function countSaved(mode) {
  return listSaved(mode).length;
}

export function readSaved(id) {
  const game = all()[id];
  return game ? { id, ...game } : null;
}

export function writeSaved(id, mode, text, setup) {
  save(KEY, { ...all(), [id]: { mode, text, setup, updatedAt: Date.now() } });
}

export function deleteSaved(id) {
  const { [id]: gone, ...rest } = all();
  if (gone) save(KEY, rest);
}

export function abandonSaved(id) {
  const game = readSaved(id);
  if (game) countAbandoned(game.mode);
  deleteSaved(id);
}

export function newSaveId() {
  return crypto.randomUUID?.() ?? `${Date.now().toString(36)}-${Math.random().toString(36).slice(2)}`;
}

export async function savedProgress(game) {
  await whenReady();
  return call(PROGRESS[game.mode], game.text).catch(() => null);
}

function all() {
  if (!migrated) migrate();
  return load(KEY, {});
}

function migrate() {
  migrated = true;
  Object.entries(LEGACY).forEach(([key, modeOf]) => adoptLegacy(key, modeOf));
}

function adoptLegacy(key, modeOf) {
  const old = load(key, null);
  if (!old?.text) return;
  writeSaved(newSaveId(), modeOf(old.setup), old.text, old.setup);
  save(key, null);
}
