import { h } from "../dom.js";
import { go } from "../router.js";
import { openDialog, actions } from "../dialogs.js";
import { online } from "./client.js";
import { lobbyRoute, roomRoute } from "./lobby.js";

const TEXT = "Termina la partida en la que estás involucrado primero.";

export function blockedByGame() {
  if (!online.seated()) return false;
  involvedDialog();
  return true;
}

export function involvedDialog() {
  openDialog((close) => [
    h("p", {}, TEXT),
    actions(
      h("button.text-btn", { onclick: close }, "Cerrar"),
      h("button.btn", { onclick: () => (close(), backToGame()) }, "Continuar partida"),
    ),
  ]);
}

function backToGame() {
  online.parked = null;
  go(roomRoute(online) ?? lobbyRoute());
}
