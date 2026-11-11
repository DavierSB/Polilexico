import { h, replace } from "./dom.js";
import { load, save } from "./store.js";
import { onlinePanel } from "./online/lobby.js";
import { onPresence, onlineLabel } from "./online/presence.js";

export function modeTabs(mode, bots) {
  let current = load(`tab.${mode}`, "online");
  let panel = null;
  const onlineTab = h("button.mode-tab", { onclick: () => show("online") }, "En línea");
  const botsTab = h("button.mode-tab", { onclick: () => show("bots") }, "Bots");
  const body = h("div.mode-body");
  const botsView = bots();
  const stopPresence = onPresence((n) => (onlineTab.textContent = onlineLabel(n)));
  const show = (tab) => {
    current = tab;
    save(`tab.${mode}`, tab);
    onlineTab.classList.toggle("on", tab === "online");
    botsTab.classList.toggle("on", tab === "bots");
    panel?.destroy();
    panel = tab === "online" ? onlinePanel(mode === "duplicate") : null;
    replace(body, panel ? panel.el : botsView.el);
  };
  show(current);
  return {
    el: h("div.stack.mode-tabs-wrap", {}, h("div.mode-tabs", { role: "tablist" }, onlineTab, botsTab), body),
    destroy: () => (stopPresence(), panel?.destroy(), botsView.destroy?.()),
  };
}
