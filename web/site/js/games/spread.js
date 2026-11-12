import { h } from "../dom.js";
import { openDialog, actions } from "../dialogs.js";

const STEP = 50;
const MAX_LINES = 3;
const GRID_STEPS = [50, 100, 200, 250, 500, 1000];
const WIDTH = 320;
const HEIGHT = 170;
const PAD = 6;
const NS = "http://www.w3.org/2000/svg";

export function spreadOf(me, opponent, meStarts, totals) {
  const lead = totals.map(([mine, theirs]) => mine - theirs);
  return meStarts ? { top: me, bottom: opponent, points: lead } : { top: opponent, bottom: me, points: lead.map((v) => -v) };
}

export function spreadDialog(spread, { selected = null, pick = null, extra = null } = {}) {
  openDialog((close) => [
    h("h2", {}, "Ventaja"),
    spreadChart(spread, selected, pick && ((i) => (pick(i), close()))),
    extra?.(close) ?? null,
    actions(h("button.text-btn", { onclick: close }, "Cerrar")),
  ]);
}

export function spreadChart(spread, selected = null, pick = null) {
  const limit = limitOf(spread.points);
  const last = spread.points.length - 1;
  const x = (i) => (last < 1 ? 0 : (WIDTH * i) / last);
  const y = (v) => HEIGHT / 2 - (v / limit) * (HEIGHT / 2 - PAD);
  const line = spread.points.map((v, i) => `${i ? "L" : "M"}${x(i)} ${y(v)}`).join(" ");
  const area = `${line} L${x(last)} ${HEIGHT / 2} L0 ${HEIGHT / 2} Z`;
  const svg = svgEl("svg", { class: "spread-chart", viewBox: `0 0 ${WIDTH} ${HEIGHT}`, preserveAspectRatio: "none" },
    svgEl("clipPath", { id: "spread-top" }, svgEl("rect", { width: WIDTH, height: HEIGHT / 2 })),
    svgEl("clipPath", { id: "spread-bottom" }, svgEl("rect", { y: HEIGHT / 2, width: WIDTH, height: HEIGHT / 2 })),
    svgEl("path", { d: area, class: "spread-top", "clip-path": "url(#spread-top)" }),
    svgEl("path", { d: area, class: "spread-bottom", "clip-path": "url(#spread-bottom)" }),
    gridLines(limit).flatMap((v) => [v, -v]).map((v) => svgEl("line", { x1: 0, x2: WIDTH, y1: y(v), y2: y(v), class: "spread-grid" })),
    svgEl("line", { x1: 0, x2: WIDTH, y1: HEIGHT / 2, y2: HEIGHT / 2, class: "spread-zero" }),
    selected === null ? null : svgEl("line", { x1: x(selected), x2: x(selected), y1: 0, y2: HEIGHT, class: "spread-mark-line" }),
    svgEl("path", { d: line, class: "spread-line" }),
  );
  const box = h("div.spread-box", {}, svg,
    h("span.spread-name.top", {}, spread.top), h("span.spread-name.bottom", {}, spread.bottom),
    resultLabel(spread.points[last], y(spread.points[last])),
    selected === null ? null : dot(x(selected), y(spread.points[Math.min(selected, last)])));
  if (pick && last >= 1) picking(svg, last, pick);
  return box;
}

function limitOf(points) {
  const most = Math.max(0, ...points.map(Math.abs));
  return Math.max(Math.ceil(most / STEP) * STEP, STEP);
}

function gridLines(limit) {
  const step = GRID_STEPS.find((s) => limit / s <= MAX_LINES) ?? limit;
  const out = [];
  for (let v = step; v < limit; v += step) out.push(v);
  return out;
}

function dot(cx, cy) {
  return h("span.spread-dot", { style: { left: (cx / WIDTH) * 100 + "%", top: (cy / HEIGHT) * 100 + "%" } });
}

function resultLabel(last, at) {
  const top = (last >= 0 ? at - 22 : at + 4) / HEIGHT;
  const clamped = Math.min(Math.max(top, 0), 1 - 18 / HEIGHT);
  return h("b.spread-result", { style: { top: clamped * 100 + "%" } }, last === 0 ? "0" : `+${Math.abs(last)}`);
}

function picking(svg, last, pick) {
  const at = (e) => {
    const box = svg.getBoundingClientRect();
    return Math.min(Math.max(Math.round(((e.clientX - box.left) / box.width) * last), 0), last);
  };
  svg.addEventListener("click", (e) => pick(at(e)));
}

function svgEl(name, attrs, ...children) {
  const el = document.createElementNS(NS, name);
  Object.entries(attrs).forEach(([k, v]) => el.setAttribute(k, v));
  children.flat(Infinity).forEach((c) => c && el.append(c));
  return el;
}
