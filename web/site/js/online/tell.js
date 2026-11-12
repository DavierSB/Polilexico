import { h } from "../dom.js";
import { icon } from "../icons.js";
import { openDialog, actions } from "../dialogs.js";

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

export function chatButton(panel) {
  return h("button.icon-btn.bar-chat", { "aria-label": "Chat", onclick: () => chatDialog(panel) }, "💬");
}

function chatDialog(panel) {
  const home = panel.parentNode;
  const next = panel.nextSibling;
  openDialog((close) => [panel, actions(h("button.text-btn", { onclick: close }, "Cerrar"))], { onClose: () => home?.insertBefore(panel, next) });
}
