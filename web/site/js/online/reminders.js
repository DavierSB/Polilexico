import { h } from "../dom.js";
import { go } from "../router.js";
import { flag } from "../parts.js";
import { openDialog, actions } from "../dialogs.js";
import { save } from "../store.js";
import { online } from "./client.js";
import { photoAvatar } from "./profile.js";

const REMIND_EVERY_MS = 30 * 60_000;
const shownAt = new Map();
const state = { open: false };

export function remindAdjourned(c, allowed) {
  if (!allowed || state.open) return;
  const due = c.lobby.adjourned.filter((a) => available(a) && a.online && Date.now() - (shownAt.get(a.id) ?? 0) > REMIND_EVERY_MS);
  if (!due.length) return;
  due.forEach((a) => shownAt.set(a.id, Date.now()));
  state.open = true;
  const onClose = () => (state.open = false);
  if (due.length === 1) oneDialog(due[0], onClose);
  else manyDialog(due, onClose);
}

function available(a) {
  return !a.result && (a.online || a.untimed) && !a.busy;
}

function oneDialog(a, onClose) {
  openDialog((close) => [
    h("div.resume-icon", {}, photoAvatar(a.opponent, a.country, 56)),
    h("h2.resume-title", {}, `${a.opponent} ${flag(a.country)}`),
    h("p", {}, `${a.opponent}, con quien tienes una partida pendiente, está en línea y puedes retomarla.`),
    actions(h("button.text-btn", { onclick: close }, "Ahora no"), h("button.btn.small", { onclick: () => (close(), resume(a)) }, "Retomar")),
  ], { onClose });
}

function manyDialog(games, onClose) {
  openDialog((close) => [
    h("div.resume-icon.resume-many", {}, games.slice(0, 4).map((a) => photoAvatar(a.opponent, a.country, 44))),
    h("p", {}, "Varios de los jugadores con quienes tienes partidas pendientes están en línea y puedes retomar sus partidas."),
    actions(h("button.text-btn", { onclick: close }, "Entendido")),
  ], { onClose });
}

function resume(a) {
  const mode = a.duplicate ? "duplicate" : "classic";
  online.resumeAdjourned(a.id);
  save(`tab.${mode}`, "online");
  go(a.duplicate ? "duplicada" : "clasica");
}
