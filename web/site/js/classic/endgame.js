import { h, replace } from "../dom.js";
import { go } from "../router.js";
import { screenHead, parseDuration, engineGate } from "../parts.js";
import { load, save } from "../store.js";
import { openDialog, actions, toast } from "../dialogs.js";
import { challengeSection, timeSection, rulesButton } from "./options.js";
import { startEndgame } from "./session.js";

const RULES = [
  ["Tomarás el control de una partida Clásica en sus compases finales, para que intentes ganarla."],
  ["Puedes ajustar ciertas características del final de la partida, como el número de fichas que quedan en la bolsa (por defecto, entre 2 y 8), el margen de diferencia entre tú y tu rival (por defecto, vas perdiendo por entre 0 y 40 puntos) o dónde está la Q (por defecto, puede o no haberse jugado)."],
];
const Q_PLACES = [
  ["anywhere", "Puede o no haberse jugado", "puede o no haberse jugado"],
  ["unplayed", "No se ha jugado", "no se ha jugado"],
  ["played", "Ya se jugó", "ya se jugó"],
  ["yours", "Está en tu atril", "está en tu atril"],
  ["hidden", "No se ha jugado y no está en tu atril", "no se ha jugado y no está en tu atril"],
];
const LEAD_LIMIT = 200;
const DEFAULTS = { minBag: 2, maxBag: 8, minLead: -40, maxLead: 0, q: "anywhere", single: true, timed: false, time: "5:00", overtime: "1:00" };

export function endgameSetupScreen(app) {
  const form = { ...DEFAULTS, ...load("endgameForm", {}) };
  const body = h("div.stack.setup-narrow");
  const gates = [];
  const redraw = () => {
    gates.splice(0).forEach((stop) => stop());
    replace(body, fields(form, redraw, gates));
  };
  redraw();
  app.append(h("div.screen", {}, screenHead("Finales", "minijuegos"), body));
  return () => gates.forEach((stop) => stop());
}

function fields(form, redraw, gates) {
  const start = h("button.btn.block.start", { onclick: () => begin(form, start) }, "Empezar");
  gates.push(engineGate(start));
  return [
    rulesButton("Reglas de Finales", RULES),
    h("div.stack.app-settings", {},
      h("b.app-settings-title", {}, "Ajustes"),
      h("span.muted.small", {}, "Toca un ajuste para cambiarlo"),
      settingRow(bagText(form), bagHighlight(form), () => rangeDialog("Fichas en la bolsa", "Cuántas fichas quedan en la bolsa cuando tomas el control de la partida.", form, "Bag", 0, 20, 1, String, ["Mínimo", "Máximo"], redraw)),
      settingRow(leadText(form), leadHighlight(form), () => rangeDialog("Tu ventaja al empezar", "Tus puntos menos los de Gitana al empezar: negativa si vas perdiendo.", form, "Lead", -LEAD_LIMIT, LEAD_LIMIT, 5, signed, ["Mínima", "Máxima"], redraw)),
      settingRow(`La Q ${qSummary(form)}`, qSummary(form), () => qDialog(form, redraw)),
    ),
    challengeSection(form, redraw),
    timeSection(form, redraw),
    start,
  ];
}

function settingRow(text, highlight, open) {
  const at = highlight ? text.indexOf(highlight) : -1;
  const parts = at < 0 ? [text] : [text.slice(0, at), h("b.setting-strong", {}, highlight), text.slice(at + highlight.length)];
  return h("button.app-setting", { onclick: open }, h("span.setting-dot"), h("span.grow", {}, parts));
}

function qSummary(form) {
  return Q_PLACES.find(([k]) => k === form.q)[2];
}

function bagHighlight({ minBag: min, maxBag: max }) {
  if (min !== max) return `${min} y ${max} fichas`;
  return max === 0 ? "bolsa vacía" : max === 1 ? "1 ficha" : `${max} fichas`;
}

function leadHighlight({ minLead: min, maxLead: max }) {
  const range = (a, b) => (a === b ? `${a}` : `entre ${a} y ${b}`);
  if (max <= 0) return `${range(-max, -min)} puntos`;
  if (min >= 0) return `${range(min, max)} puntos`;
  return null;
}

function bagText({ minBag: min, maxBag: max }) {
  if (min !== max) return `Entre ${min} y ${max} fichas en la bolsa`;
  return max === 0 ? "Con la bolsa vacía" : `${max === 1 ? "1 ficha" : `${max} fichas`} en la bolsa`;
}

function leadText({ minLead: min, maxLead: max }) {
  const range = (a, b) => (a === b ? `${a}` : `entre ${a} y ${b}`);
  if (max <= 0) return `Vas perdiendo por ${range(-max, -min)} puntos`;
  if (min >= 0) return `Vas ganando por ${range(min, max)} puntos`;
  return `Entre ir perdiendo por ${-min} y ganando por ${max} puntos`;
}

function signed(n) {
  return n > 0 ? `+${n}` : String(n);
}

function rangeDialog(title, text, form, key, lo, hi, step, label, names, redraw) {
  const draft = { min: form["min" + key], max: form["max" + key] };
  const lowText = h("b.range-label"), highText = h("b.range-label");
  const shown = h("div.row.range-labels", {}, lowText, highText);
  const show = () => ((lowText.textContent = `${names[0]}: ${label(draft.min)}`), (highText.textContent = `${names[1]}: ${label(draft.max)}`));
  const slider = (side) => h("input.range", {
    type: "range", min: lo, max: hi, step, value: draft[side], "aria-label": side === "min" ? "Mínimo" : "Máximo",
    oninput: (e) => {
      draft[side] = Number(e.target.value);
      if (draft.min > draft.max) draft[side === "min" ? "max" : "min"] = draft[side];
      e.target.parentElement.querySelectorAll("input").forEach((i, n) => (i.value = n ? draft.max : draft.min));
      show();
    },
  });
  show();
  openDialog((close) => [
    h("h2", {}, title), h("p.muted", {}, text), shown,
    h("div.dual-range", {}, slider("min"), slider("max")),
    h("div.row.range-scale", {}, [lo, (lo + hi) / 2, hi].map((v) => h("span", {}, label(v)))),
    actions(h("button.text-btn", { onclick: close }, "Cancelar"), h("button.text-btn", { onclick: () => ((form["min" + key] = draft.min), (form["max" + key] = draft.max), close(), redraw()) }, "Aceptar")),
  ]);
}

function qDialog(form, redraw) {
  let draft = form.q;
  openDialog((close) => [
    h("h2", {}, "La Q"), h("p.muted", {}, "Dónde está la Q cuando tomas el control de la partida."),
    h("div.stack", {}, Q_PLACES.map(([k, option]) => h("label.row.theme-choice", {}, h("input", { type: "radio", name: "q", checked: k === draft, onchange: () => (draft = k) }), option))),
    actions(h("button.text-btn", { onclick: close }, "Cancelar"), h("button.text-btn", { onclick: () => ((form.q = draft), close(), redraw()) }, "Aceptar")),
  ]);
}

async function begin(form, button) {
  const timeMs = form.timed ? parseDuration(form.time) : 0;
  const overtimeMs = form.timed ? parseDuration(form.overtime) : 0;
  if (form.timed && (!timeMs || overtimeMs === null)) return toast("Revisa los tiempos.");
  save("endgameForm", form);
  button.disabled = true;
  button.firstChild.textContent = "Buscando un final…";
  const endgame = { minBag: form.minBag, maxBag: form.maxBag, minLead: form.minLead, maxLead: form.maxLead, q: form.q };
  try {
    const session = await startEndgame({ bot: "HastyBot", starter: 0, single: form.single, timeMs, overtimeMs, endgame });
    go("partida/" + session.id);
  } catch (e) {
    toast(e.message);
    button.disabled = false;
    button.firstChild.textContent = "Empezar";
  }
}
