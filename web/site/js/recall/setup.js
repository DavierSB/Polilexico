import { h, replace } from "../dom.js";
import { go } from "../router.js";
import { screenHead, engineGate } from "../parts.js";
import { load, save } from "../store.js";
import { choiceRow, sectionTitle, stepper, rulesButton } from "../classic/options.js";
import { RecallSession, recordKey } from "./session.js";
import { RECALL_RULES, CHALLENGE_INFO } from "./rules.js";

const DEFAULTS = { intervalMs: 1600, lives: 3, single: true };
let current = null;

export function currentRecall() {
  return current;
}

export function recallSetupScreen(app) {
  const form = { ...DEFAULTS, ...load("recallForm", {}) };
  const body = h("div.stack.setup-narrow");
  const gates = [];
  const redraw = () => {
    gates.splice(0).forEach((stop) => stop());
    replace(body, fields(form, redraw, gates));
  };
  redraw();
  app.append(h("div.screen", {}, screenHead("¿Cuántas recuerdas?", "minijuegos"), body));
  return () => gates.forEach((stop) => stop());
}

function fields(form, redraw, gates) {
  const start = h("button.btn.block.start", { onclick: () => begin(form) }, "Empezar");
  gates.push(engineGate(start));
  const best = load(recordKey(form), 0);
  const shown = h("b", {}, seconds(form.intervalMs));
  return [
    rulesButton("Reglas de ¿Cuántas recuerdas?", RECALL_RULES),
    h("div.row", {}, h("div.grow", {}, sectionTitle("Tiempo entre jugadas", "Menos tiempo, más difícil recordar.")), shown),
    h("input.range", { type: "range", min: 500, max: 3000, step: 100, value: form.intervalMs, "aria-label": "Tiempo entre jugadas", oninput: (e) => ((form.intervalMs = Number(e.target.value)), (shown.textContent = seconds(form.intervalMs))), onchange: redraw }),
    sectionTitle("Vidas", "Cada palabra que no recuerdas cuesta una vida. Sin vidas, se acaba la serie."),
    stepper(form.lives, 1, 5, (n) => ((form.lives = n), redraw())),
    sectionTitle("Comprobación de jugadas", CHALLENGE_INFO),
    choiceRow(["Void", "Single"], form.single ? 1 : 0, (i) => ((form.single = i === 1), redraw())),
    best ? h("div.card.record-card", {}, h("span.muted", {}, "Récord"), h("b", {}, `${best} ${best === 1 ? "palabra" : "palabras"}`)) : null,
    start,
  ];
}

function seconds(ms) {
  return `${(ms / 1000).toLocaleString("es", { minimumFractionDigits: 1, maximumFractionDigits: 1 })} s`;
}

function begin(form) {
  save("recallForm", form);
  current = new RecallSession({ ...form });
  go("recuerdas-partida");
}
