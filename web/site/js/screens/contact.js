import { h } from "../dom.js";
import { icon } from "../icons.js";
import { openDialog, actions } from "../dialogs.js";
import { AUTHOR_PHONE, AUTHOR_CHAT_URL, POLILEXICO_EMAIL, EMAIL_URL } from "./community.js";

export function contactDialog() {
  openDialog((close) => [
    h("h2", {}, "Escríbenos"),
    h("div.stack.contact-options", {},
      contactRow("whatsapp", "contact-whatsapp", "WhatsApp", AUTHOR_PHONE, AUTHOR_CHAT_URL),
      contactRow("mail", "contact-mail", "Correo", POLILEXICO_EMAIL, EMAIL_URL),
    ),
    actions(h("button.text-btn", { onclick: close }, "Cerrar")),
  ]);
}

function contactRow(name, kind, label, value, href) {
  return h(`a.contact-row.${kind}`, { href, target: "_blank", rel: "noopener" },
    icon(name, 24),
    h("span.stack.contact-text", {}, h("span.contact-label", {}, label), h("span.muted.small", {}, value)),
  );
}
