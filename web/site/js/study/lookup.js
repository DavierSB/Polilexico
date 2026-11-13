import { h } from "../dom.js";
import { openDialog } from "../dialogs.js";
import { icon } from "../icons.js";
import { SIZE } from "../tiles.js";
import { studyData } from "./data.js";
import { keyOf, upper, wordEntry, entry } from "./keys.js";

export function wordsAt(cells, i) {
  if (!cells[i]) return [];
  return [[0, 1], [1, 0]].map(([dr, dc]) => wordThrough(cells, i, dr, dc)).filter((w) => w.length >= 2).map((tiles) => keyOf(tiles));
}

function wordThrough(cells, i, dr, dc) {
  const filled = (r, c) => r >= 0 && r < SIZE && c >= 0 && c < SIZE && cells[r * SIZE + c];
  let r = Math.floor(i / SIZE), c = i % SIZE;
  while (filled(r - dr, c - dc)) (r -= dr), (c -= dc);
  const tiles = [];
  for (; filled(r, c); r += dr, c += dc) tiles.push(cells[r * SIZE + c].letter);
  return tiles;
}

export function lookUp(cells, i) {
  const words = wordsAt(cells, i);
  if (!words.length) return;
  const anchor = [...document.querySelectorAll(`.square[data-index="${i}"]`)].at(-1);
  wordMenu(words, anchor);
}

export function lookUpWord(word) {
  openWord(entry(word));
}

function wordMenu(words, anchor) {
  const box = anchor?.getBoundingClientRect() ?? { left: innerWidth / 2, bottom: innerHeight / 2 };
  const menu = h("div.word-menu", { role: "menu" }, words.map((w) => h("button.word-menu-item", { role: "menuitem", onclick: () => (close(), openWord(wordEntry(w))) }, icon("lookup"), `Ver ${upper(w)} en el diccionario`)));
  const scrim = h("div.menu-scrim", {}, menu);
  const close = () => scrim.remove();
  scrim.addEventListener("click", (e) => e.target === scrim && close());
  document.getElementById("layer").append(scrim);
  const width = menu.offsetWidth, height = menu.offsetHeight;
  menu.style.left = Math.max(8, Math.min(box.left, innerWidth - width - 8)) + "px";
  menu.style.top = (box.bottom + height + 8 > innerHeight ? Math.max(8, box.top - height) : box.bottom) + "px";
}

export async function openWord(entry) {
  const [{ lexicon }, { wordCard, looseCard }] = await Promise.all([studyData(), import("./word.js")]);
  let close = null;
  const body = h("div.word-dialog-body");
  const show = (e) => {
    const unit = lexicon.resolve(e);
    body.replaceChildren(unit ? wordCard(unit, lexicon, show) : looseCard(e.replace(/^\w:/, ""), lexicon));
    head.replaceChildren(h("button.icon-btn", { "aria-label": "Volver", onclick: () => close() }, icon("back")), h("h1", {}, upper(unit?.head ?? e.replace(/^\w:/, ""))));
  };
  const head = h("header.screen-head");
  close = openDialog(() => [head, body], { wide: true, full: true });
  show(entry);
}
