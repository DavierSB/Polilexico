import { h } from "../dom.js";
import { openDialog, actions } from "../dialogs.js";
import { MASTER } from "./table.js";

const NS = "http://www.w3.org/2000/svg";
const WIDTH = 300;
const HEIGHT = 160;
const MAX_ALL_COLORED = 8;
const TOP_COLORED = 5;
const COLORS = ["#E53935", "#1E88E5", "#43A047", "#FB8C00", "#8E24AA", "#00ACC1", "#FDD835", "#D81B60", "#3949AB", "#7CB342", "#6D4C41", "#90A4AE"];

export function percentChartDialog(t, me, colored) {
  openDialog((close) => [h("h2", {}, "Evolución (% del Máster)"), body(t, me, colored), actions(h("button.text-btn", { onclick: close }, "Cerrar"))]);
}

function body(t, me, colored) {
  if (!t.rounds.length) return h("p", {}, "Aún no ha terminado ninguna ronda.");
  const lines = percents(t);
  const floor = Math.trunc(Math.min(80, ...Object.values(lines).flat()) / 10) * 10;
  if (!colored) return chart(floor, [...Object.entries(lines).filter(([n]) => n !== me).map(([, p]) => [p, "var(--outline)", 3]), ...(lines[me] ? [[lines[me], "var(--primary)", 6]] : [])]);
  const ranked = Object.keys(lines).sort((a, b) => lines[b].at(-1) - lines[a].at(-1));
  const shown = ranked.length > MAX_ALL_COLORED ? ranked.slice(0, TOP_COLORED) : ranked;
  const color = (n) => COLORS[shown.indexOf(n) % COLORS.length];
  const grey = ranked.filter((n) => !shown.includes(n)).map((n) => [lines[n], "var(--outline-variant)", 3, false]);
  return [
    chart(floor, [...grey, ...[...shown].reverse().map((n) => [lines[n], color(n), 4])]),
    h("div.row.chart-legend", {}, shown.map((n) => h("span.row.legend-entry", {}, h("span.legend-dot", { style: { background: color(n) } }), `${n} ${Math.trunc(lines[n].at(-1))} %`)),
      ranked.length > shown.length ? h("span.muted.small", {}, `+${ranked.length - shown.length} en gris`) : null),
  ];
}

function chart(floor, series) {
  const point = (p, i) => [p.length > 1 ? (i * WIDTH) / (p.length - 1) : WIDTH / 2, HEIGHT * (1 - (p[i] - floor) / Math.max(1, 100 - floor))];
  return h("div.percent-chart", {}, svg("svg", { viewBox: `-10 -24 ${WIDTH + 36} ${HEIGHT + 32}` },
    series.flatMap(([p, color, width, labeled = true]) => {
      const pts = p.map((_, i) => point(p, i));
      const [x, y] = pts.at(-1);
      return [
        svg("polyline", { points: pts.map((q) => q.join(",")).join(" "), fill: "none", stroke: color, "stroke-width": width / 2 }),
        svg("circle", { cx: x, cy: y, r: width * 0.8, fill: color }),
        labeled ? svg("text", { x, y: y - 8, fill: color, "font-size": 11, "text-anchor": "middle" }, `${Math.trunc(p.at(-1))} %`) : null,
      ];
    })));
}

function percents(t) {
  const names = [...new Set(t.rounds.flatMap((r) => r.entries.map((e) => e.name)))].filter((n) => n !== MASTER);
  return Object.fromEntries(names.map((n) => [n, t.rounds.map((r) => {
    const master = r.entries.find((e) => e.name === MASTER)?.total ?? 0;
    const mine = r.entries.find((e) => e.name === n)?.total ?? 0;
    return master > 0 ? (mine * 100) / master : 100;
  })]));
}

function svg(name, attrs, ...children) {
  const el = document.createElementNS(NS, name);
  Object.entries(attrs).forEach(([k, v]) => el.setAttribute(k, v));
  children.flat(Infinity).forEach((c) => c && el.append(typeof c === "string" ? document.createTextNode(c) : c));
  return el;
}
