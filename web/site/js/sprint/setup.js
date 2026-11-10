import { h, replace } from "../dom.js";
import { go } from "../router.js";
import { screenHead, parseDuration, engineGate } from "../parts.js";
import { load, save } from "../store.js";
import { toast } from "../dialogs.js";
import { choiceRow, sectionTitle, stepper, rulesButton } from "../classic/options.js";
import { startSprint, recordKey } from "./session.js";
import { SPRINT_RULES, DIFFICULTY_INFO, CHALLENGE_INFO } from "./rules.js";

const DIFFICULTIES = [["easy", "Fácil"], ["normal", "Normal"], ["hard", "Difícil"]];
const DEFAULTS = { difficulty: "normal", total: "5:00", lives: 3, single: true };

export function sprintSetupScreen(app) {
  const form = { ...DEFAULTS, ...load("sprintForm", {}) };
  const body = h("div.stack.setup-narrow");
  const gates = [];
  const redraw = () => {
    gates.splice(0).forEach((stop) => stop());
    replace(body, fields(form, redraw, gates));
  };
  redraw();
  app.append(h("div.screen", {}, screenHead("Scrabble Sprint", "minijuegos"), body));
  return () => gates.forEach((stop) => stop());
}

function setupOf(form) {
  const totalMs = parseDuration(form.total);
  return totalMs ? { totalMs, lives: form.lives, single: form.single, difficulty: form.difficulty } : null;
}

function fields(form, redraw, gates) {
  const start = h("button.btn.block.start", { onclick: () => begin(form, start) }, "Empezar");
  gates.push(engineGate(start));
  const setup = setupOf(form);
  const best = setup ? load(recordKey(setup), 0) : 0;
  return [
    rulesButton("Reglas de Scrabble Sprint", SPRINT_RULES),
    sectionTitle("Dificultad", DIFFICULTY_INFO),
    choiceRow(DIFFICULTIES.map(([, l]) => l), DIFFICULTIES.findIndex(([d]) => d === form.difficulty), (i) => ((form.difficulty = DIFFICULTIES[i][0]), redraw())),
    sectionTitle("Tiempo"),
    h("div.field", {}, h("label", { for: "sprint-total" }, "Tiempo de la serie"), h("input", { id: "sprint-total", value: form.total, inputmode: "numeric", oninput: (e) => (form.total = e.target.value) })),
    sectionTitle("Vidas"),
    stepper(form.lives, 1, 5, (n) => ((form.lives = n), redraw())),
    sectionTitle("Comprobación de jugadas", CHALLENGE_INFO),
    choiceRow(["Void", "Single"], form.single ? 1 : 0, (i) => ((form.single = i === 1), redraw())),
    best ? h("div.card.record-card", {}, h("span.muted", {}, "Récord"), h("b", {}, `${best} ${best === 1 ? "scrabble" : "scrabbles"}`)) : null,
    start,
  ];
}

async function begin(form, button) {
  const setup = setupOf(form);
  if (!setup) return toast("Revisa el tiempo.");
  save("sprintForm", form);
  button.disabled = true;
  try {
    await startSprint(setup);
    go("sprint-partida");
  } catch (e) {
    toast(e.message);
    button.disabled = false;
  }
}
