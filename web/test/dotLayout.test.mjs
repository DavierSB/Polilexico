import assert from "node:assert/strict";
import fs from "node:fs";

const source = fs.readFileSync(new URL("../site/js/games/dotLayout.js", import.meta.url), "utf8");
const { layoutDots } = await import("data:text/javascript," + encodeURIComponent(source));
const frame = { left: 0, right: 400, bottom: 88, step: 16, labelHeight: 16 };

const lone = layoutDots([50, 200], [10, 40], frame);
assert.deepEqual(lone.spots.map((s) => s.x), [50, 200]);
assert.equal(lone.labels.length, 0);

const row = layoutDots([200, 200, 200], [90, 90, 90], frame);
assert.deepEqual(row.spots.map((s) => s.x), [184, 200, 216]);
assert.deepEqual(row.labels, [{ low: 90, high: 90, x: 200, bottom: 72 }]);

const near = layoutDots([100, 108], [87, 88], frame);
assert.deepEqual(near.labels, [{ low: 87, high: 88, x: 104, bottom: 72 }]);

const stacked = layoutDots(Array(7).fill(200), Array(7).fill(100), frame);
assert.deepEqual([...new Set(stacked.spots.map((s) => s.y))], [80, 64]);
assert.equal(stacked.labels[0].bottom, 56);

const xs = [...Array(40).fill(400), ...Array(5).fill(395)];
const crowd = layoutDots(xs, xs, frame);
assert.ok(crowd.spots.every((s) => s.x >= 0 && s.x <= 400 && s.y - 8 >= 16));
assert.ok(minDistance(crowd.spots) >= 15.99);
assert.ok(minDistance(layoutDots([100, 100, 100, 100, 140, 140, 140], [1, 1, 1, 1, 2, 2, 2], frame).spots) >= 15.99);

function minDistance(spots) {
  return Math.min(...spots.flatMap((a, i) => spots.slice(i + 1).map((b) => Math.hypot(a.x - b.x, a.y - b.y))));
}

console.log("dotLayout: ok");
