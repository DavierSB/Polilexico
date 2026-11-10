import { h, replace } from "../dom.js";
import { go } from "../router.js";
import { screenHead, parseDuration, engineGate } from "../parts.js";
import { load, save } from "../store.js";
import { BOTS, bot, botPhoto } from "./bots.js";
import { startLocal } from "./session.js";
import { toast } from "../dialogs.js";
import { timeSection, challengeSection, choiceRow } from "./options.js";

const DEFAULTS = { bot: "HastyBot", starter: 0, single: true, timed: false, time: "30:00", overtime: "8:00" };

export function classicSetupScreen(app) {
  const form = { ...DEFAULTS, ...load("classicForm", {}) };
  const settings = h("div.stack.setup-settings");
  const rivals = h("div.stack.setup-rivals");
  const gates = [];
  const redraw = () => {
    gates.splice(0).forEach((stop) => stop());
    replace(rivals, h("p.section-title", {}, "Rival"), BOTS.map((b) => botCard(b, form, redraw)));
    replace(settings, settingsFor(form, redraw, gates));
  };
  redraw();
  app.append(h("div.screen.wide", {}, screenHead("Clásica"), h("div.setup", {}, rivals, settings)));
  return () => gates.forEach((stop) => stop());
}

function botCard(b, form, redraw) {
  const selected = form.bot === b.name;
  return h("button", { class: "card choice" + (selected ? " selected" : ""), onclick: () => ((form.bot = b.name), redraw()) },
    h("img.avatar", { src: botPhoto(b), alt: "", width: 64, height: 64 }),
    h("div.grow", {}, h("div.title", {}, b.alias), h("div.muted", {}, b.description)),
  );
}

function settingsFor(form, redraw, gates) {
  const b = bot(form.bot);
  return [
    h("p.section-title", {}, "Salidor"),
    choiceRow(["Cualquiera", "Tú", b.alias], form.starter, (i) => ((form.starter = i), redraw())),
    challengeSection(form, redraw, b.voidOnly ? b.alias : null),
    timeSection(form, redraw),
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
  save("classicForm", form);
  button.disabled = true;
  const b = bot(form.bot);
  const setup = {
    bot: form.bot, starter: form.starter, single: form.single && !b.voidOnly,
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
