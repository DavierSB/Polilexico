import { h, replace } from "../dom.js";
import { openDialog } from "../dialogs.js";
import { createBoard } from "../board.js";
import { icon } from "../icons.js";
import { at } from "../placement.js";
import { BLANK } from "../tiles.js";
import { scoreBar } from "./reviewParts.js";

const STEP_MS = 1400;

export function planDialog(position, players, lineNow) {
  const state = { step: 0, playing: true, timer: null };
  const board = createBoard();
  const top = h("div.stack.plan-top");
  const bottom = h("div.stack.plan-bottom");
  const draw = () => drawPlan({ position, players, line: lineNow(), board, top, bottom, state, draw, close });
  const close = openDialog((done) => [
    h("div.row.plan-title", {}, h("button.icon-btn", { "aria-label": "Volver", onclick: done }, icon("back")), h("h2.ellipsis", {}, `Plan: ${lineNow().move.text}`)),
    h("div.stack.plan", {}, top, h("div.plan-board", {}, board.el), bottom),
  ], { wide: true, onClose: () => clearTimeout(state.timer) });
  draw();
}

function drawPlan(v) {
  const { line, state } = v;
  const last = line.steps.length - 1;
  const shown = Math.min(state.step, last);
  const step = line.steps[shown];
  const tally = talliesOf(v.position, v.players, line)[shown];
  const pending = new Map();
  const p = step.move.placement;
  if (p) p.tiles.forEach((t, i) => t && !step.board[at(p, i)] && pending.set(at(p, i), t));
  v.board.render({ cells: step.board, pending });
  const bar = (player) => scoreBar(v.players[player], tally.scores[player], tally.racks[player], step.who === v.players[player]);
  replace(v.top, bar(1 - v.position.me));
  replace(v.bottom, bar(v.position.me), controls(v, shown, last), line.steps.map((s, i) => stepRow(v, s, i, i === shown)), ending(line));
  clearTimeout(state.timer);
  if (state.playing) state.timer = setTimeout(() => advance(v, shown, last), STEP_MS);
}

function advance(v, shown, last) {
  if (shown < last) v.state.step = shown + 1;
  else v.state.playing = false;
  v.draw();
}

function controls(v, shown, last) {
  const go = (i) => ((v.state.step = Math.min(Math.max(i, 0), last)), (v.state.playing = false), v.draw());
  const toggle = () => {
    v.state.playing = !v.state.playing;
    if (v.state.playing && shown === last) v.state.step = 0;
    v.draw();
  };
  return h("div.row.turn-nav", {},
    h("button.btn.small.outlined", { disabled: shown === 0, onclick: () => go(shown - 1) }, "‹"),
    h("button.text-btn.grow", { onclick: toggle }, v.state.playing ? "Pausa" : "Reproducir"),
    h("button.btn.small.outlined", { disabled: shown >= last, onclick: () => go(shown + 1) }, "›"),
  );
}

function stepRow(v, s, i, active) {
  const go = () => ((v.state.step = i), (v.state.playing = false), v.draw());
  return h("button", { class: "plan-step" + (active ? " on" : ""), onclick: go },
    h("span.plan-number", {}, i + 1), h("span.plan-who.ellipsis", {}, s.who), h("b.grow.ellipsis", {}, s.move.text), h("b.plan-points", {}, s.move.score));
}

function ending(line) {
  if (line.pending) return h("p.muted.small", {}, "Calculando el resto del plan…");
  return [h("hr.plan-rule"), line.ending.map(endingLine), outcomeLine(winner(line), Math.abs(line.spread), null)];
}

export function endingLine(row) {
  return h("div.row.ending-line", {},
    h("span.plan-who.wide.ellipsis", {}, row.who),
    h("span.grow.ellipsis", {}, row.left.length ? "Se queda con " + row.left.join(" ") : "Cierra"),
    h("b.plan-points", {}, (row.delta > 0 ? "+" : "") + row.delta));
}

export function outcomeLine(winner, margin, me) {
  const text = margin === 0 || !winner ? "Empate" : winner === me || winner === "Tú" ? "Ganas tú por" : `Gana ${winner} por`;
  return h("div.row.outcome-line", {}, h("b.grow", {}, text), h("b.plan-points.outcome-margin", {}, margin));
}

function winner(line) {
  if (line.spread > 0) return line.mover;
  if (line.spread < 0) return line.ending.map((r) => r.who).find((w) => w !== line.mover);
  return null;
}

function talliesOf(position, players, line) {
  let tally = { scores: position.scores.slice(), racks: position.racks.map((r) => r.slice()) };
  const played = line.steps.map((s) => (tally = after(tally, players.indexOf(s.who), s)));
  if (line.pending || !played.length) return played;
  const end = played.at(-1);
  played[played.length - 1] = { ...end, scores: end.scores.map((sc, i) => sc + line.ending.filter((r) => r.who === players[i]).reduce((t, r) => t + r.delta, 0)) };
  return played;
}

function after(tally, player, step) {
  const scores = tally.scores.map((s, i) => (i === player ? s + step.move.score : s));
  const racks = tally.racks.map((r, i) => (i === player ? without(r, spent(step)) : r));
  return { scores, racks };
}

function spent(step) {
  const p = step.move.placement;
  if (!p) return [];
  return p.tiles.map((t, i) => (t && !step.board[at(p, i)] ? (t.blank ? BLANK : t.letter.toUpperCase()) : null)).filter(Boolean);
}

function without(rack, tiles) {
  const left = rack.slice();
  tiles.forEach((t) => {
    const i = left.indexOf(t);
    if (i >= 0) left.splice(i, 1);
  });
  return left;
}
