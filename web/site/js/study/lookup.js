import { h } from "../dom.js";
import { openDialog, actions } from "../dialogs.js";
import { SIZE } from "../tiles.js";
import { studyData } from "./data.js";
import { keyOf, upper, show } from "./keys.js";
import { summary, sourceLabel } from "./dictionary.js";

const RAE_URL = "https://dle.rae.es/";
const STUDY_INFO = "Con el Modo Estudio activado, durante la partida se puede tocar una palabra ya jugada en el tablero para ver su significado en el diccionario.";

export const STUDY_TITLE = "Modo Estudio";
export { STUDY_INFO };

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

export async function lookUp(cells, i) {
  const words = wordsAt(cells, i);
  if (!words.length) return;
  const { dictionary } = await studyData();
  openDialog((close) => [
    ...words.map((w) => wordBlock(w, dictionary.meanings(w))),
    actions(h("button.text-btn", { onclick: close }, "Cerrar")),
  ], { wide: true });
}

function wordBlock(word, meanings) {
  const sources = [...new Set(meanings.flatMap((m) => m.senses.map((s) => sourceLabel(s.source))))];
  return h("div.stack.lookup-word", {},
    h("h2", {}, upper(word)),
    meanings.length
      ? [meanings.slice(0, 2).map((m) => h("div.stack", {}, m.relation ? h("b.small", {}, m.relation) : null, m.senses.slice(0, 3).map((s, n) => h("p.sense", {}, h("b.sense-n", {}, `${n + 1}. `), s.text)))), h("span.muted.small", {}, "Fuente: " + sources.join(", "))]
      : h("p.muted", {}, summary(meanings) ?? "No está en el diccionario sin conexión."),
    h("a.text-btn", { href: RAE_URL + encodeURIComponent(show(word)), target: "_blank", rel: "noopener" }, "Consultar en la RAE"),
  );
}
