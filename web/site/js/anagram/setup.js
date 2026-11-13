import { h, replace } from "../dom.js";
import { go } from "../router.js";
import { screenHead, parseDuration, engineGate, setupColumns, recordCard } from "../parts.js";
import { load, save } from "../store.js";
import { toast } from "../dialogs.js";
import { choiceRow, sectionTitle, stepper, rulesButton } from "../classic/options.js";
import { AnagramSession, recordKey } from "./session.js";
import { ANAGRAM_RULES, DIFFICULTY_INFO } from "./rules.js";

const KEYS = ["easy", "medium", "hard"];
const LABELS = ["Fácil", "Medio", "Difícil"];
const DEFAULTS = { difficulty: "medium", total: "5:00", lives: 3 };
let current = null;

export function currentAnagram() {
  return current;
}

export function anagramSetupScreen(app) {
  const form = { ...DEFAULTS, ...load("anagramForm", {}) };
  const body = h("div.stack.setup-narrow");
  const gates = [];
  const redraw = () => {
    gates.splice(0).forEach((stop) => stop());
    replace(body, fields(form, redraw, gates));
  };
  redraw();
  app.append(h("div.screen", {}, screenHead("Anagrama", "minijuegos"), body));
  return () => gates.forEach((stop) => stop());
}

function configOf(form) {
  const totalMs = parseDuration(form.total);
  return totalMs ? { totalMs, lives: form.lives, difficulty: form.difficulty } : null;
}

function fields(form, redraw, gates) {
  const start = h("button.btn.block.start", { onclick: () => begin(form) }, "Empezar");
  gates.push(engineGate(start));
  const config = configOf(form);
  const best = config ? load(recordKey(config), 0) : 0;
  return setupColumns([
    rulesButton("Reglas de Anagrama", ANAGRAM_RULES),
    sectionTitle("Dificultad", DIFFICULTY_INFO),
    choiceRow(LABELS, KEYS.indexOf(form.difficulty), (i) => ((form.difficulty = KEYS[i]), redraw())),
    sectionTitle("Tiempo"),
    h("div.field", {}, h("label", { for: "anagram-total" }, "Tiempo de la serie"), h("input", { id: "anagram-total", value: form.total, inputmode: "numeric", oninput: (e) => (form.total = e.target.value) })),
    sectionTitle("Vidas", "Una palabra no válida o rendirse cuesta una vida. Sin vidas, se acaba la serie."),
    stepper(form.lives, 1, 5, (n) => ((form.lives = n), redraw())),
  ], [recordCard(best, "mano", "manos"), start]);
}

function begin(form) {
  const config = configOf(form);
  if (!config) return toast("Revisa el tiempo.");
  save("anagramForm", form);
  current?.close();
  current = new AnagramSession(config);
  go("anagrama-partida");
}
