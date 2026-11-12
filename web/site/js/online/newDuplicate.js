import { h, replace } from "../dom.js";
import { lobbyRoute } from "./lobby.js";
import { go } from "../router.js";
import { screenHead, parseDuration } from "../parts.js";
import { load, save } from "../store.js";
import { toast } from "../dialogs.js";
import { choiceRow, sectionTitle, stepper } from "../classic/options.js";
import { account } from "./api.js";
import { online } from "./client.js";
import { blockedByGame } from "./involved.js";

const ROUNDS_TEXT = "Cuántas rondas se juegan. A bolsa completa, la partida sigue hasta que ya no se puede formar un atril válido.";
const LIVE_INFO = "Ver los puntos de todos tras cada ronda.";
const DEFAULTS = { fixed: false, rounds: 12, turn: "3:00", live: true };

export function newDuplicateScreen(app) {
  if (!account()) return go("cuenta", true);
  const form = { ...DEFAULTS, ...load("onlineDuplicateForm", {}) };
  const body = h("div.stack.setup-narrow");
  const redraw = () => replace(body,
    sectionTitle("Rondas", ROUNDS_TEXT),
    choiceRow(["Bolsa completa", "Nº de rondas fijo"], form.fixed ? 1 : 0, (i) => ((form.fixed = i === 1), redraw())),
    form.fixed ? stepper(form.rounds, 1, 40, (n) => ((form.rounds = n), redraw())) : null,
    sectionTitle("Tiempo", "Cada ronda dura el tiempo completo."),
    h("div.field", {}, h("label", { for: "round-time" }, "Tiempo por ronda"), h("input", { id: "round-time", value: form.turn, inputmode: "numeric", oninput: (e) => (form.turn = e.target.value) })),
    sectionTitle("Puntuaciones en vivo", LIVE_INFO),
    choiceRow(["Sí", "No"], form.live ? 0 : 1, (i) => ((form.live = i === 0), redraw())),
    h("button.btn.block.start", { onclick: () => create(form) }, "Crear sala"),
  );
  redraw();
  app.append(h("div.screen", {}, screenHead("Duplicada en línea", lobbyRoute()), body));
  online.acquire();
  return () => online.release();
}

function create(form) {
  const turnMs = parseDuration(form.turn);
  if (!turnMs) return toast("Revisa el tiempo por ronda.");
  if (blockedByGame()) return;
  save("onlineDuplicateForm", form);
  save("lobbyTab", "duplicate");
  online.publish({ duplicate: true, turnMs, rounds: form.fixed ? form.rounds : 0, live: form.live });
  go(lobbyRoute(), true);
}
