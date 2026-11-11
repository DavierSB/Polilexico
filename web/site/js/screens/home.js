import { h } from "../dom.js";
import { icon } from "../icons.js";
import { go } from "../router.js";
import { brand, engineGate } from "../parts.js";
import { themeDialog } from "../dialogs.js";
import { startDemo } from "./demo.js";
import { currentSession, hasSaved } from "../classic/session.js";
import { currentDuplicate, hasSavedDuplicate } from "../duplicate/session.js";
import { onPresence } from "../online/presence.js";
import { peopleDialog } from "../online/lobby.js";
import { account } from "../online/api.js";

export function homeScreen(app) {
  const demo = startDemo();
  const gates = [];
  const chip = onlineChip();
  app.append(h("div.home", {},
    h("header.home-head", {}, brand(), h("div.row.head-actions", {}, chip.el, h("button.icon-btn", { "aria-label": "Tema", onclick: themeDialog }, icon("palette")))),
    h("div.home-board", {}, demo.el),
    h("nav.home-menu", {}, menu(gates), h("a.home-link", { href: "/org/" }, "Poliléxico para Android")),
  ));
  return () => (demo.stop(), chip.stop(), gates.forEach((stop) => stop()));
}

function onlineChip() {
  const dot = h("span.status-dot");
  const label = h("b", {}, "En línea");
  const el = h("button.online-chip", { onclick: () => (account() ? peopleDialog() : go("cuenta")) }, dot, label);
  const stop = onPresence((n) => {
    dot.classList.toggle("on", !!n);
    label.textContent = n ? `En línea · ${n}` : "En línea";
  });
  return { el, stop };
}

function gated(gates, button) {
  gates.push(engineGate(button));
  return button;
}

function menu(gates) {
  return [
    resumeButton(gates),
    h("button.btn.block", { onclick: () => go("clasica") }, "Clásica"),
    h("button.btn.block", { onclick: () => go("duplicada") }, "Duplicada"),
    h("button.btn.block", { onclick: () => go("minijuegos") }, "Minijuegos"),
    h("button.btn.block", { onclick: () => go("estudiar") }, "Estudiar"),
    h("button.btn.block.outlined", { onclick: () => go("partidas") }, "Mis partidas"),
  ];
}

function resumeButton(gates) {
  const classic = currentSession()?.live() || hasSaved();
  const duplicate = currentDuplicate()?.live() || hasSavedDuplicate();
  return [
    classic ? gated(gates, h("button.btn.block.tonal", { onclick: () => go("partida") }, "Volver a la partida")) : null,
    duplicate ? gated(gates, h("button.btn.block.tonal", { onclick: () => go("duplicada-partida") }, "Volver a la duplicada")) : null,
  ];
}
