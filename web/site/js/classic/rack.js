import { h } from "../dom.js";
import { rackValue } from "../tiles.js";
import { draggable } from "./drag.js";

export function createRack(placer) {
  const el = h("div.rack");
  return { el, render: (enabled) => render(el, placer, enabled) };
}

function render(el, placer, enabled) {
  const used = placer.usedSlots();
  el.replaceChildren(...placer.slots.map((slot) => rackTile(slot, placer, used.has(slot.id), enabled)));
}

function rackTile(slot, placer, used, enabled) {
  const classes = ["rack-tile", used ? "used" : "", placer.selected === slot.id ? "selected" : "", placer.exchanging?.has(slot.id) ? "marked" : ""];
  const el = h("button", { class: classes.join(" "), "data-slot": slot.id, disabled: used && !placer.exchanging, onclick: () => placer.clickSlot(slot.id) },
    face(slot.letter),
  );
  draggable(el, {
    canStart: () => enabled && !used && !placer.exchanging,
    ghost: () => h("div.rack-tile", {}, face(slot.letter)),
    drop: (target) => dropSlot(placer, slot, target),
  });
  return el;
}

export function face(letter) {
  const blank = letter === "?";
  return [
    h("span", { class: "rack-letter" + (letter.length > 1 ? " digraph" : "") }, blank ? "" : letter),
    blank ? null : h("span.rack-value", {}, rackValue(letter)),
  ];
}

function dropSlot(placer, slot, target) {
  if (target.square) return placer.put(slot.id, Number(target.square.dataset.index));
  if (target.slot) return placer.reorder(slot.id, Number(target.slot.dataset.slot));
  if (target.rack) placer.reorder(slot.id, null);
}
