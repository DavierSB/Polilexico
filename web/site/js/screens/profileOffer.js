import { h } from "../dom.js";
import { go } from "../router.js";
import { openDialog, actions } from "../dialogs.js";
import { onPresence } from "../online/presence.js";
import { account } from "../online/api.js";
import { load, save } from "../store.js";

const OFFERED = "profileOffered";

export function offerProfile() {
  let people = 0;
  const stop = onPresence((n) => (people = n ?? 0));
  return { go: (route) => (shouldOffer(people) ? offerDialog(people, route) : go(route)), stop };
}

function shouldOffer(people) {
  return !account() && !load(OFFERED, false) && people > 0;
}

function offerDialog(people, route) {
  const answer = (create, close) => {
    save(OFFERED, true);
    close();
    go(route);
    if (create) go("cuenta");
  };
  openDialog((close) => [
    h("div.dialog-icon", {}, "👥"),
    h("h2.center", {}, "Jugar en línea"),
    h("p", {}, people === 1 ? "Hay 1 persona conectada." : `Hay ${people} personas conectadas.`),
    actions(h("button.text-btn", { onclick: () => answer(false, close) }, "Ahora no"), h("button.btn.small", { onclick: () => answer(true, close) }, "Crear perfil")),
  ], { onClose: () => save(OFFERED, true) });
}
