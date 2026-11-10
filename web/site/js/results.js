import { h } from "./dom.js";

export function resultCard(label, value, caption, chips = [], progress = null) {
  return h("div.result-card", {},
    h("span.small-caps", {}, label),
    h("div.row.result-value", {}, h("b", {}, value), h("span", {}, caption)),
    progress === null ? null : h("div.progress.result-progress", {}, h("div", { style: { width: Math.round(progress * 100) + "%" } })),
    chips.length ? h("div.row.result-chips", {}, chips.map(([text, on]) => h("span", { class: "result-chip" + (on ? " on" : "") }, text))) : null,
  );
}

export function outcomeBanner(title, main, detail, good) {
  return h("div", { class: "outcome-banner" + (good ? " good" : " bad") },
    h("span.outcome-mark", {}, good ? "✓" : "✗"),
    h("div.grow", {}, h("span.small-caps", {}, title), h("div.row", {}, h("b.outcome-main", {}, main), detail ? h("span.muted", {}, detail) : null)),
  );
}

export function resultSection(title, hint = null) {
  return h("div.row.result-section", {}, h("span.small-caps.grow", {}, title.toUpperCase()), hint ? h("span.muted.small", {}, hint) : null);
}

export function wordRow(word, { tag = null, score = null, selected = false, onclick = null, mark = null } = {}) {
  return h(onclick ? "button" : "div", { class: "word-row" + (selected ? " selected" : ""), onclick },
    mark ? h("span", { class: "word-mark " + mark }, mark === "hit" ? "✓" : mark === "miss" ? "✗" : "·") : null,
    h("b.grow", {}, word.toUpperCase()),
    tag ? h("span.muted.small", {}, tag) : null,
    score !== null ? h("span.word-score", {}, score) : null,
  );
}

export function livesRow(lives, maxLives, label, solved, best) {
  return h("div.lives-row", {},
    h("div.row.grow", {}, Array.from({ length: maxLives }, (_, i) => h("span", { class: "heart" + (i < lives ? " full" : "") }, i < lives ? "♥" : "♡"))),
    h("div.lives-side", {}, h("b", {}, `${label}: ${solved}`), recordToBeat(solved, best)),
  );
}

function recordToBeat(solved, best) {
  if (!best) return null;
  return solved > best ? h("span.record-new", {}, "¡Nuevo récord!") : h("span.muted.small", {}, `Récord: ${best}`);
}
