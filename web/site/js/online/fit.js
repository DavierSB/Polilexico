import { h, replace } from "../dom.js";
import { openDialog, actions } from "../dialogs.js";

const FULL_ROW = 64;
const COMPACT_ROW = 40;
const GAME_ROW = 42;
const TITLE = 28;
const TOP_PAD = 8;

export function fitArea() {
  const el = h("section.lobby-col.lobby-lower");
  let spec = null;
  const draw = () => spec && replace(el, layout(spec, el.clientHeight - TOP_PAD));
  const observer = new ResizeObserver(draw);
  observer.observe(el);
  const show = (next) => {
    spec = next;
    const games = next.games.items.length > 0;
    const people = !!next.people?.items.length;
    el.style.flexBasis = (games ? TITLE + GAME_ROW * 2 : 0) + (people ? TITLE + FULL_ROW : 0) + TOP_PAD + "px";
    el.classList.toggle("hidden", !games && !people);
    draw();
  };
  return { el, show, stop: () => observer.disconnect() };
}

function layout(spec, height) {
  const games = spec.games.items;
  const people = spec.people?.items ?? [];
  const gamesHeight = !people.length ? height : Math.min(TITLE + GAME_ROW * games.length, Math.max(height / 2, height - (TITLE + FULL_ROW * people.length)));
  return [
    games.length ? h("div.lower-games", { style: { height: gamesHeight + "px" } }, ongoing(spec.games, gamesHeight)) : null,
    people.length ? h("div.lower-people", {}, h("h3.lower-title", {}, spec.people.title), fittedPeople(spec.people, height - (games.length ? gamesHeight : 0) - TITLE - TOP_PAD)) : null,
  ];
}

function ongoing(spec, height) {
  const fit = Math.max(1, Math.floor((height - TITLE) / GAME_ROW));
  const shown = spec.items.length <= fit ? spec.items.length : Math.max(1, fit - 1);
  return [
    h("h3.lower-title", {}, spec.title),
    h("div.lower-rows", {}, spec.items.slice(0, shown).map(spec.row)),
    shown < spec.items.length ? moreButton(spec.items.length - shown, () => listDialog(spec.title, spec.items.map(spec.row))) : null,
  ];
}

function fittedPeople(spec, height) {
  const people = spec.items;
  if (FULL_ROW * people.length <= height) return h("div.stack.people-full", {}, people.map(spec.row));
  if (COMPACT_ROW * Math.ceil(people.length / 2) <= height) return compactGrid(people, spec.compact);
  const shown = Math.max(0, Math.floor((height - COMPACT_ROW) / COMPACT_ROW)) * 2;
  return [compactGrid(people.slice(0, shown), spec.compact), moreButton(people.length - shown, () => listDialog(spec.title, people.map(spec.row)))];
}

function compactGrid(people, compact) {
  return h("div.compact-grid", {}, people.map(compact));
}

function moreButton(hidden, onclick) {
  return h("button.text-btn.more-btn", { onclick }, `${hidden} más`);
}

export function listDialog(title, rows) {
  openDialog((close) => {
    const list = h("div.stack.dialog-list", {}, rows);
    list.addEventListener("click", (e) => e.target.closest("button") && close());
    return [h("h2", {}, title), list, actions(h("button.text-btn", { onclick: close }, "Cerrar"))];
  }, { wide: true });
}
