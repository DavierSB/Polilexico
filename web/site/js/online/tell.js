import { h, replace } from "../dom.js";
import { icon } from "../icons.js";
import { openDialog, actions } from "../dialogs.js";
import { nameFlag } from "../parts.js";
import { online } from "./client.js";

export function tellDialog(person) {
  const lines = h("div.chat-lines");
  const draw = (c) => replace(lines, conversation(c, person));
  let stop = null;
  openDialog((close) => {
    stop = online.subscribe(draw);
    return [
      h("h2", {}, nameFlag(person.name, person.country)),
      lines,
      chatInput((text) => online.tell(person, text)),
      actions(
        h("button.text-btn", { onclick: () => (online.ignore(person), close()) }, "Ignorar"),
        h("button.text-btn", { onclick: close }, "Cerrar"),
      ),
    ];
  }, { onClose: () => stop?.() });
}

function conversation(c, person) {
  const mine = c.chat.filter((l) => l.private && l.from === person.name && (l.country ?? "") === (person.country ?? ""));
  if (!mine.length) return h("p.muted", {}, "Sin mensajes.");
  return mine.map((l) => chatLine(l.mine ? "Tú" : l.from, l.text, l.mine));
}

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
