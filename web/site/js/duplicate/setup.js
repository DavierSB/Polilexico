import { h, replace } from "../dom.js";
import { go } from "../router.js";
import { screenHead, parseDuration, engineGate } from "../parts.js";
import { load, save } from "../store.js";
import { toast } from "../dialogs.js";
import { modeTabs } from "../modeTabs.js";
import { accountButtons } from "../online/profile.js";
import { choiceRow, sectionTitle, stepper, rulesButton } from "../classic/options.js";
import { startDuplicate } from "./session.js";
import { savedButtons, keepScroll } from "../savedButtons.js";
import { DUPLICATE_RULES, ROUNDS_TEXT, VOID_HINT, SINGLE_HINT, AT_END_HINT } from "./rules.js";

const CHECKS = ["Void", "Single", "Al Final"];
const CHALLENGE_INTRO = "Cómo se tratan las jugadas con palabras que no existen.";
const DEFAULTS = { check: 1, fixed: false, rounds: 12, turn: "3:00" };

export function duplicateSetupScreen(app) {
  const form = { ...DEFAULTS, ...load("duplicateForm", {}) };
  const body = h("div.setup-step");
  const gates = [];
  const redraw = () => {
    gates.splice(0).forEach((stop) => stop());
    keepScroll(body, () => replace(body, h("div.setup-scroll", {}, fields(form, redraw)), startButton(form, gates), savedButtons("duplicate", gates)));
  };
  redraw();
  const tabs = modeTabs("duplicate", () => ({ el: body }));
  const account = accountButtons();
  app.append(h("div.screen.wide.fit-screen", {}, screenHead("Duplicada", "", account.el), tabs.el));
  return () => (gates.forEach((stop) => stop()), tabs.destroy(), account.stop());
}

function startButton(form, gates) {
  const start = h("button.btn.block.start", { onclick: () => begin(form, start) }, "Empezar");
  gates.push(engineGate(start));
  return start;
}

function fields(form, redraw) {
  return [
    rulesButton("Reglas de la Duplicada", DUPLICATE_RULES),
    sectionTitle("Rondas", ROUNDS_TEXT),
    choiceRow(["Bolsa completa", "Nº de rondas fijo"], form.fixed ? 1 : 0, (i) => ((form.fixed = i === 1), redraw())),
    form.fixed ? stepper(form.rounds, 1, 40, (n) => ((form.rounds = n), redraw())) : null,
    sectionTitle("Comprobación de jugadas", [CHALLENGE_INTRO, "Void: " + VOID_HINT, "Single: " + SINGLE_HINT, "Al Final: " + AT_END_HINT].join("\n\n")),
    choiceRow(CHECKS, form.check, (i) => ((form.check = i), redraw())),
    sectionTitle("Tiempo", "Si no juegas a tiempo, 0 puntos ese turno."),
    turnField(form),
  ];
}

function turnField(form) {
  return h("div.field", {},
    h("label", { for: "turn-time" }, "Tiempo por turno"),
    h("input", { id: "turn-time", value: form.turn, inputmode: "numeric", oninput: (e) => (form.turn = e.target.value) }),
  );
}

async function begin(form, button) {
  const turnMs = parseDuration(form.turn);
  if (!turnMs) return toast("Revisa el tiempo por turno.");
  save("duplicateForm", form);
  button.disabled = true;
  try {
    const session = await startDuplicate({ turnMs, single: form.check !== 0, atEnd: form.check === 2, rounds: form.fixed ? form.rounds : 0 });
    go("duplicada-partida/" + session.id);
  } catch (e) {
    toast(e.message);
    button.disabled = false;
  }
}
