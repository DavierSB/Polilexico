import { h, replace } from "../dom.js";
import { go } from "../router.js";
import { screenHead, parseDuration, engineGate } from "../parts.js";
import { load, save } from "../store.js";
import { BOTS, bot, botPhoto } from "./bots.js";
import { startLocal } from "./session.js";
import { toast } from "../dialogs.js";
import { modeTabs } from "../modeTabs.js";
import { timeSection, challengeSection, choiceRow, studySection } from "./options.js";

const DEFAULTS = { bot: "HastyBot", starter: 0, single: true, timed: false, time: "30:00", overtime: "8:00", shared: false, first: "Jugador A", second: "Jugador B" };
const NAME_CHARS = 16;

export function classicSetupScreen(app) {
  const form = { ...DEFAULTS, ...load("classicForm", {}) };
  const settings = h("div.stack.setup-settings");
  const rivals = h("div.stack.setup-rivals");
  const gates = [];
  const redraw = () => {
    gates.splice(0).forEach((stop) => stop());
    replace(rivals, form.shared ? sharedNames(form, redraw) : [h("p.section-title", {}, "Rival"), BOTS.map((b) => botCard(b, form, redraw)),
      h("button.text-btn.switch-prompt", { onclick: () => ((form.shared = true), (form.starter = 0), redraw()) }, "¿O son dos jugadores locales?")]);
    replace(settings, settingsFor(form, redraw, gates));
  };
  redraw();
  const tabs = modeTabs("classic", () => ({ el: h("div.setup", {}, rivals, settings) }));
  app.append(h("div.screen.wide", {}, screenHead("Clásica"), tabs.el));
  return () => (gates.forEach((stop) => stop()), tabs.destroy());
}

function botCard(b, form, redraw) {
  const selected = form.bot === b.name;
  return h("button", { class: "card choice" + (selected ? " selected" : ""), onclick: () => ((form.bot = b.name), redraw()) },
    h("img.avatar", { src: botPhoto(b), alt: "", width: 64, height: 64 }),
    h("div.grow", {}, h("div.title", {}, b.alias), h("div.muted", {}, b.description)),
  );
}

function sharedNames(form, redraw) {
  const field = (label, key) => h("div.field", {}, h("label", {}, label), h("input", { value: form[key], maxlength: NAME_CHARS, onchange: (e) => ((form[key] = e.target.value), redraw()) }));
  return [
    h("p.section-title", {}, "Jugadores"), field("Jugador A", "first"), field("Jugador B", "second"),
    h("button.text-btn.switch-prompt", { onclick: () => ((form.shared = false), (form.starter = 0), redraw()) }, "¿O eres tú solo?"),
  ];
}

function namesValid(form) {
  const a = form.first.trim(), b = form.second.trim();
  return a && b && a.toLowerCase() !== b.toLowerCase() && !bot(a) && !bot(b);
}

function settingsFor(form, redraw, gates) {
  const b = bot(form.bot);
  const starters = form.shared ? ["Cualquiera", form.first.trim() || "A", form.second.trim() || "B"] : ["Cualquiera", "Tú", b.alias];
  return [
    h("p.section-title", {}, "Salidor"),
    choiceRow(starters, form.starter, (i) => ((form.starter = i), redraw())),
    challengeSection(form, redraw, !form.shared && b.voidOnly ? b.alias : null),
    timeSection(form, redraw),
    form.shared ? null : studySection(form, redraw),
    startButton(form, gates),
  ];
}

function startButton(form, gates) {
  const button = h("button.btn.block.start", { onclick: () => begin(form, button) }, "Empezar");
  gates.push(engineGate(button));
  return button;
}

function valid(form) {
  return !form.timed || (parseDuration(form.time) > 0 && parseDuration(form.overtime) !== null);
}

async function begin(form, button) {
  if (!valid(form)) return toast("Revisa los tiempos.");
  if (form.shared && !namesValid(form)) return toast("Revisa los nombres.");
  save("classicForm", form);
  button.disabled = true;
  const b = bot(form.bot);
  const setup = {
    bot: form.bot, starter: form.starter, single: form.single && (form.shared || !b.voidOnly),
    players: form.shared ? [form.first.trim(), form.second.trim()] : null,
    study: !form.shared && !!form.study,
    timeMs: form.timed ? parseDuration(form.time) : 0, overtimeMs: form.timed ? parseDuration(form.overtime) : 0,
  };
  try {
    await startLocal(setup);
    go("partida");
  } catch (e) {
    toast(e.message);
    button.disabled = false;
  }
}
