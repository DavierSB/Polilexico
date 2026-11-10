import { BLANK, LETTERS, SIZE } from "../tiles.js";
import { placementText, writePlacement } from "../placement.js";

const DIGRAPH_OF = { C: "CH", L: "LL", R: "RR" };

export class Placer {
  constructor(onChange) {
    this.onChange = onChange;
    this.cells = [];
    this.slots = [];
    this.pending = new Map();
    this.order = [];
    this.arrow = null;
    this.selected = null;
    this.exchanging = null;
  }

  sync(cells, rack) {
    this.cells = cells;
    const key = [...rack].sort().join(" ");
    if (key !== this.rackKey) {
      this.rackKey = key;
      return this.reset(rack);
    }
    if ([...this.pending.keys()].some((i) => cells[i])) this.recall();
  }

  reset(rack) {
    this.slots = rack.map((letter, i) => ({ id: i, letter }));
    this.clear();
  }

  clear() {
    this.pending.clear();
    this.order = [];
    this.arrow = null;
    this.selected = null;
    this.exchanging = null;
    this.changed();
  }

  changed() {
    this.onChange?.();
  }

  usedSlots() {
    return new Set([...this.pending.values()].map((p) => p.slot));
  }

  freeSlots() {
    const used = this.usedSlots();
    return this.slots.filter((s) => !used.has(s.id));
  }

  placement() {
    const tiles = new Map([...this.pending].map(([i, p]) => [i, p.tile]));
    return writePlacement(this.cells, tiles);
  }

  placementInput() {
    const p = this.placement();
    return p ? placementText(p) : null;
  }

  clickSquare(i) {
    if (this.exchanging) return;
    if (this.pending.has(i)) return this.take(i);
    if (this.cells[i]) return;
    if (this.selected !== null) return this.put(this.selected, i);
    this.toggleArrow(i);
  }

  toggleArrow(i) {
    if (this.arrow?.index !== i) this.arrow = { index: i, vertical: false };
    else if (!this.arrow.vertical) this.arrow = { index: i, vertical: true };
    else this.arrow = null;
    this.changed();
  }

  clickSlot(id) {
    if (this.exchanging) return this.toggleExchange(id);
    if (this.usedSlots().has(id)) return;
    if (this.arrow) return this.put(id, this.arrow.index);
    this.selected = this.selected === id ? null : id;
    this.changed();
  }

  async put(slotId, index, blankLetter = null) {
    const slot = this.slots.find((s) => s.id === slotId);
    if (!slot || this.cells[index] || this.pending.has(index)) return;
    const letter = slot.letter === BLANK ? blankLetter ?? (await this.askBlank?.()) : slot.letter;
    if (!letter) return;
    this.pending.set(index, { slot: slotId, tile: { letter, blank: slot.letter === BLANK } });
    this.order.push(index);
    this.selected = null;
    this.advanceFrom(index);
    this.changed();
  }

  advanceFrom(index) {
    if (this.arrow?.index !== index) return;
    const step = this.arrow.vertical ? SIZE : 1;
    let next = index + step;
    while (this.inLine(next, index) && (this.cells[next] || this.pending.has(next))) next += step;
    this.arrow = this.inLine(next, index) ? { ...this.arrow, index: next } : null;
  }

  inLine(next, from) {
    if (next < 0 || next >= SIZE * SIZE) return false;
    return this.arrow.vertical || Math.floor(next / SIZE) === Math.floor(from / SIZE);
  }

  take(index) {
    this.pending.delete(index);
    this.order = this.order.filter((i) => i !== index);
    this.changed();
  }

  type(key) {
    if (!this.arrow || this.exchanging) return false;
    const letter = key.toUpperCase();
    if (!LETTERS.includes(letter)) return false;
    const slot = this.slotFor(letter);
    if (!slot) return false;
    this.put(slot.id, this.arrow.index, slot.letter === BLANK ? letter : null);
    return true;
  }

  slotFor(letter) {
    const free = this.freeSlots();
    return free.find((s) => s.letter === letter) ?? free.find((s) => s.letter === DIGRAPH_OF[letter]) ?? free.find((s) => s.letter === BLANK);
  }

  backspace() {
    const last = this.order.at(-1);
    if (last === undefined) return false;
    const vertical = this.arrow?.vertical ?? this.lineIsVertical();
    this.take(last);
    this.arrow = { index: last, vertical };
    this.changed();
    return true;
  }

  lineIsVertical() {
    const [a, b] = this.order;
    return b !== undefined && Math.abs(b - a) >= SIZE;
  }

  move(fromIndex, toIndex) {
    const p = this.pending.get(fromIndex);
    if (!p || this.cells[toIndex] || this.pending.has(toIndex)) return;
    this.pending.delete(fromIndex);
    this.pending.set(toIndex, p);
    this.order = this.order.map((i) => (i === fromIndex ? toIndex : i));
    this.changed();
  }

  reorder(slotId, beforeId) {
    const from = this.slots.findIndex((s) => s.id === slotId);
    if (from < 0) return;
    const [slot] = this.slots.splice(from, 1);
    const to = beforeId === null ? this.slots.length : this.slots.findIndex((s) => s.id === beforeId);
    this.slots.splice(to < 0 ? this.slots.length : to, 0, slot);
    this.changed();
  }

  recall() {
    this.pending.clear();
    this.order = [];
    this.selected = null;
    this.changed();
  }

  shuffle() {
    for (let i = this.slots.length - 1; i > 0; i--) {
      const j = Math.floor(Math.random() * (i + 1));
      [this.slots[i], this.slots[j]] = [this.slots[j], this.slots[i]];
    }
    this.changed();
  }

  startExchange() {
    this.recall();
    this.arrow = null;
    this.exchanging = new Set();
    this.changed();
  }

  toggleExchange(id) {
    if (this.exchanging.has(id)) this.exchanging.delete(id);
    else this.exchanging.add(id);
    this.changed();
  }

  cancelExchange() {
    this.exchanging = null;
    this.changed();
  }

  exchangeInput() {
    const letters = this.slots.filter((s) => this.exchanging?.has(s.id)).map((s) => (s.letter.length > 1 ? `[${s.letter}]` : s.letter));
    return letters.length ? "cambiar " + letters.join("") : null;
  }
}
