import { h, replace } from "../dom.js";
import { go } from "../router.js";
import { screenHead, parseDuration, engineGate } from "../parts.js";
import { load, save } from "../store.js";
import { BOTS, bot, botPhoto } from "./bots.js";
import { startLocal, currentSession, hasSaved } from "./session.js";
import { icon } from "../icons.js";
import { savedButtons, keepScroll } from "../savedButtons.js";
import { toast } from "../dialogs.js";
import { modeTabs } from "../modeTabs.js";
import { accountButtons } from "../online/profile.js";
import { timeSection, challengeSection, choiceRow } from "./options.js";

const DEFAULTS = { bot: "HastyBot", starter: 0, single: true, timed: false, time: "30:00", overtime: "8:00", shared: false, first: "Jugador A", second: "Jugador B" };
const NAME_CHARS = 16;
const LAPTOP = "(min-width: 900px) and (min-aspect-ratio: 1/1)";

export function classicSetupScreen(app) {
  const form = { ...DEFAULTS, ...load("classicForm", {}) };
  const gates = [];
  const step = h("div.setup-step");
  const screen = h("div.screen.wide.fit-screen");
  const view = { settings: false };
  const account = accountButtons();
  const redraw = () => {
    gates.splice(0).forEach((stop) => stop());
    keepScroll(step, () => replace(step, view.settings ? settingsStep(form, redraw, gates) : rivalStep(form, redraw, gates, choose)));
  };
  const tabs = modeTabs("classic", () => ({ el: step }));
  const choose = (settings) => {
    view.settings = settings;
    replace(step);
    replace(screen, settings ? stepHead(form, () => choose(false)) : screenHead("Clásica", "", account.el), settings ? step : tabs.el);
    redraw();
  };
  choose(false);
  app.append(screen);
  return () => (gates.forEach((stop) => stop()), tabs.destroy(), account.stop());
}

function stepHead(form, back) {
  return h("header.screen-head", {},
    h("button.icon-btn", { "aria-label": "Volver", onclick: back }, icon("back")),
    h("h1", {}, form.shared ? "Dos jugadores" : "Clásica contra " + bot(form.bot).alias),
  );
}

function rivalStep(form, redraw, gates, choose) {
  if (matchMedia(LAPTOP).matches) return laptopStep(form, redraw, gates, choose);
  return [
    h("div.setup-scroll", {}, BOTS.map((b) => botCard(b, form, redraw)), localPrompt(form, choose)),
    h("button.btn.block.start", { onclick: () => ((form.shared = false), choose(true)) }, "Seleccionar rival"),
    savedGames(gates),
  ];
}

function laptopStep(form, redraw, gates, choose) {
  form.shared = false;
  return h("div.setup-columns", {},
    h("div.setup-scroll.bot-column", {}, BOTS.map((b) => botCard(b, form, redraw)), localPrompt(form, choose)),
    h("div.setup-column", {}, h("div.setup-scroll", {}, settingsFor(form, redraw)), startButton(form, gates), savedGames(gates)),
  );
}

function localPrompt(form, choose) {
  return h("button.text-btn.switch-prompt", { onclick: () => ((form.shared = true), (form.starter = 0), choose(true)) }, "¿O son dos jugadores locales?");
}

function savedGames(gates) {
  const live = currentSession()?.live() || hasSaved();
  return savedButtons("classic", live ? 1 : 0, "partida", gates);
}

function settingsStep(form, redraw, gates) {
  return [
    h("div.setup-scroll", {}, form.shared ? sharedNames(form, redraw) : null, settingsFor(form, redraw)),
    startButton(form, gates),
  ];
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
  return [h("p.section-title", {}, "Jugadores"), field("Jugador A", "first"), field("Jugador B", "second")];
}

function namesValid(form) {
  const a = form.first.trim(), b = form.second.trim();
  return a && b && a.toLowerCase() !== b.toLowerCase() && !bot(a) && !bot(b);
}

function settingsFor(form, redraw) {
  const b = bot(form.bot);
  const starters = form.shared ? ["Cualquiera", form.first.trim() || "A", form.second.trim() || "B"] : ["Cualquiera", "Tú", b.alias];
  return [
    h("p.section-title", {}, "Salidor"),
    choiceRow(starters, form.starter, (i) => ((form.starter = i), redraw())),
    challengeSection(form, redraw, !form.shared && b.voidOnly ? b.alias : null),
    timeSection(form, redraw),
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
