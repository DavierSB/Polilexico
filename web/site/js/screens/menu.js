import { h } from "../dom.js";
import { go } from "../router.js";
import { screenHead, engineGate } from "../parts.js";
import { homeFrame } from "./home.js";

const MENUS = {
  minijuegos: {
    title: "Minijuegos",
    entries: [["Scrabble Sprint", "sprint"], ["Finales", "finales"], ["¿Cuántas recuerdas?", "recuerdas"], ["Anagrama", "anagrama"]],
  },
  estudiar: {
    title: "Estudiar",
    entries: [["Listas", "listas"], ["Diccionario", "diccionario"], ["Anagramador", "anagramador"], ["Analizador", "analizador"]],
  },
};

export const READY_ROUTES = new Set();
const STUDY_PACK = new Set(["listas", "diccionario"]);

export function menuScreen(name) {
  return (app) => {
    const menu = MENUS[name];
    const gates = [];
    const done = homeFrame(app, screenHead(menu.title), menu.entries.map(([label, route]) => entry(label, route, gates)));
    return () => (done(), gates.forEach((stop) => stop()));
  };
}

function entry(label, route, gates) {
  if (!READY_ROUTES.has(route)) return h("button.btn.block.outlined", { disabled: true }, label, h("span.badge", {}, "Pronto"));
  const button = h("button.btn.block", { onclick: () => go(route) }, label);
  gates.push(engineGate(button, STUDY_PACK.has(route) ? "study" : "engine"));
  return button;
}
