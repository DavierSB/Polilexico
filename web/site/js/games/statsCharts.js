import { h } from "../dom.js";
import { openDialog, actions } from "../dialogs.js";

const STEPS = [10, 20, 25, 50, 100, 200, 250, 500];
const MAX_TICKS = 5;
const NS = "http://www.w3.org/2000/svg";
const SIDE = 300;
const LEFT = 34;
const BOTTOM = 22;

export function dotStrip(games, label, unit = "") {
  const scale = scaleOf(games.map((g) => g.value), unit === "%" ? 100 : null);
  const x = (v) => fraction(scale, v) * 100;
  const dot = (g, i) => h("button.strip-dot", { "aria-label": g.title, style: { left: x(g.value) + "%", top: `calc(${50 + jitter(i) * 30}% - 11px)` }, onclick: () => gameDialog(g) });
  return h("div.stack.chart-box", {},
    h("div.strip", {},
      h("div.strip-plot", {}, games.map(dot)),
      h("div.strip-axis", {}, ticksOf(scale).map((v) => h("span.strip-tick", { style: { left: x(v) + "%" } }, `${v}${unit}`))),
    ),
    h("span.chart-caption", {}, label),
  );
}

export function scoreScatter(games) {
  const scale = scaleOf(games.flatMap((g) => [g.myScore, g.opponentScore]));
  const bottom = SIDE - BOTTOM;
  const side = Math.min(SIDE - LEFT, bottom);
  const px = (v) => LEFT + side * fraction(scale, v);
  const py = (v) => bottom - side * fraction(scale, v);
  const svg = el("svg", { class: "scatter", viewBox: `0 0 ${SIDE} ${SIDE}` },
    ticksOf(scale).map((v) => [
      el("line", { x1: px(v), x2: px(v), y1: bottom, y2: bottom - side, class: "chart-grid" }),
      el("line", { x1: LEFT, x2: px(scale.max), y1: py(v), y2: py(v), class: "chart-grid" }),
      el("text", { x: px(v), y: bottom + 15, class: "chart-text", "text-anchor": "middle" }, String(v)),
      el("text", { x: 0, y: py(v) + 4, class: "chart-text" }, String(v)),
    ]),
    el("line", { x1: px(scale.min), y1: py(scale.min), x2: px(scale.max), y2: py(scale.max), class: "chart-diagonal" }),
    el("text", { x: px(scale.max) - 60, y: bottom - 8, class: "chart-text" }, "Victoria"),
    el("text", { x: LEFT + 6, y: bottom - side + 14, class: "chart-text" }, "Derrota"),
    games.map((g) => {
      const c = el("circle", { cx: px(g.myScore), cy: py(g.opponentScore), r: 6, class: "chart-dot" });
      c.addEventListener("click", () => gameDialog(g));
      return c;
    }),
  );
  return h("div.stack.chart-box", {}, svg, h("span.chart-caption", {}, "X: tus puntos · Y: puntos del rival"));
}

function gameDialog(g) {
  openDialog((close) => [h("h2", {}, g.title), h("div.stack.game-lines", {}, g.lines.map((l) => h("span", {}, l))), actions(h("button.text-btn", { onclick: close }, "Cerrar"))]);
}

function scaleOf(values, cap = null) {
  const lo = values.length ? Math.min(...values) : 0;
  const hi = values.length ? Math.max(...values) : 100;
  const air = Math.max(20, Math.trunc((hi - lo) / 10));
  const min = Math.max(0, Math.floor((lo - air) / 10) * 10);
  let max = Math.max(Math.ceil((hi + air) / 10) * 10, min + 10);
  if (cap !== null) max = Math.min(max, cap);
  return { min, max };
}

function ticksOf({ min, max }) {
  const step = STEPS.find((s) => (max - min) / s <= MAX_TICKS) ?? 1000;
  const out = [];
  for (let v = Math.ceil(min / step) * step; v <= max; v += step) out.push(v);
  return out;
}

function fraction({ min, max }, v) {
  return (v - min) / (max - min);
}

function jitter(i) {
  return (((i * 37) % 7) - 3) / 3;
}

function el(name, attrs, ...children) {
  const e = document.createElementNS(NS, name);
  Object.entries(attrs).forEach(([k, v]) => e.setAttribute(k, v));
  children.flat(Infinity).forEach((c) => c && e.append(typeof c === "string" ? document.createTextNode(c) : c));
  return e;
}
