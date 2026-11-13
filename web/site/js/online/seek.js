import { h, replace } from "../dom.js";
import { lobbyRoute } from "./lobby.js";
import { go } from "../router.js";
import { screenHead, parseDuration, flag } from "../parts.js";
import { toast } from "../dialogs.js";
import { load, save } from "../store.js";
import { account } from "./api.js";
import { online } from "./client.js";
import { challengeSection, timeSection, ONLINE_SINGLE_HINT } from "../classic/options.js";
import { blockedByGame } from "./involved.js";

const DEFAULTS = { single: true, timed: true, time: "20:00", overtime: "1:00" };

export function seekScreen(app, [name, country]) {
  if (!account()) return go("cuenta", true);
  const rival = name ? { name, country } : null;
  const form = { ...DEFAULTS, ...load("seekForm", {}) };
  const body = h("div.stack");
  const redraw = () => replace(body, challengeSection(form, redraw, null, ONLINE_SINGLE_HINT), timeSection(form, redraw), sendButton(form, rival));
  redraw();
  const title = rival ? `Retar a ${rival.name} ${flag(rival.country)}` : "Buscar clásica";
  app.append(h("div.screen", {}, screenHead(title, lobbyRoute()), body));
  online.acquire();
  return () => online.release();
}

function sendButton(form, rival) {
  return h("button.btn.block.start", { onclick: () => send(form, rival) }, rival ? "Enviar reto" : "Publicar búsqueda");
}

function send(form, rival) {
  const timeMs = parseDuration(form.time);
  const overtimeMs = parseDuration(form.overtime);
  if (form.timed && (!timeMs || overtimeMs === null)) return toast("Revisa los tiempos.");
  if (blockedByGame()) return;
  save("seekForm", form);
  save("lobbyTab", "classic");
  save("tab.classic", "online");
  const proposal = { single: form.single, timed: form.timed, timeMs: form.timed ? timeMs : 0, overtimeMs: form.timed ? overtimeMs : 0 };
  if (rival) online.challenge(rival, proposal);
  else online.publish(proposal);
  go(lobbyRoute(), true);
}
