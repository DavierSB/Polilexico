import { h } from "../dom.js";
import { icon } from "../icons.js";
import { openDialog, actions } from "../dialogs.js";
import { nameFlag } from "../parts.js";

export function chatLine(who, text, mine) {
  return h("div", { class: "chat-line" + (mine ? " mine" : "") }, h("b", {}, who + ": "), text);
}

export function chatInput(send) {
  const input = h("input.chat-input", { placeholder: "Escribe un mensaje", maxlength: 200, autocomplete: "off", "aria-label": "Mensaje" });
  const submit = (e) => {
    e.preventDefault();
    const text = input.value.trim();
    if (!text) return;
    send(text);
    input.value = "";
  };
  return h("form.chat-form", { onsubmit: submit }, input, h("button.icon-btn", { type: "submit", "aria-label": "Enviar" }, icon("send", 22)));
}

export function chatButton(open) {
  return h("button.icon-btn.bar-chat", { "aria-label": "Chat", onclick: open }, "💬");
}

export function watchersButton(watchers) {
  if (!watchers?.length) return null;
  return h("button.text-btn.watchers-btn", { "aria-label": "Observando", onclick: () => watchersDialog(watchers) }, icon("eye"), String(watchers.length));
}

function watchersDialog(watchers) {
  openDialog((close) => [h("h2", {}, "Observando"), h("div.stack", {}, watchers.map((w) => h("div.player-row", {}, nameFlag(w.name, w.country)))), actions(h("button.text-btn", { onclick: close }, "Cerrar"))]);
}


