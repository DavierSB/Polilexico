import assert from "node:assert/strict";
import path from "node:path";
import { fileURLToPath } from "node:url";

const JS = path.join(path.dirname(fileURLToPath(import.meta.url)), "..", "site", "js");
const { Placer } = await import(path.join(JS, "classic", "placer.js"));

const letters = (p) => p.slots.map((s) => s.letter).join("");

function rack() {
  const p = new Placer();
  p.sync([], ["A", "B", "C", "D"]);
  return p;
}

const swapped = rack();
swapped.clickSlot(0);
swapped.clickSlot(2);
assert.equal(letters(swapped), "CBAD");
assert.equal(swapped.selected, null);

const again = rack();
again.clickSlot(1);
again.clickSlot(1);
assert.equal(letters(again), "ABCD");
assert.equal(again.selected, null);

const aimed = rack();
aimed.clickSquare(112);
aimed.clickSlot(0);
await aimed.clickSlot(1);
assert.equal(letters(aimed), "ABCD");
assert.equal(aimed.pending.size, 2);

console.log("atril: 0 fallos");
