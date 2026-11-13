import { h } from "../dom.js";
import { openDialog, actions, confirm } from "../dialogs.js";
import { infoButton } from "../classic/options.js";
import { online } from "./client.js";

const AWAY_MS = 3 * 60_000;
const ASK_AGAIN_MS = 2 * 60_000;
const ADJUDICATION_RULES = "Una partida aplazada puede retomarse durante 3 días, cuando los dos jugadores estén conectados; si es sin tiempo, aunque el rival no lo esté. Si pasan los 3 días, se adjudica en contra del responsable: quien propuso aplazar; si se aplazó porque el rival llevaba más de un minuto desconectado, ese rival; si se aplazó con los dos desconectados, el primero en desconectarse. Si el otro jugador va por delante, gana; si no, la partida se anula. Si el responsable hizo la última jugada, esa jugada no cuenta.";

export function watchAway(screen, c) {
  const since = c.room && !c.room.watching && !c.finished() && !c.ended ? c.rivalAwaySince : null;
  const key = `${since}|${screen.waits}`;
  if (key === screen.awayKey) return;
  screen.awayKey = key;
  clearTimeout(screen.awayTimer);
  if (since === null || since === undefined) return;
  const wait = Math.max(0, since + AWAY_MS + screen.waits * ASK_AGAIN_MS - Date.now());
  screen.awayTimer = setTimeout(() => awayDialog(screen, c), wait);
}

function awayDialog(screen, c) {
  const rival = rivalOf(c);
  const again = () => ((screen.waits += 1), watchAway(screen, online));
  openDialog((close) => [
    h("h2", {}, `${rival} lleva 3 minutos fuera`),
    h("p", {}, "Puedes aplazar la partida y retomarla cuando estéis los dos conectados, en los próximos 3 días."),
    h("div.row", {}, h("p.grow", {}, "Si pasan los 3 días, se adjudica según las reglas de adjudicación."), infoButton("Reglas de adjudicación", ADJUDICATION_RULES)),
    h("button.btn.block", { onclick: () => ((screen.adjourning = true), close(), adjournConfirm(rival, false, again)) }, "Aplazar"),
    actions(h("button.text-btn", { onclick: close }, "Esperar")),
  ], { onClose: () => (screen.adjourning ? (screen.adjourning = false) : again()) });
}

export async function adjournConfirm(rival, rivalOnline, declined = () => {}) {
  const question = rivalOnline ? `¿Proponer a ${rival} aplazar la partida?` : "¿Aplazar la partida? Se guarda 3 días para retomarla.";
  if (await confirm(question, rivalOnline ? "Proponer" : "Aplazar", "No")) online.adjourn();
  else declined();
}

export function rivalOf(c) {
  return c.room?.players.find((p) => p !== c.room.me) ?? "Tu rival";
}


export function adjournDialog(name) {
  openDialog((close) => [
    h("h2", {}, `${name} propone aplazar`),
    h("p", {}, "La partida se guarda 3 días para retomarla."),
    actions(
      h("button.text-btn", { onclick: () => (close(), online.answerAdjourn(false)) }, "Rechazar"),
      h("button.text-btn", { onclick: () => (close(), online.answerAdjourn(true)) }, "Aceptar"),
    ),
  ], { dismissable: false });
}

const noticed = new Set();

export function watchingNotice(c) {
  if (!c.room?.watching || noticed.has(c.room.id)) return;
  noticed.add(c.room.id);
  openDialog((close) => [h("div.dialog-icon", {}, "👁"), h("h2.center", {}, "Estás solo observando"), actions(h("button.text-btn", { onclick: close }, "Entendido"))]);
}
