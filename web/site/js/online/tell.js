import { h } from "../dom.js";
import { icon } from "../icons.js";
import { openDialog, actions } from "../dialogs.js";
import { nameFlag } from "../parts.js";
import { profileDialog } from "./profile.js";

export function chatButton(open) {
  return h("button.icon-btn.bar-chat", { "aria-label": "Chat", onclick: open }, "💬");
}

export function watchersButton(watchers) {
  if (!watchers?.length) return null;
  return h("button.text-btn.watchers-btn", { "aria-label": "Observando", onclick: () => watchersDialog(watchers) }, icon("eye"), String(watchers.length));
}

function watchersDialog(watchers) {
  openDialog((close) => [h("h2", {}, "Observando"), h("div.stack", {}, watchers.map((w) => h("button.player-row.contact-row.opens-profile", { onclick: () => profileDialog(w) }, nameFlag(w.name, w.country)))), actions(h("button.text-btn", { onclick: close }, "Cerrar"))]);
}


