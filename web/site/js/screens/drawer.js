import { h } from "../dom.js";
import { icon } from "../icons.js";
import { go } from "../router.js";
import { brand } from "../parts.js";
import { themeDialog } from "../dialogs.js";

const layer = () => document.getElementById("layer");

export function drawerButton() {
  return h("button.icon-btn", { "aria-label": "Menú", onclick: openDrawer }, icon("menu"));
}

function openDrawer() {
  const sheet = h("nav.drawer", { "aria-label": "Menú" });
  const scrim = h("div.drawer-scrim", {}, sheet);
  const close = () => (scrim.remove(), document.removeEventListener("keydown", onKey, true));
  const onKey = (e) => e.key === "Escape" && close();
  const item = (label, action) => h("button.drawer-item", { onclick: () => (close(), action()) }, label);
  scrim.addEventListener("click", (e) => e.target === scrim && close());
  document.addEventListener("keydown", onKey, true);
  sheet.append(
    h("div.drawer-brand", {}, brand()),
    item("Inicio", () => go("")),
    item("Mis partidas", () => go("partidas")),
    item("Mis estadísticas", () => go("estadisticas")),
    h("hr.drawer-rule"),
    item("Tema", themeDialog),
    h("hr.drawer-rule"),
    h("a.drawer-item", { href: "/org/" }, "Poliléxico para Android"),
  );
  layer().append(scrim);
}
