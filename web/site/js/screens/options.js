import { h } from "../dom.js";
import { openDialog, actions } from "../dialogs.js";
import { setting, setSetting } from "../settings.js";
import { soundsOn, setSounds } from "../sound.js";
import { infoButton } from "../classic/options.js";

const OPTIONS = [
  ["liveScore", "Contar los puntos al colocar", "Mientras pones fichas, el tablero muestra los puntos que valdría la jugada."],
  ["showUnseen", "Mostrar las letras faltantes", "En la Clásica, al tocar la bolsa se ven las fichas que faltan por salir, no solo cuántas son."],
  ["sounds", "Sonidos"],
  ["hidden", "No aparecer en línea"],
  ["noChallenges", "No recibir desafíos"],
];

export function optionsDialog() {
  openDialog((close) => [
    h("h2", {}, "Opciones"),
    h("div.stack", {}, OPTIONS.map(([name, title, hint]) => optionRow(name, title, hint))),
    actions(h("button.text-btn", { onclick: close }, "Cerrar")),
  ]);
}

function optionRow(name, title, hint) {
  return h("div.row.theme-choice", {},
    h("span.row.grow", {}, h("span", {}, title), hint ? infoButton(title, hint) : null),
    h("input.switch", { type: "checkbox", "aria-label": title, checked: current(name), onchange: (e) => change(name, e.target.checked) }),
  );
}

function current(name) {
  return name === "sounds" ? soundsOn() : setting(name);
}

function change(name, on) {
  if (name === "sounds") return setSounds(on);
  setSetting(name, on);
}
