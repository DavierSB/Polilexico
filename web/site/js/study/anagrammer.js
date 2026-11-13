import { h, replace } from "../dom.js";
import { typedTile, softKeyboard } from "./typing.js";
import { screenHead } from "../parts.js";
import { call, whenReady } from "../engine.js";
import { BLANK, LETTERS, blankFace, readWord, tokens } from "../tiles.js";
import { face } from "../classic/rack.js";
import { draggable } from "../classic/drag.js";

const MAX_TILES = 15;
const MAX_BLANKS = 2;
const MIN_TILES = 2;

export function anagrammerScreen(app) {
  const state = { tiles: [], pins: new Map(), found: null, search: 0 };
  const rackCard = h("div.study-card.rack-card");
  const pinCard = h("div.study-card");
  const results = h("div.study-card");
  const ui = { state, rackCard, pinCard, results };
  ui.redraw = () => draw(ui);
  ui.soft = softKeyboard({ add: (l) => add(ui, l), back: () => ui.state.tiles.length && remove(ui, ui.state.tiles.length - 1) });
  rackCard.addEventListener("click", (e) => e.target.closest(".rack-tile") || ui.soft.open());
  const palette = h("div.palette", {}, [...LETTERS, BLANK].map((l) => h("button.rack-tile.palette-key", { onclick: () => add(ui, l) }, keyFace(l))));
  app.append(h("div.screen.study-screen.anagrammer-screen", {},
    screenHead("Anagramador", "estudiar", h("button.text-btn", { onclick: () => clear(ui) }, "Vaciar")),
    h("div.anagrammer-scroll", {}, rackCard, pinCard, results),
    ui.soft.input,
    palette,
  ));
  const onKey = (e) => keys(ui, e);
  document.addEventListener("keydown", onKey);
  ui.redraw();
  return () => document.removeEventListener("keydown", onKey);
}

function keyFace(letter) {
  return letter === BLANK ? h("span.rack-letter.blank-mark", {}, "?") : face(letter);
}

function add(ui, letter) {
  const t = ui.state.tiles;
  if (t.length >= MAX_TILES || (letter === BLANK && t.filter((x) => x === BLANK).length >= MAX_BLANKS)) return;
  t.push(letter);
  changed(ui);
}

function remove(ui, i) {
  ui.state.pins.clear();
  ui.state.tiles.splice(i, 1);
  changed(ui);
}

function clear(ui) {
  ui.state.pins.clear();
  ui.state.tiles = [];
  changed(ui);
}

function pin(ui, slot, tile) {
  for (const [k, v] of ui.state.pins) if (v === tile) ui.state.pins.delete(k);
  ui.state.pins.set(slot, tile);
  ui.redraw();
}

async function changed(ui) {
  const s = ui.state;
  const token = ++s.search;
  s.found = s.tiles.length >= MIN_TILES ? null : undefined;
  ui.redraw();
  if (s.tiles.length < MIN_TILES) return;
  await whenReady();
  const rack = s.tiles.map((t) => (t.length > 1 ? `[${t}]` : t)).join("");
  const words = await call("anagramTiles", rack).then(tokens, () => []);
  if (token !== s.search) return;
  s.found = words.map((w) => readWord(w).filter(Boolean));
  ui.redraw();
}

function draw(ui) {
  const s = ui.state;
  replace(ui.rackCard, s.tiles.length ? h("div.tile-flow", {}, s.tiles.map((l, i) => rackKey(ui, l, i))) : h("span.muted", {}, "Atril vacío"));
  ui.pinCard.classList.toggle("hidden", !s.tiles.length);
  replace(ui.pinCard, h("b.muted", {}, "Letras fijadas"), h("div.tile-flow", {}, s.tiles.map((_, slot) => pinSlot(ui, slot))));
  ui.results.classList.toggle("hidden", s.tiles.length < MIN_TILES);
  replace(ui.results, results(s));
}

function rackKey(ui, letter, i) {
  const pinned = [...ui.state.pins.values()].includes(i);
  const el = h("button", { class: "rack-tile palette-key" + (pinned ? " used" : ""), onclick: () => remove(ui, i) }, keyFace(letter));
  draggable(el, { ghost: () => h("div.rack-tile.palette-key", {}, keyFace(letter)), drop: (t) => t.pin && pin(ui, Number(t.pin.dataset.slot), i) });
  return el;
}

function pinSlot(ui, slot) {
  const tile = ui.state.pins.get(slot);
  if (tile === undefined) return h("div.pin-slot.empty-pin", { "data-slot": slot });
  return h("button.pin-slot.rack-tile.palette-key", { "data-slot": slot, onclick: () => (ui.state.pins.delete(slot), ui.redraw()) }, keyFace(ui.state.tiles[tile]));
}

function results(s) {
  if (s.found === null) return h("span.muted", {}, "Buscando…");
  if (!s.found?.length) return h("b.muted", {}, "Sin anagramas");
  const pins = [...s.pins].map(([slot, tile]) => [slot, s.tiles[tile]]);
  const shown = s.found.filter((w) => pins.every(([slot, l]) => w[slot] && (l === BLANK ? w[slot].blank : w[slot].letter === l && !w[slot].blank)));
  if (!shown.length) return h("b.muted", {}, "Ninguno con esas letras fijadas");
  return [
    h("div.row.count-line", {}, h("b.count", {}, shown.length), h("span.muted", {}, shown.length === 1 ? "anagrama" : "anagramas")),
    h("div.word-flow", {}, shown.map(wordText)),
  ];
}

export function wordText(tiles) {
  return h("span.found-word", {}, tiles.map((t) => (t.blank ? h("span.blank-letter", {}, blankFace(t)) : t.letter)));
}

function keys(ui, e) {
  if (e.ctrlKey || e.metaKey || e.altKey || document.querySelector(".scrim") || e.target === ui.soft.input) return;
  if (e.key === "Backspace" && ui.state.tiles.length) return remove(ui, ui.state.tiles.length - 1);
  const letter = typedTile(e);
  if (letter) add(ui, letter);
}
