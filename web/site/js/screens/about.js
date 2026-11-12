import { h } from "../dom.js";
import { openDialog, actions } from "../dialogs.js";

const SOURCE_URL = "https://github.com/DavierSB/polilexico";
const SITE_URL = "https://polilexico.github.io/org/";

const PARAGRAPHS = [
  "El propósito de Poliléxico es ser la primera aplicación offline para móvil que combine las diferentes modalidades del Scrabble con todas sus reglas precisas, y además brinde herramientas didácticas para su aprendizaje.",
  "El motor de juego está basado en el de Woogles.io (Macondo), usado bajo los términos de su licencia, la GNU General Public License v3.",
  "Las definiciones sin conexión son de Wikcionario y Wiktionary (colaboradores de Wikimedia), bajo licencia CC BY-SA 4.0, y del Diccionario de la lengua castellana de la Real Academia Española, 14.ª ed. (1914), de dominio público.",
  "Los avatares de los bots son un tributo a la cultura hispanoamericana, y en especial a la cubana.",
  "Hecha con amor desde Cuba.",
];

const LICENSE = "© 2026 Davier Sánchez Bello. Poliléxico es software libre: se distribuye bajo la GNU General Public License, versión 3 o (a tu elección) cualquier versión posterior, con los términos adicionales que acompañan al código.";

export function aboutDialog() {
  openDialog((close) => [
    h("h2", {}, "¡Gracias por usar Poliléxico! ", h("span.accent", {}, "♥")),
    h("div.stack.about-text", {}, PARAGRAPHS.map((p) => h("p", {}, p)), link("Visítanos", SITE_URL), h("button.about-link", { onclick: authorDialog }, "Acerca del autor"), license()),
    actions(h("button.text-btn", { onclick: close }, "Cerrar")),
  ]);
}

function license() {
  return h("div.stack.about-license", {}, h("p.muted.small", {}, LICENSE), link("Código fuente", SOURCE_URL));
}

function link(text, href) {
  return h("a.about-link", { href, target: "_blank", rel: "noopener" }, text);
}

function authorDialog() {
  openDialog((close) => [
    h("div.stack.about-author", {}, h("span", {}, "Davier Sánchez Bello"), h("span.muted.small", {}, "daviersanbell@gmail.com"), h("span.muted.small", {}, "+53 55722267")),
    actions(h("button.text-btn", { onclick: close }, "Cerrar")),
  ]);
}
