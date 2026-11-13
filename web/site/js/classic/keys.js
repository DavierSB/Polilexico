import { SIZE } from "../tiles.js";

const MOVES = { ArrowLeft: [0, -1], ArrowRight: [0, 1], ArrowUp: [-1, 0], ArrowDown: [1, 0] };

export function bindKeys(view) {
  const onKey = (e) => {
    if (e.ctrlKey || e.metaKey || e.altKey || typing(e.target) || document.querySelector(".scrim")) return;
    if (handle(view, e)) e.preventDefault();
  };
  document.addEventListener("keydown", onKey);
  return () => document.removeEventListener("keydown", onKey);
}

function typing(target) {
  return target instanceof HTMLInputElement || target instanceof HTMLTextAreaElement;
}

function handle(view, e) {
  const p = view.placer;
  if (!view.canArrange()) return false;
  if (e.key === "Enter") return view.canPlay() && (p.exchanging ? view.exchange() || true : view.play() || true);
  if (e.key === "Escape") return escape(p);
  if (e.key === "Backspace") return p.backspace();
  if (e.key === " ") return turnArrow(p);
  if (MOVES[e.key]) return moveArrow(p, MOVES[e.key]);
  if (e.key.length === 1) return p.type(e.key);
  return false;
}

function escape(p) {
  if (p.exchanging) p.cancelExchange();
  else if (p.pending.size) p.recall();
  else if (p.arrow) {
    p.arrow = null;
    p.changed();
  }
  return true;
}

function turnArrow(p) {
  if (!p.arrow) return false;
  p.arrow = { ...p.arrow, vertical: !p.arrow.vertical };
  p.changed();
  return true;
}

function moveArrow(p, [dr, dc]) {
  const from = p.arrow?.index ?? 7 * SIZE + 7;
  const row = Math.min(SIZE - 1, Math.max(0, Math.floor(from / SIZE) + (p.arrow ? dr : 0)));
  const col = Math.min(SIZE - 1, Math.max(0, (from % SIZE) + (p.arrow ? dc : 0)));
  p.arrow = { index: row * SIZE + col, vertical: dr !== 0 ? true : dc !== 0 ? false : p.arrow?.vertical ?? false };
  p.changed();
  return true;
}
