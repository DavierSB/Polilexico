import { h, replace } from "../dom.js";
import { icon } from "../icons.js";
import { go } from "../router.js";
import { createBoard } from "../board.js";
import { floatScore } from "../motion.js";
import { themeDialog } from "../dialogs.js";
import { applyPlacement, parsePlacement, placedSquares } from "../placement.js";
import { emptyBoard, LETTERS } from "../tiles.js";
import { face } from "../classic/rack.js";
import { rulesIconButton } from "../classic/options.js";
import { resultCard, resultSection, wordRow, livesRow } from "../results.js";
import { RECALL_RULES } from "./rules.js";

const FINAL_PAUSE_MS = 2000;
const HIT_PAUSE_MS = 900;
const MISSED = { invalid: "No es válida", not_played: "La palabra que se jugó no fue esa", forgotten: null };

export function recallView(session, exit) {
  const body = h("div.recall-body");
  const lives = h("div.recall-lives");
  const el = h("div.screen.recall-screen", {},
    h("header.screen-head", {},
      h("button.icon-btn", { "aria-label": "Volver", onclick: exit }, icon("back")),
      h("h1", {}, "¿Cuántas recuerdas?"),
      rulesIconButton("Reglas de ¿Cuántas recuerdas?", RECALL_RULES),
      h("button.icon-btn", { "aria-label": "Tema", onclick: themeDialog }, icon("palette")),
    ),
    body, lives,
  );
  const ui = { session, el, body, exit, stageKey: null, attempt: null, timer: null };
  const stop = session.subscribe(() => draw(ui, lives));
  const onKey = (e) => keys(ui, e);
  document.addEventListener("keydown", onKey);
  return { el, destroy: () => (stop(), clearTimeout(ui.timer), (ui.dead = true), document.removeEventListener("keydown", onKey)) };
}

function draw(ui, lives) {
  const s = ui.session;
  replace(lives, livesRow(s.lives, s.config.lives, "Recordadas", s.recalled, s.best));
  const key = JSON.stringify([s.round, s.stage.name, s.stage.index, s.stage.verdict]);
  if (key === ui.stageKey) return;
  ui.stageKey = key;
  ui.el.dataset.stage = s.stage.name;
  const stages = { loading, watching, solving, round_done: roundDone, series_done: seriesDone, failed };
  replace(ui.body, stages[s.stage.name](ui));
}

function loading() {
  return h("div.searching", {}, h("div.spinner"), h("span", {}, "Preparando la partida…"));
}

function failed(ui) {
  return h("div.stack", {}, h("p.error", {}, `No se pudo preparar la partida: ${ui.session.stage.message}`), h("button.btn", { onclick: () => ui.session.startRound() }, "Reintentar"));
}

function watching(ui) {
  const game = ui.session.stage.game;
  const count = h("b.watch-count", {}, "0");
  const bar = h("div");
  const board = createBoard();
  board.render({ cells: emptyBoard() });
  replay(ui, game, board, (n) => ((count.textContent = n), (bar.style.width = `${(n / game.length) * 100}%`)));
  return [
    h("div.watch-head", {}, h("div.row", {}, h("span.memorize.grow", {}, "MEMORIZA"), count, h("span.muted", {}, ` / ${game.length}`)), h("div.progress", {}, bar)),
    h("div.recall-board", {}, board.el),
    h("p.muted", {}, "Fíjate en las palabras que más puntos hacen: luego tendrás que armarlas."),
  ];
}

async function replay(ui, game, board, progress) {
  let cells = emptyBoard();
  for (const [n, { placement, score }] of game.entries()) {
    await wait(ui.session.config.intervalMs);
    if (ui.dead || ui.session.stage.game !== game) return;
    const p = parsePlacement(placement);
    if (!p) continue;
    cells = applyPlacement(cells, p);
    const squares = placedSquares(p);
    board.render({ cells, latest: new Set(squares) });
    floatScore(board, cells, squares, score);
    progress(n + 1);
  }
  await wait(FINAL_PAUSE_MS);
  if (!ui.dead) ui.session.watched();
}

function solving(ui) {
  const s = ui.session;
  const stage = s.stage;
  const word = s.words[stage.index];
  if (!stage.verdict) ui.attempt = { word, slots: word.tiles.map(() => null), rejected: false, checking: false };
  if (stage.verdict === "hit") ui.timer = setTimeout(() => s.advance(), HIT_PAUSE_MS);
  const card = h("div.word-card");
  const pool = h("div.pool");
  const buttons = h("div.row.attempt-buttons");
  const redraw = () => drawAttempt(ui, card, pool, buttons);
  ui.redraw = redraw;
  redraw();
  return [card, stage.verdict ? null : pool, buttons];
}

function drawAttempt(ui, card, pool, buttons) {
  const s = ui.session;
  const { index, verdict } = s.stage;
  const a = ui.attempt;
  const letters = verdict ? a.word.tiles : slotLetters(a);
  replace(card,
    h("span.small-caps.word-count", {}, `PALABRA ${index + 1} DE ${s.words.length}`),
    h("div.tile-line", {}, letters.map((l, i) => slot(ui, l, i, !verdict))),
    feedback(a, verdict),
  );
  if (!verdict) replace(pool, h("div.tile-line", {}, a.word.scrambled.map((l, i) => poolTile(ui, l, i))));
  replace(buttons, ...actionButtons(ui, verdict));
}

function slotLetters(a) {
  return a.word.tiles.map((t, i) => (a.word.fixed.has(i) ? t : a.slots[i] === null ? null : a.word.scrambled[a.slots[i]]));
}

function slot(ui, letter, i, open) {
  const fixed = ui.attempt.word.fixed.has(i);
  if (letter === null) return h("div.empty-slot");
  return h("button", { class: "rack-tile line-tile" + (fixed ? " fixed" : ""), disabled: !open || fixed, onclick: () => clearSlot(ui, i) }, face(letter));
}

function poolTile(ui, letter, i) {
  const used = ui.attempt.slots.includes(i);
  return h("button", { class: "rack-tile line-tile" + (used ? " used" : ""), disabled: used, onclick: () => place(ui, i) }, face(letter));
}

function place(ui, i) {
  const a = ui.attempt;
  const free = a.slots.findIndex((v, k) => v === null && !a.word.fixed.has(k));
  if (free < 0 || a.slots.includes(i)) return;
  a.slots[free] = i;
  a.rejected = false;
  ui.redraw();
}

function clearSlot(ui, k) {
  ui.attempt.slots[k] = null;
  ui.attempt.rejected = false;
  ui.redraw();
}

function complete(a) {
  return a.slots.every((v, k) => v !== null || a.word.fixed.has(k));
}

function feedback(a, verdict) {
  if (verdict === "hit") return h("b.verdict-text.hit", {}, "✓  ¡Esa es!");
  if (verdict) return h("div.stack.missed", {}, MISSED[verdict] ? h("b.verdict-text.miss", {}, MISSED[verdict]) : null, h("b", {}, `Era ${a.word.text}`));
  return a.rejected ? h("span.error.small", {}, "No es válida. Corrígela tocando sus fichas.") : null;
}

function actionButtons(ui, verdict) {
  const a = ui.attempt;
  if (verdict === "hit") return [];
  if (verdict) return [h("button.btn.grow", { onclick: () => ui.session.advance() }, "Siguiente")];
  return [
    h("button.btn.grow", { disabled: !complete(a) || a.checking || a.rejected, onclick: () => send(ui) }, "Enviar"),
    h("button.text-btn.grow", { disabled: a.checking, onclick: () => ui.session.forget() }, "No la recuerdo"),
  ];
}

async function send(ui) {
  const a = ui.attempt;
  a.checking = true;
  const result = await ui.session.submit(slotLetters(a));
  a.checking = false;
  if (result === "rejected") a.rejected = true;
  if (ui.session.stage.verdict === null || result === "rejected") ui.redraw();
}

function keys(ui, e) {
  const s = ui.session;
  if (s.stage.name !== "solving" || e.ctrlKey || e.metaKey || e.altKey || document.querySelector(".scrim")) return;
  const a = ui.attempt;
  if (s.stage.verdict) return e.key === "Enter" && s.stage.verdict !== "hit" && s.advance();
  if (e.key === "Enter" && complete(a)) return send(ui);
  if (e.key === "Backspace") return undoLast(ui);
  const letter = e.key.toUpperCase();
  if (!LETTERS.includes(letter)) return;
  const i = a.word.scrambled.findIndex((l, k) => l === letter && !a.slots.includes(k));
  if (i >= 0) place(ui, i);
}

function undoLast(ui) {
  const a = ui.attempt;
  for (let k = a.slots.length - 1; k >= 0; k--) if (a.slots[k] !== null) return clearSlot(ui, k);
}

function roundDone(ui) {
  const s = ui.session;
  const hits = s.answers.filter((x) => x.hit);
  const points = `${hits.reduce((t, x) => t + x.word.score, 0)} de ${s.answers.reduce((t, x) => t + x.word.score, 0)} pts`;
  return [
    resultCard("RECORDASTE", String(hits.length), `/ ${s.words.length}`, [[points, hits.length > 0], [s.lives === 1 ? "1 vida" : `${s.lives} vidas`, false]], s.words.length ? hits.length / s.words.length : 0),
    h("div.row.result-buttons", {}, h("button.btn.grow", { onclick: () => s.nextRound() }, "Siguiente partida"), h("button.btn.outlined.grow", { onclick: ui.exit }, "Salir")),
    answerList(s.answers, s.words.slice(s.answers.length)),
  ];
}

function seriesDone(ui) {
  const s = ui.session;
  const chips = s.record ? [[s.record.isNew ? "¡Nuevo récord!" : `Récord: ${s.record.best} ${s.record.best === 1 ? "palabra" : "palabras"}`, s.record.isNew]] : [];
  return [
    resultCard("EN LA SERIE", String(s.recalled), `/ ${s.asked}`, chips, s.asked ? s.recalled / s.asked : 0),
    h("div.row.result-buttons", {}, h("button.btn.grow", { onclick: () => s.restart() }, "Otra serie"), h("button.btn.outlined.grow", { onclick: () => go("minijuegos") }, "Menú")),
    answerList(s.answers, []),
  ];
}

function answerList(answers, unanswered) {
  return [
    resultSection("Las palabras de la partida"),
    h("div.stack.answer-list", {},
      [...answers].sort((a, b) => b.word.score - a.word.score).map((x) => wordRow(x.word.text, { score: x.word.score, mark: x.hit ? "hit" : "miss" })),
      [...unanswered].sort((a, b) => b.score - a.score).map((w) => wordRow(w.text, { score: w.score, mark: "none" })),
    ),
  ];
}

function wait(ms) {
  return new Promise((r) => setTimeout(r, ms));
}
