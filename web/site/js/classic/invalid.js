import { h } from "../dom.js";
import { openDialog, actions } from "../dialogs.js";
import { face } from "./rack.js";

export function invalidDialog(move) {
  openDialog((close) => [
    h("h2", {}, "Jugada inválida"),
    h("p", {}, "Una o varias de las palabras formadas no son válidas"),
    formedWords(move.words),
    usedLetters(move.letters),
    actions(h("button.text-btn", { autofocus: true, onclick: close }, "OK")),
  ], { dismissable: false });
}

function formedWords(words) {
  if (!words.length) return null;
  return h("div.invalid-words", {}, words.map((w) => h("span.invalid-word", {}, w)));
}

function usedLetters(letters) {
  if (!letters.length) return null;
  return h("div.stack", {},
    h("p", {}, "Las letras usadas fueron:"),
    h("div.tile-line", {}, letters.map((l) => h("div.rack-tile.line-tile.still-tile", {}, face(l)))),
  );
}
