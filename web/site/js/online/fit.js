import { h, replace } from "../dom.js";
import { openDialog, actions } from "../dialogs.js";

const ROW = 70;
const TITLE = 36;
const EMPTY = 110;
const MORE = 48;
const GAP = 16;

export function fitArea() {
  const el = h("section.lobby-col.lobby-lower");
  let specs = [];
  const draw = () => replace(el, layout(specs, el.clientHeight));
  const observer = new ResizeObserver(draw);
  observer.observe(el);
  const show = (next) => {
    specs = next.filter((s) => s.items.length || s.empty);
    el.style.flexBasis = reserve(specs) + "px";
    el.classList.toggle("hidden", !specs.length);
    draw();
  };
  return { el, show, stop: () => observer.disconnect() };
}

export function fullSection(spec) {
  if (!spec.items.length && !spec.empty) return null;
  return h("div.lobby-section", {}, h("h3.side-title", {}, spec.title), spec.items.length ? h("div.stack", {}, spec.items.map(spec.row)) : spec.empty());
}

function reserve(specs) {
  return specs.reduce((sum, s) => sum + TITLE + Math.min(s.items.length, 1) * ROW + (s.items.length ? 0 : EMPTY), 0);
}

function layout(specs, height) {
  const counts = allot(specs, height);
  return specs.map((s, i) => fitted(s, counts[i]));
}

function allot(specs, height) {
  const counts = specs.map((s) => s.items.length);
  const fixed = specs.reduce((sum, s) => sum + TITLE + (s.items.length ? 0 : EMPTY), 0) + GAP * (specs.length - 1);
  let shown = counts;
  for (let pass = 0, mores = 0; pass < 3; pass++) {
    shown = split(counts, Math.floor((height - fixed - mores * MORE) / ROW));
    mores = shown.filter((n, i) => n < counts[i]).length;
  }
  return shown;
}

function split(counts, rows) {
  if (counts.length < 2) return counts.map((n) => Math.min(n, Math.max(1, rows)));
  const [a, b] = counts;
  const first = Math.min(a, Math.max(Math.ceil(rows / 2), rows - b));
  return [Math.max(Math.min(a, 1), first), Math.max(Math.min(b, 1), Math.min(b, rows - first))];
}

function fitted(spec, shown) {
  if (!spec.items.length) return fullSection(spec);
  const hidden = spec.items.length - shown;
  return h("div.lobby-section", {},
    h("h3.side-title", {}, spec.title),
    h("div.stack", {}, spec.items.slice(0, shown).map(spec.row)),
    hidden ? h("button.text-btn.more-btn", { onclick: () => listDialog(spec) }, `Mostrar ${hidden} más`) : null,
  );
}

function listDialog(spec) {
  openDialog((close) => {
    const list = h("div.stack.dialog-list", {}, spec.items.map(spec.row));
    list.addEventListener("click", (e) => e.target.closest("button") && close());
    return [h("h2", {}, spec.title), list, actions(h("button.text-btn", { onclick: close }, "Cerrar"))];
  }, { wide: true });
}
