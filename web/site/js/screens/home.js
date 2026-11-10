import { h } from "../dom.js";
import { icon } from "../icons.js";
import { go } from "../router.js";
import { brand, engineGate } from "../parts.js";
import { themeDialog } from "../dialogs.js";
import { startDemo } from "./demo.js";
import { currentSession, hasSaved } from "../classic/session.js";

export function homeScreen(app) {
  const demo = startDemo();
  const gates = [];
  app.append(h("div.home", {},
    h("header.home-head", {}, brand(), h("button.icon-btn", { "aria-label": "Tema", onclick: themeDialog }, icon("palette"))),
    h("div.home-board", {}, demo.el),
    h("nav.home-menu", {}, menu(gates), h("a.home-link", { href: "/org/" }, "Poliléxico para Android")),
  ));
  return () => (demo.stop(), gates.forEach((stop) => stop()));
}

function gated(gates, button) {
  gates.push(engineGate(button));
  return button;
}

function menu(gates) {
  return [
    resumeButton(gates),
    gated(gates, h("button.btn.block", { onclick: () => go("clasica") }, "Clásica")),
    h("button.btn.block", { onclick: () => go("en-linea") }, icon("globe", 22), "En línea"),
    soon("Duplicada"),
    soon("Minijuegos"),
    soon("Analizador"),
  ];
}

function resumeButton(gates) {
  if (!currentSession()?.live() && !hasSaved()) return null;
  return gated(gates, h("button.btn.block.tonal", { onclick: () => go("partida") }, "Volver a la partida"));
}

function soon(label) {
  return h("button.btn.block.outlined", { disabled: true }, label, h("span.badge", {}, "Pronto"));
}
