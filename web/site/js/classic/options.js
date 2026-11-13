import { h } from "../dom.js";
import { openDialog, actions } from "../dialogs.js";
import { icon } from "../icons.js";

const TIME_INFO = "Cuando se te acaba el tiempo empieza el descuento. Por cada minuto de descuento que empieces se te restan 10 puntos; si agotas también el descuento, pierdes por tiempo.";
const CHALLENGE_INTRO = "Cómo se tratan las jugadas con palabras que no existen.";
const VOID_HINT = "si intentas colocar una jugada inválida, no se te permite y continúas tu turno.";
const SINGLE_HINT = "si colocas una jugada inválida, obtienes 0 puntos, vuelven las fichas a tu atril y pierdes el turno.";
export const ONLINE_SINGLE_HINT = "una jugada inválida cuenta, salvo que el rival la reclame antes de jugar. Reclamar no tiene castigo.";

export function choiceRow(labels, selected, choose) {
  return h("div.chips", {}, labels.map((label, i) =>
    h("button", { class: "chip" + (i === selected ? " on" : ""), "aria-pressed": i === selected, onclick: () => choose(i) }, label),
  ));
}

export function sectionTitle(text, info) {
  return h("div.row.section-title", {}, text, info ? infoButton(text, info) : null);
}

export function infoButton(title, text) {
  const open = () => openDialog((close) => [
    h("h2", {}, title),
    text.split("\n\n").map((p) => h("p", {}, p)),
    actions(h("button.text-btn", { onclick: close }, "Entendido")),
  ]);
  return h("button.info-btn", { "aria-label": "Más información", onclick: open }, "i");
}

export function challengeSection(form, redraw, voidOnlyBot = null, singleHint = SINGLE_HINT) {
  if (voidOnlyBot) {
    return [sectionTitle("Comprobación de jugadas", `${voidOnlyBot} solo juega en modo void: una jugada con palabras no válidas se rechaza y puedes volver a intentarlo.`), h("p", {}, "Void")];
  }
  const info = [CHALLENGE_INTRO, "Void: " + VOID_HINT, "Single: " + singleHint].join("\n\n");
  return [
    sectionTitle("Comprobación de jugadas", info),
    choiceRow(["Void", "Single"], form.single ? 1 : 0, (i) => ((form.single = i === 1), redraw())),
  ];
}

export function timeSection(form, redraw) {
  return [
    sectionTitle("Tiempo", TIME_INFO),
    choiceRow(["Con tiempo", "Sin tiempo"], form.timed ? 0 : 1, (i) => ((form.timed = i === 0), redraw())),
    form.timed ? timeFields(form) : null,
  ];
}

function timeFields(form) {
  return h("div.time-fields", {},
    timeField("Tiempo por jugador (min o min:seg)", form.time, (v) => (form.time = v)),
    timeField("Tiempo de descuento (min o min:seg)", form.overtime, (v) => (form.overtime = v)),
  );
}

function timeField(label, value, change) {
  const id = "f" + Math.random().toString(36).slice(2);
  return h("div.field", {},
    h("label", { for: id }, label),
    h("input", { id, value, inputmode: "numeric", oninput: (e) => change(e.target.value) }),
  );
}

export function stepper(value, min, max, change) {
  return h("div.row.stepper", {},
    h("button.btn.small.outlined", { disabled: value <= min, "aria-label": "Menos", onclick: () => change(value - 1) }, "−"),
    h("span.stepper-value", {}, value),
    h("button.btn.small.outlined", { disabled: value >= max, "aria-label": "Más", onclick: () => change(value + 1) }, "+"),
  );
}

export function rulesButton(title, sections) {
  return h("button.text-btn.rules-btn", { onclick: () => rulesDialog(title, sections) }, "Leer reglas");
}

export function rulesIconButton(title, sections) {
  return h("button.icon-btn", { "aria-label": "Reglas", onclick: () => rulesDialog(title, sections) }, icon("rules"));
}

function rulesDialog(title, sections) {
  openDialog((close) => [
    h("h2", {}, title),
    h("div.rules-text", {}, sections.map(([text, heading]) => [heading ? h("h3", {}, heading) : null, text.split("\n\n").map((p) => h("p", {}, p))])),
    actions(h("button.text-btn", { onclick: close }, "Cerrar")),
  ], { wide: true });
}
