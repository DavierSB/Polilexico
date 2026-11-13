import { h, replace } from "../dom.js";
import { go } from "../router.js";
import { screenHead, engineLoading } from "../parts.js";
import { icon } from "../icons.js";
import { confirm } from "../dialogs.js";
import { listSaved, savedProgress, abandonSaved } from "../savedGames.js";
import { gameTitle, scoreText } from "./list.js";

const FOLDERS = {
  classic: { title: "Clásicas en curso", back: "clasica", game: "partida/" },
  duplicate: { title: "Duplicadas en curso", back: "duplicada", game: "duplicada-partida/" },
};
const HOLD_MS = 500;
const press = { timer: null, held: false };
const DAY = new Intl.DateTimeFormat("es", { day: "numeric", month: "short" });
const TIME = new Intl.DateTimeFormat("es", { hour: "2-digit", minute: "2-digit", hourCycle: "h23" });

export function inProgressScreen(app, [mode]) {
  const ui = { mode: FOLDERS[mode] ? mode : "classic", items: null, selected: new Set(), head: h("div"), body: h("div.stack.saved-list") };
  ui.folder = FOLDERS[ui.mode];
  app.append(h("div.screen", {}, ui.head, ui.body));
  const stop = showItems(ui);
  const onKey = (e) => e.key === "Escape" && ui.selected.size && clear(ui);
  document.addEventListener("keydown", onKey);
  return () => (stop(), document.removeEventListener("keydown", onKey));
}

function showItems(ui) {
  const loading = engineLoading();
  replace(ui.head, screenHead(ui.folder.title, ui.folder.back));
  replace(ui.body, loading.card);
  readItems(ui.mode).then((items) => ((ui.items = items), loading.stop(), draw(ui)));
  return loading.stop;
}

async function readItems(mode) {
  return Promise.all(listSaved(mode).map(async (game) => itemOf(game, await savedProgress(game))));
}

function itemOf(game, p) {
  const item = { id: game.id, detail: savedAt(game.updatedAt), title: titleOf(game.mode, p), score: null, note: null };
  if (!p) return item;
  if (game.mode === "duplicate") return { ...item, score: `${p.HumanTotal}/${p.MasterTotal}`, note: `Ronda ${p.Round}` };
  return { ...item, score: scoreText({ Mode: game.mode, MyScore: p.HumanTotal, OpponentScore: p.BotTotal, HumanStarts: p.HumanStarts }) };
}

function titleOf(mode, p) {
  return gameTitle({ Mode: mode, Opponent: p?.BotName ?? null, Local: !!p?.Local, Me: p?.HumanName });
}

function savedAt(at) {
  const date = new Date(at);
  return `Guardada el ${DAY.format(date)}, ${TIME.format(date)}`;
}

function draw(ui) {
  replace(ui.head, ui.selected.size ? selectionHead(ui) : screenHead(ui.folder.title, ui.folder.back));
  replace(ui.body, ui.items.length ? rows(ui) : h("div.empty-state", {}, h("b", {}, "No hay partidas sin terminar.")));
}

function selectionHead(ui) {
  const count = ui.selected.size;
  return h("header.screen-head", {},
    h("button.icon-btn", { "aria-label": "Cancelar", onclick: () => clear(ui) }, icon("close")),
    h("h1", {}, count === 1 ? "1 seleccionada" : `${count} seleccionadas`),
    h("button.icon-btn", { "aria-label": "Borrar", title: "Borrar", onclick: () => remove(ui) }, icon("trash")),
  );
}

function rows(ui) {
  const hint = ui.selected.size ? null : h("p.muted.small", {}, "Mantén pulsada una partida para seleccionarla.");
  return [hint, ui.items.map((item) => row(ui, item))];
}

function row(ui, item) {
  const checked = ui.selected.has(item.id);
  const el = h("div", { class: "list-card saved-row" + (checked ? " selected" : ""), role: "button", tabindex: 0 },
    ui.selected.size ? h("input.saved-check", { type: "checkbox", checked, tabindex: -1, "aria-label": item.title }) : null,
    h("div.grow.stack", {}, h("b.list-name", {}, item.title), h("span.muted.small", {}, item.detail)),
    item.score ? h("div.stack.saved-score", {}, h("b", {}, item.score), item.note ? h("span.result-chip", {}, item.note) : null) : null,
  );
  pressable(el, () => (ui.selected.size ? toggle(ui, item.id) : go(ui.folder.game + item.id, true)), () => toggle(ui, item.id));
  return el;
}

function pressable(el, onTap, onHold) {
  const cancel = () => clearTimeout(press.timer);
  const hold = () => press.held || ((press.held = true), cancel(), onHold());
  el.addEventListener("pointerdown", (e) => ((press.held = false), cancel(), e.button === 0 && (press.timer = setTimeout(hold, HOLD_MS))));
  ["pointerup", "pointerleave", "pointercancel"].forEach((name) => el.addEventListener(name, cancel));
  el.addEventListener("contextmenu", (e) => (e.preventDefault(), hold()));
  el.addEventListener("click", () => (press.held ? (press.held = false) : onTap()));
  el.addEventListener("keydown", (e) => e.key === "Enter" && onTap());
}

function toggle(ui, id) {
  if (ui.selected.has(id)) ui.selected.delete(id);
  else ui.selected.add(id);
  draw(ui);
}

function clear(ui) {
  ui.selected.clear();
  draw(ui);
}

async function remove(ui) {
  const count = ui.selected.size;
  if (!(await confirm(deleteQuestion(count), "Borrar", "No"))) return;
  ui.selected.forEach(abandonSaved);
  ui.items = ui.items.filter((item) => !ui.selected.has(item.id));
  clear(ui);
}

function deleteQuestion(count) {
  if (count === 1) return "¿Borrar la partida seleccionada? No se podrá continuar y contará como abandonada.";
  return `¿Borrar las ${count} partidas seleccionadas? No se podrán continuar y contarán como abandonadas.`;
}
