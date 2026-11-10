import { h, replace } from "../dom.js";
import { icon } from "../icons.js";
import { confirm, themeDialog } from "../dialogs.js";
import { formatClock } from "../parts.js";
import { LETTERS } from "../tiles.js";
import { face } from "../classic/rack.js";
import { rulesIconButton } from "../classic/options.js";
import { resultCard, outcomeBanner, resultSection, livesRow } from "../results.js";
import { ANAGRAM_RULES } from "./rules.js";

const LOW_TIME_MS = 30000;
const REPLIES = { invalid: ["No es válida: pierdes una vida.", "error"], repeated: ["Esa ya la encontraste.", "muted"], found: ["¡Bien! Sigue buscando.", "hit"] };
const BANNERS = {
  solved: ["¡RESUELTA!", "Mano completa", true],
  gave_up: ["TE RENDISTE", "Pierdes una vida", false],
  no_lives: ["SIN VIDAS", "Fin de la serie", false],
  timeout: ["SE ACABÓ EL TIEMPO", "Fin de la serie", false],
};

export function anagramView(session, exit) {
  const bar = h("div.game-bar");
  const clock = h("div.turn-clock");
  const body = h("div.recall-body");
  const lives = h("div.recall-lives");
  const el = h("div.screen.recall-screen", {}, bar, clock, body, lives);
  const ui = { session, exit, bar, clock, body, key: null, attempt: null, reply: null };
  const stop = session.subscribe(() => draw(ui, lives));
  const onKey = (e) => keys(ui, e);
  document.addEventListener("keydown", onKey);
  return { el, destroy: () => (stop(), session.close(), document.removeEventListener("keydown", onKey)) };
}

function draw(ui, lives) {
  const s = ui.session;
  ui.clock.textContent = formatClock(s.remainingMs);
  ui.clock.classList.toggle("low", s.remainingMs < LOW_TIME_MS);
  replace(lives, livesRow(s.lives, s.config.lives, "Resueltas", s.solved, s.best));
  const key = JSON.stringify([s.hand, s.stage.name, s.stage.found?.length, s.paused, s.lives]);
  if (key === ui.key) return;
  ui.key = key;
  drawBar(ui);
  const stages = { loading, solving, revealed, finished, failed };
  replace(ui.body, stages[s.stage.name](ui));
}

function drawBar(ui) {
  const s = ui.session;
  replace(ui.bar,
    h("button.text-btn", { onclick: ui.exit }, "Salir"),
    h("span.bar-title.grow", {}, "Anagrama"),
    s.stage.name === "solving" ? h("button.icon-btn", { "aria-label": "Pausa", onclick: () => s.togglePause() }, icon(s.paused ? "play" : "pause")) : null,
    rulesIconButton("Reglas de Anagrama", ANAGRAM_RULES),
    h("button.icon-btn", { "aria-label": "Tema", onclick: themeDialog }, icon("palette")),
  );
}

function loading() {
  return h("div.searching", {}, h("div.spinner"), h("span", {}, "Preparando la mano…"));
}

function failed(ui) {
  return h("div.stack", {}, h("p.error", {}, `No se pudo preparar la mano: ${ui.session.stage.message}`), h("button.btn", { onclick: () => ui.session.startHand() }, "Reintentar"));
}

function solving(ui) {
  const s = ui.session;
  if (s.paused) return h("div.card.pause-card", {}, h("p", {}, "En pausa"), h("button.btn", { onclick: () => s.togglePause() }, "Seguir"));
  const p = s.stage.puzzle;
  if (ui.attempt?.puzzle !== p) ui.attempt = { puzzle: p, slots: p.tiles.map(() => null) };
  const card = h("div.word-card");
  const pool = h("div.pool");
  const buttons = h("div.row.attempt-buttons");
  ui.redraw = () => drawAttempt(ui, card, pool, buttons);
  ui.redraw();
  return [card, pool, buttons];
}

function drawAttempt(ui, card, pool, buttons) {
  const s = ui.session;
  const a = ui.attempt;
  const found = s.stage.found;
  replace(card,
    h("span.small-caps.word-count", {}, `MANO ${s.hand}`),
    h("div.tile-line", {}, letters(a).map((l, i) => slot(ui, l, i))),
    ui.reply ? h("span", { class: "small reply-" + REPLIES[ui.reply][1] }, REPLIES[ui.reply][0]) : null,
    found.length ? h("b.small", {}, "Encontradas: " + found.map((f) => f.join("")).join("  ")) : null,
  );
  replace(pool, h("div.tile-line", {}, a.puzzle.rack.map((l, i) => poolTile(ui, l, i))));
  replace(buttons,
    h("button.btn.grow", { disabled: !complete(a), onclick: () => send(ui) }, "Enviar"),
    h("button.btn.tonal.grow", { onclick: () => ((a.slots = a.slots.map(() => null)), ui.redraw()) }, "Recoger"),
    h("button.btn.outlined.grow", { onclick: () => giveUp(ui) }, "Rendirse"),
  );
}

function letters(a) {
  return a.puzzle.tiles.map((t, i) => (a.puzzle.fixed.has(i) ? t : a.slots[i] === null ? null : a.puzzle.rack[a.slots[i]]));
}

function slot(ui, letter, i) {
  const fixed = ui.attempt.puzzle.fixed.has(i);
  if (letter === null) return h("div.empty-slot");
  return h("button", { class: "rack-tile line-tile" + (fixed ? " fixed" : ""), disabled: fixed, onclick: () => clear(ui, i) }, face(letter));
}

function poolTile(ui, letter, i) {
  const used = ui.attempt.slots.includes(i);
  return h("button", { class: "rack-tile line-tile" + (used ? " used" : ""), disabled: used, onclick: () => place(ui, i) }, face(letter));
}

function place(ui, i) {
  const a = ui.attempt;
  const free = a.slots.findIndex((v, k) => v === null && !a.puzzle.fixed.has(k));
  if (free < 0 || a.slots.includes(i)) return;
  a.slots[free] = i;
  ui.reply = null;
  ui.redraw();
}

function clear(ui, k) {
  ui.attempt.slots[k] = null;
  ui.redraw();
}

function complete(a) {
  return a.slots.every((v, k) => v !== null || a.puzzle.fixed.has(k));
}

function send(ui) {
  const a = ui.attempt;
  ui.reply = ui.session.submit(letters(a));
  if (ui.reply === "solved") return (ui.reply = null);
  a.slots = a.slots.map(() => null);
  ui.key = null;
  ui.session.changed();
}

async function giveUp(ui) {
  if (await confirm("¿Rendirse? Pierdes una vida.", "Rendirse", "Seguir")) ui.session.giveUp();
}

function revealed(ui) {
  const s = ui.session.stage;
  return [banner(s.outcome), h("button.btn.block", { onclick: () => ui.session.nextHand() }, "Siguiente mano"), answers(s.puzzle, s.found, "Tenía")];
}

function finished(ui) {
  const s = ui.session;
  const why = s.stage.outcome === "timeout" ? "¡TIEMPO!" : "SIN VIDAS";
  const chips = s.record ? [[s.record.isNew ? "¡Nuevo récord!" : `Récord: ${s.record.best} ${s.record.best === 1 ? "mano" : "manos"}`, s.record.isNew]] : [];
  return [
    resultCard(why, String(s.solved), s.solved === 1 ? "mano resuelta" : "manos resueltas", chips),
    h("div.row.result-buttons", {}, h("button.btn.grow", { onclick: () => s.restart() }, "Otra serie"), h("button.btn.outlined.grow", { onclick: ui.exit }, "Salir")),
    s.stage.puzzle ? answers(s.stage.puzzle, s.stage.found, "La última mano:") : null,
  ];
}

function banner(outcome) {
  const [title, main, good] = BANNERS[outcome];
  return outcomeBanner(title, main, "", good);
}

function answers(puzzle, found, lead) {
  const n = puzzle.answers.length;
  return [
    resultSection(`${lead} ${n === 1 ? "1 anagrama" : `${n} anagramas`}`, "Letras fijas, subrayadas"),
    h("div.stack.answer-list", {}, puzzle.answers.map((w) => h("div.word-row", {},
      h("span", { class: "word-mark " + (found.some((f) => f.join() === w.join()) ? "hit" : "none") }, found.some((f) => f.join() === w.join()) ? "✓" : "·"),
      h("b.grow", {}, w.map((l, i) => (puzzle.fixed.has(i) ? h("u", {}, l) : l))),
    ))),
  ];
}

function keys(ui, e) {
  if (ui.session.stage.name !== "solving" || ui.session.paused || e.ctrlKey || e.metaKey || e.altKey || document.querySelector(".scrim")) return;
  const a = ui.attempt;
  if (e.key === "Enter" && complete(a)) return send(ui);
  if (e.key === "Backspace") {
    for (let k = a.slots.length - 1; k >= 0; k--) if (a.slots[k] !== null) return clear(ui, k);
    return;
  }
  const letter = e.key.toUpperCase();
  if (!LETTERS.includes(letter)) return;
  const i = a.puzzle.rack.findIndex((l, k) => l === letter && !a.slots.includes(k));
  if (i >= 0) place(ui, i);
}
