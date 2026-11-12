import { h } from "../dom.js";
import { openDialog, actions } from "../dialogs.js";

const SOURCE_URL = "https://github.com/DavierSB/polilexico";
const SITE_URL = "https://polilexico.github.io/";

const PURPOSE = "El propósito de Poliléxico es ser un espacio multiplataforma gratuito, con todos los recursos necesarios para jugar al Scrabble en cualquiera de sus modalidades, y hacer más fácil y directo el crecimiento de los jugadores de Scrabble.";

const PLATFORMS = [
  "Contamos con una plataforma web (",
  ") y una aplicación móvil para Android, que permiten jugar con o sin conexión a internet cualquiera de las modalidades, Clásica o Duplicada, con sus reglas oficiales; así como disfrutar de minijuegos, listas de palabras para estudiar y herramientas como el Diccionario, el Anagramador y el Analizador de posiciones.",
];

const CREDITS = [
  "El motor de juego está basado en el de Woogles.io (Macondo), usado bajo los términos de su licencia, la GNU General Public License v3.",
  "Las definiciones sin conexión son de Wikcionario y Wiktionary (colaboradores de Wikimedia), bajo licencia CC BY-SA 4.0, y del Diccionario de la lengua castellana de la Real Academia Española, 14.ª ed. (1914), de dominio público.",
  "Los avatares de los bots son un tributo a la cultura hispanoamericana, y en especial a la cubana.",
  "Hecha con amor desde Cuba.",
];

const LICENSE = "© 2026 Davier Sánchez Bello. Poliléxico es software libre: se distribuye bajo la GNU General Public License, versión 3 o (a tu elección) cualquier versión posterior, con los términos adicionales que acompañan al código.";

export function aboutDialog() {
  openDialog((close) => [
    h("h2.about-title", {}, "¡Gracias por usar Poliléxico! ", h("span.about-heart", {}, "♥")),
    h("div.stack.about-text", {},
      h("p", {}, PURPOSE),
      h("p", {}, PLATFORMS[0], link("polilexico.github.io", SITE_URL), PLATFORMS[1]),
      CREDITS.map((p) => h("p", {}, p)),
      h("button.about-link", { onclick: authorDialog }, "Acerca del autor"),
      license(),
    ),
    actions(h("button.text-btn", { onclick: close }, "Cerrar")),
  ]);
}

function license() {
  const text = h("p.muted.small", {}, LICENSE);
  appVersion().then((v) => v && text.prepend(`Versión ${v}. `));
  return h("div.stack.about-license", {}, text, link("Código fuente", SOURCE_URL));
}

async function appVersion() {
  try {
    const response = await fetch("version.json", { cache: "no-cache" });
    return response.ok ? (await response.json()).version : null;
  } catch {
    return null;
  }
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
