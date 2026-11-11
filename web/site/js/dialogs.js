import { h, append } from "./dom.js";
import { THEMES, choose, chooseLight, swatches, theme } from "./theme.js";
import { soundsOn, setSounds } from "./sound.js";

const layer = () => document.getElementById("layer");

export function openDialog(build, { wide = false, dismissable = true, onClose = null } = {}) {
  const box = h("div", { class: "dialog" + (wide ? " wide" : ""), role: "dialog", "aria-modal": "true" });
  const scrim = h("div.scrim", {}, box);
  const close = () => {
    if (!scrim.isConnected) return;
    scrim.remove();
    document.removeEventListener("keydown", onKey, true);
    onClose?.();
  };
  const onKey = (e) => {
    if (e.key !== "Escape" || !dismissable) return;
    e.stopPropagation();
    close();
  };
  scrim.addEventListener("click", (e) => dismissable && e.target === scrim && close());
  document.addEventListener("keydown", onKey, true);
  append(box, [build(close)]);
  layer().append(scrim);
  box.querySelector("[autofocus]")?.focus();
  return close;
}

export function confirm(question, yes, no = "No") {
  return new Promise((resolve) => {
    openDialog((close) => [
      h("p", {}, question),
      actions(
        h("button.text-btn", { onclick: () => (close(), resolve(false)) }, no),
        h("button.text-btn", { autofocus: true, onclick: () => (close(), resolve(true)) }, yes),
      ),
    ]);
  });
}

export function actions(...buttons) {
  return h("div.dialog-actions", {}, buttons);
}

export function toast(text, ms = 3500) {
  const el = h("div.toast", { role: "status" }, text);
  layer().append(el);
  setTimeout(() => el.remove(), ms);
}

export function themeDialog() {
  openDialog((close) => [
    h("h2", {}, "Tema"),
    h("div.stack", {}, THEMES.map((name) => themeChoice(name, close))),
    lightSwitch(),
    h("label.row.theme-choice", {}, h("span.grow", {}, "Sonidos"), h("input.switch", { type: "checkbox", checked: soundsOn(), onchange: (e) => setSounds(e.target.checked) })),
    actions(h("button.text-btn", { onclick: close }, "Cerrar")),
  ]);
}

function themeChoice(name, close) {
  const selected = theme().name === name;
  return h("label.row.theme-choice", {},
    h("input", { type: "radio", name: "theme", checked: selected, onchange: () => (choose(name), close()) }),
    h("span.grow", {}, name),
    h("span.row", {}, swatches(name).map((c) => h("span.dot", { style: { background: c } }))),
  );
}

function lightSwitch() {
  return h("label.row.theme-choice", {},
    h("span.grow", {}, "Tablero claro"),
    h("input.switch", { type: "checkbox", checked: theme().light, onchange: (e) => chooseLight(e.target.checked) }),
  );
}
