import { h } from "../dom.js";
import { icon } from "../icons.js";
import { go } from "../router.js";
import { brand } from "../parts.js";
import { themeDialog } from "../dialogs.js";
import { optionsDialog } from "./options.js";
import { aboutDialog } from "./about.js";
import { DOWNLOAD_URL } from "./download.js";

const layer = () => document.getElementById("layer");

export function drawerButton() {
  return h("button.icon-btn.drawer-btn", { "aria-label": "Menú", onclick: openDrawer }, icon("menu"));
}

function openDrawer() {
  const sheet = h("nav.drawer", { "aria-label": "Menú" });
  const scrim = h("div.drawer-scrim", {}, sheet);
  const close = () => (scrim.remove(), document.removeEventListener("keydown", onKey, true));
  const onKey = (e) => e.key === "Escape" && close();
  scrim.addEventListener("click", (e) => e.target === scrim && close());
  document.addEventListener("keydown", onKey, true);
  sheet.append(h("div.drawer-brand", {}, brand()), ...drawerItems(close));
  layer().append(scrim);
}

export function drawerItems(close = () => {}) {
  const item = (label, action) => h("button.drawer-item", { onclick: () => (close(), action()) }, label);
  return [
    item("Inicio", () => go("")),
    item("Mis estadísticas", () => go("estadisticas")),
    h("hr.drawer-rule"),
    item("Tema", themeDialog),
    item("Opciones", optionsDialog),
    h("hr.drawer-rule"),
    item("Acerca de", aboutDialog),
    h("a.drawer-item.drawer-download", { href: DOWNLOAD_URL }, "Descargar APK para Android"),
  ];
}
