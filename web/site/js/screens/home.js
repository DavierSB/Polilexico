import { h } from "../dom.js";
import { go } from "../router.js";
import { brand } from "../parts.js";
import { startDemo } from "./demo.js";
import { onPresence } from "../online/presence.js";
import { peopleDialog } from "../online/lobby.js";
import { account } from "../online/api.js";
import { drawerButton } from "./drawer.js";
import { online } from "../online/client.js";
import { messagesButton } from "../online/messages.js";
import { setting } from "../settings.js";

export function homeScreen(app) {
  const demo = startDemo();
  const chip = onlineChip();
  const messages = account() ? messagesButton() : null;
  const stay = stayOnline();
  app.append(h("div.home", {},
    h("header.home-head", {}, drawerButton(), brand(), h("div.row.head-actions", {}, messages?.el, chip.el)),
    h("div.home-board", {}, demo.el),
    h("nav.home-menu", {}, menu()),
  ));
  return () => (demo.stop(), chip.stop(), messages?.stop(), stay());
}

function stayOnline() {
  if (!account() || setting("hidden")) return () => {};
  online.acquire();
  return () => online.release();
}

function onlineChip() {
  const dot = h("span.status-dot");
  const label = h("b.chip-label", {}, "En línea");
  const el = h("button.online-chip", { onclick: () => (account() ? peopleDialog() : go("cuenta")) }, dot, label);
  const stop = onPresence((n) => {
    dot.classList.toggle("on", !!n);
    label.textContent = n ? `En línea · ${n}` : "En línea";
  });
  return { el, stop };
}

function menu() {
  return [
    h("button.btn.block", { onclick: () => go("clasica") }, "Clásica"),
    h("button.btn.block", { onclick: () => go("duplicada") }, "Duplicada"),
    h("button.btn.block", { onclick: () => go("minijuegos") }, "Minijuegos"),
    h("button.btn.block", { onclick: () => go("estudiar") }, "Estudiar"),
  ];
}
