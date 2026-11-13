import { h, replace } from "../dom.js";
import { openDialog, actions } from "../dialogs.js";

const FULL = 100;
const WIDTH = 320;
const HEIGHT = 170;
const TOP = 10;
const BAR_SHARE = 0.6;
const STEPS = [5, 10, 25];
const MAX_LINES = 4;
const NS = "http://www.w3.org/2000/svg";

export function efficiencyOf(mine, master) {
  const turns = mine.map((m, i) => (master[i] > 0 ? Math.min(FULL, (m * FULL) / master[i]) : null));
  let got = 0;
  let best = 0;
  const running = mine.map((m, i) => ((got += m), (best += master[i]), best > 0 ? (got * FULL) / best : FULL));
  return { turns, running };
}

export function efficiencyDialog(efficiency, { selected = null, pick = null, extra = null } = {}) {
  let view = "running";
  const chart = h("div");
  const chips = h("div.chips");
  const draw = (close) => {
    replace(chips, chip("Acumulada", view === "running", () => ((view = "running"), draw(close))), chip("Jugada a jugada", view === "turns", () => ((view = "turns"), draw(close))));
    replace(chart, efficiencyChart(efficiency, view, selected, pick && ((i) => (pick(i), close()))));
  };
  openDialog((close) => {
    draw(close);
    return [h("h2", {}, "Eficiencia"), chips, chart, extra?.(close) ?? null, actions(h("button.text-btn", { onclick: close }, "Cerrar"))];
  });
}

function chip(label, on, onclick) {
  return h("button", { class: "chip" + (on ? " on" : ""), onclick }, label);
}

export function efficiencyChart(efficiency, view, selected = null, pick = null) {
  const floor = view === "running" ? floorOf(efficiency.running) : 0;
  const y = (v) => HEIGHT - ((v - floor) / (FULL - floor)) * (HEIGHT - TOP);
  const slot = WIDTH / Math.max(1, efficiency.turns.length);
  const center = (i) => slot * (i + 0.5);
  const svg = svgEl("svg", { class: "efficiency-chart", viewBox: `0 0 ${WIDTH} ${HEIGHT}`, preserveAspectRatio: "none" },
    selected === null ? null : svgEl("rect", { x: center(selected) - slot / 2, y: 0, width: slot, height: HEIGHT, class: "efficiency-band" }),
    linesOf(floor).map((v) => svgEl("line", { x1: 0, x2: WIDTH, y1: y(v), y2: y(v), class: "spread-grid" })),
    svgEl("line", { x1: 0, x2: WIDTH, y1: HEIGHT, y2: HEIGHT, class: "spread-grid solid" }),
    view === "turns" ? bars(efficiency, slot, center, y) : svgEl("path", { d: runningPath(efficiency, center, y), class: "spread-line" }),
  );
  const area = h("div.efficiency-area", {}, svg, view === "running" ? runningEnd(efficiency, center, y) : null);
  if (pick && efficiency.turns.length) picking(svg, efficiency.turns.length, pick);
  return h("div.efficiency-box", {}, h("div.efficiency-axis", {}, linesOf(floor).map((v) => h("span", { style: { top: (y(v) / HEIGHT) * 100 + "%" } }, v))), area);
}

function floorOf(running) {
  const low = running.length ? Math.min(...running) : 0;
  return Math.min(Math.max(Math.floor((low - 5) / 10) * 10, 0), 90);
}

function linesOf(floor) {
  const step = STEPS.find((s) => (FULL - floor) / s <= MAX_LINES) ?? 25;
  const out = [];
  for (let v = FULL; v >= floor; v -= step) out.push(v);
  return out;
}

function bars(efficiency, slot, center, y) {
  const width = slot * BAR_SHARE;
  return efficiency.turns.map((p, i) => {
    const left = center(i) - width / 2;
    if (p === null) return svgEl("line", { x1: left, x2: left + width, y1: HEIGHT - 1, y2: HEIGHT - 1, class: "efficiency-empty" });
    return svgEl("rect", { x: left, y: y(p), width, height: HEIGHT - y(p), class: p === FULL ? "efficiency-hit" : "efficiency-miss" });
  });
}

function runningPath(efficiency, center, y) {
  return efficiency.running.map((v, i) => `${i ? "L" : "M"}${center(i)} ${y(v)}`).join(" ");
}

function runningEnd(efficiency, center, y) {
  const last = efficiency.running.at(-1);
  if (last === undefined) return null;
  const left = (center(efficiency.running.length - 1) / WIDTH) * 100;
  const top = (y(last) / HEIGHT) * 100;
  const text = last.toLocaleString("es", { minimumFractionDigits: 1, maximumFractionDigits: 1 }) + " %";
  return [
    h("span.spread-dot.efficiency-end", { style: { left: left + "%", top: top + "%" } }),
    h("b.efficiency-result", { style: { right: 100 - left + "%", top: `calc(${top}% ${top < 15 ? "+ 6px" : "- 22px"})` } }, text),
  ];
}

function picking(svg, count, pick) {
  svg.addEventListener("click", (e) => {
    const box = svg.getBoundingClientRect();
    pick(Math.min(Math.max(Math.floor(((e.clientX - box.left) / box.width) * count), 0), count - 1));
  });
}

function svgEl(name, attrs, ...children) {
  const el = document.createElementNS(NS, name);
  Object.entries(attrs).forEach(([k, v]) => el.setAttribute(k, v));
  children.flat(Infinity).forEach((c) => c && el.append(c));
  return el;
}
