import { h, replace } from "../dom.js";
import { go } from "../router.js";
import { screenHead } from "../parts.js";
import { call, whenReady } from "../engine.js";
import { readWord, tokens, LETTERS } from "../tiles.js";
import { face } from "../classic/rack.js";
import { dictionaryText, fixedOptions, scramble, shuffle } from "../recall/words.js";
import { withStudy, chips, big, progressBar } from "./shell.js";
import { tilesOf, keyOf, wordEntry } from "./keys.js";
import { findList } from "./listScreens.js";
import { saveList } from "./lists.js";

const REPLIES = {
  learned: ["¡Bien!", "hit"], already: ["Válida, pero ya está aprendida", "hit"], outside: ["Válida, pero no está en esta lista", "hit"],
  repeated: ["Esa ya la encontraste.", "muted"], invalid: ["No es válida.", "error"],
};

export function testScreen(app, [id]) {
  return withStudy(app, (data) => {
    const list = findList(data, id);
    if (!list) return go("listas", true);
    const ui = { data, list, filter: list.spec.filter(), skipped: new Set(), hand: 0, stage: { name: "loading" }, reply: null, slots: [] };
    ui.progress = h("div.stack");
    ui.body = h("div.recall-body");
    app.append(h("div.screen.recall-screen", {}, screenHead(list.name, "lista/" + encodeURIComponent(id)), ui.progress, ui.body));
    const onKey = (e) => keys(ui, e);
    document.addEventListener("keydown", onKey);
    startHand(ui);
    return () => document.removeEventListener("keydown", onKey);
  });
}

function draw(ui) {
  const l = ui.list;
  replace(ui.progress, h("div.row.big-line", {}, big(`${l.percent}%`), h("span.muted", {}, `${l.learned.size} de ${l.members.length} aprendidas`)), progressBar(l.percent));
  const s = ui.stage;
  const stages = { loading: () => h("div.searching", {}, h("div.spinner")), solving, revealed, completed, no_hands: noHands };
  replace(ui.body, stages[s.name](ui));
}

async function startHand(ui) {
  if (ui.list.isComplete) return ((ui.stage = { name: "completed" }), draw(ui));
  ui.stage = { name: "loading" };
  draw(ui);
  await whenReady();
  const hand = await nextHand(ui);
  ui.reply = null;
  if (!hand) ui.stage = { name: "no_hands" };
  else {
    ui.hand++;
    ui.slots = hand.tiles.map(() => null);
    ui.stage = { name: "solving", hand, found: [] };
  }
  draw(ui);
}

async function nextHand(ui) {
  for (const key of shuffle(ui.list.pending.filter((k) => !ui.skipped.has(k)))) {
    const unit = ui.data.lexicon.find(key);
    const hand = unit && (await handOf(unit, ui));
    if (hand) return hand;
    ui.skipped.add(key);
  }
  return null;
}

async function handOf(unit, ui) {
  const form = pickForm(ui.data.lexicon.forms(unit), ui.filter);
  if (!form) return null;
  const tiles = tilesOf(form);
  const all = unique(tokens(await call("anagramTiles", dictionaryText(tiles))).map((w) => readWord(w).filter(Boolean).map((t) => t.letter)));
  if (!all.some((a) => a.join() === tiles.join())) return null;
  const count = Math.max(0, tiles.length - 7);
  const options = await fixedOptions(tiles, count, requiredFixed(ui.filter, tiles.length));
  if (!options.length) return null;
  const fixed = options[Math.floor(Math.random() * options.length)];
  const answers = all.filter((a) => [...fixed].every((i) => a[i] === tiles[i]));
  return { unit, tiles, fixed, rack: scramble(tiles.filter((_, i) => !fixed.has(i))), answers };
}

function requiredFixed(filter, n) {
  const out = new Set();
  for (const [i] of filter.fixed ?? []) if (i < n) out.add(i);
  for (const [i] of filter.fixedEnd ?? []) if (i < n) out.add(n - 1 - i);
  for (let i = 0; i < Math.min(filter.start?.length ?? 0, n); i++) out.add(i);
  for (let i = 0; i < Math.min(filter.end?.length ?? 0, n); i++) out.add(n - 1 - i);
  return out.size < n ? out : new Set();
}

function pickForm(forms, filter) {
  const candidates = forms.filter((f) => filter.matches(f.head));
  if (!candidates.length) return null;
  const weight = (f) => 2 ** (Math.max(0, f.permille) / 100);
  let ticket = Math.random() * candidates.reduce((t, f) => t + weight(f), 0);
  return (candidates.find((c) => (ticket -= weight(c)) < 0) ?? candidates.at(-1)).head;
}

function unique(words) {
  const seen = new Set();
  return words.filter((w) => !seen.has(w.join()) && seen.add(w.join()));
}

function solving(ui) {
  const { hand, found } = ui.stage;
  const n = hand.answers.length;
  return [
    h("div.row", {}, h("b.grow.hand-title", {}, `Mano ${ui.hand}`), chips(`${found.length} de ${n} ${n === 1 ? "anagrama" : "anagramas"}`)),
    h("div.tile-line", {}, letters(ui).map((l, i) => slot(ui, l, i))),
    h("div.tile-line.test-pool", {}, hand.rack.map((l, i) => h("button", { class: "rack-tile line-tile" + (ui.slots.includes(i) ? " used" : ""), disabled: ui.slots.includes(i), onclick: () => place(ui, i) }, face(l)))),
    h("div.stack.center.feedback", {},
      ui.reply ? h("b", { class: "reply-" + REPLIES[ui.reply][1] }, REPLIES[ui.reply][0]) : null,
      found.length ? h("b.reply-hit", {}, found.map((f) => f.join("")).join("   ")) : null,
    ),
    h("div.stack.test-buttons", {},
      h("button.btn.block", { disabled: !complete(ui), onclick: () => send(ui) }, "Enviar"),
      h("div.row", {},
        h("button.btn.tonal.grow", { onclick: () => ((ui.slots = ui.slots.map(() => null)), draw(ui)) }, "Recoger"),
        h("button.btn.outlined.grow", { onclick: () => ((ui.stage = { name: "revealed", hand, found }), draw(ui)) }, "Ver respuestas"),
      ),
    ),
  ];
}

function letters(ui) {
  const { hand } = ui.stage;
  return hand.tiles.map((t, i) => (hand.fixed.has(i) ? t : ui.slots[i] === null ? null : hand.rack[ui.slots[i]]));
}

function slot(ui, letter, i) {
  const fixed = ui.stage.hand.fixed.has(i);
  if (letter === null) return h("div.empty-slot");
  return h("button", { class: "rack-tile line-tile" + (fixed ? " fixed" : ""), disabled: fixed, onclick: () => ((ui.slots[i] = null), draw(ui)) }, face(letter));
}

function place(ui, i) {
  const free = ui.slots.findIndex((v, k) => v === null && !ui.stage.hand.fixed.has(k));
  if (free < 0 || ui.slots.includes(i)) return;
  ui.slots[free] = i;
  ui.reply = null;
  draw(ui);
}

function complete(ui) {
  return ui.slots.every((v, k) => v !== null || ui.stage.hand.fixed.has(k));
}

function send(ui) {
  const s = ui.stage;
  const word = letters(ui);
  const key = word.join();
  ui.slots = ui.slots.map(() => null);
  if (!s.hand.answers.some((a) => a.join() === key)) ui.reply = "invalid";
  else if (s.found.some((f) => f.join() === key)) ui.reply = "repeated";
  else ui.reply = hit(ui, word);
  draw(ui);
}

function hit(ui, word) {
  const s = ui.stage;
  const before = ui.list.learned.size;
  const keys = ui.data.lexicon.unitsOf(keyOf(word)).map((u) => u.key);
  ui.list = ui.list.learn(keys);
  saveList(ui.list);
  const found = [...s.found, word];
  ui.stage = found.length === s.hand.answers.length ? { name: "revealed", hand: s.hand, found } : { ...s, found };
  if (ui.list.learned.size > before) return "learned";
  return keys.some((k) => ui.list.has(k)) ? "already" : "outside";
}

function revealed(ui) {
  const { hand, found } = ui.stage;
  const n = hand.answers.length;
  return [
    h("b.reveal-title", {}, `${n === 1 ? "Tenía un solo anagrama" : `Tenía ${n} anagramas`}; encontraste ${found.length}.`),
    h("button.btn.block", { onclick: () => startHand(ui) }, "Siguiente mano"),
    h("div.stack", {}, hand.answers.map((w) => {
      const ok = found.some((f) => f.join() === w.join());
      return h("button.answer-row", { onclick: () => go("diccionario/" + encodeURIComponent(wordEntry(keyOf(w)))) }, h("b", { class: ok ? "reply-hit" : "muted" }, ok ? "✓" : "·"), h("span", {}, w.join("")));
    })),
  ];
}

function completed(ui) {
  return [
    h("b.reveal-title.reply-hit", {}, "¡Lista completada!"),
    h("p", {}, `Todas las ${ui.list.spec.unit} de la lista están aprendidas.`),
    h("button.btn.block", { onclick: () => ((ui.list = ui.list.restart()), saveList(ui.list), ui.skipped.clear(), startHand(ui)) }, "Empezar de nuevo"),
    h("button.btn.outlined.block", { onclick: () => go("lista/" + encodeURIComponent(ui.list.id)) }, "Salir"),
  ];
}

function noHands(ui) {
  return [h("p", {}, "No quedan manos que se puedan formar con lo que falta de esta lista."), h("button.btn.outlined.block", { onclick: () => go("lista/" + encodeURIComponent(ui.list.id)) }, "Salir")];
}

function keys(ui, e) {
  if (ui.stage.name !== "solving" || e.ctrlKey || e.metaKey || e.altKey || document.querySelector(".scrim")) return;
  if (e.key === "Enter" && complete(ui)) return send(ui);
  if (e.key === "Backspace") {
    for (let k = ui.slots.length - 1; k >= 0; k--) if (ui.slots[k] !== null) return ((ui.slots[k] = null), draw(ui));
    return;
  }
  const letter = e.key.toUpperCase();
  if (!LETTERS.includes(letter)) return;
  const i = ui.stage.hand.rack.findIndex((l, k) => l === letter && !ui.slots.includes(k));
  if (i >= 0) place(ui, i);
}
