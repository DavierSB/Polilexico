import { h } from "../dom.js";
import { openDialog, actions } from "../dialogs.js";
import { parsePlacement, spelled } from "../placement.js";
import { fullBag, tokens, parseBoard, BLANK } from "../tiles.js";
import { infoButton } from "../classic/options.js";
import { alias } from "./list.js";

const RACK = 7;
const WHO = { "Tú": "Tu jugada", "Máster": "Jugada del Máster" };
const EQUITY_INFO = "El valor de la jugada, considerando su puntuación y lo que queda en la mano.";

export function moveOf(t, m) {
  const raw = m.Description.replace(/^\(Inválida (\S+ \S+):.*\)$/, "$1");
  const placement = parsePlacement(raw);
  const equity = m.HasEquity ? m.Equity : (t.Candidates ?? []).find((c) => c.Description === m.Description && c.HasEquity)?.Equity;
  return { text: placement ? spelled(placement, t.cells) : m.Description, score: m.Score, equity, placement };
}

export function movesTable(ui, t, select) {
  const valued = ui.classic && (t.Candidates ?? []).some((c) => c.HasEquity);
  return h("div.review-table", {},
    h("div.review-row.review-header", {},
      h("span.review-move-cell", {}, "Jugada"), h("span.review-cell", {}, "#"), h("span.review-cell", {}, "Puntos"),
      valued ? h("span.review-cell.review-value-head", {}, "Equity", infoButton("Equity", EQUITY_INFO)) : null),
    lines(ui, t).map(([caption, rank, m]) => lineRow(ui, caption, rank, m, valued, select)),
  );
}

function lines(ui, t) {
  const best = t.Candidates?.[0];
  const rows = [];
  if (best && ui.classic) rows.push(["Mejor jugada", "#1", moveOf(t, best)]);
  (t.Marks ?? []).forEach((m) => rows.push([WHO[m.Who] ?? `Jugada de ${alias(m.Who)}`, m.Rank >= 0 ? `#${m.Rank + 1}` : "—", moveOf(t, m)]));
  return rows;
}

function lineRow(ui, caption, rank, m, valued, select) {
  return h("button", { class: "review-row review-line" + (ui.selected?.text === m.text ? " active" : ""), onclick: () => select(m) },
    h("span.review-move-cell", {}, h("span.review-caption", {}, caption), h("b.review-move", {}, m.text)),
    h("span.review-cell.muted", {}, rank),
    h("b.review-cell", {}, m.score),
    valued ? h("span.review-cell.muted", {}, m.equity == null ? "—" : m.equity.toFixed(1)) : null,
  );
}

export function turnBag(t, classic) {
  const tiles = unseen(t);
  const rival = classic ? Math.min(RACK, tiles.length) : null;
  return { tiles, rival, count: tiles.length - (rival ?? 0) };
}

function unseen(t) {
  const left = fullBag();
  const seen = [...t.cells.filter(Boolean).map((c) => (c.blank ? BLANK : c.letter)), ...tokens(t.Rack)];
  seen.forEach((l) => {
    const i = left.indexOf(l);
    if (i >= 0) left.splice(i, 1);
  });
  return left;
}

export function reviewMovesDialog(ui, go) {
  const rows = playedRows(ui);
  openDialog((close) => [
    h("h2", {}, "Movidas"),
    h("div.moves-scroll", {}, rows.length ? h("div.turns", {},
      movesLine(h("b.turn-number", {}, "#"), rowNames(ui).map((n) => h("b.turn-cell.ellipsis", {}, n)), "turn-head"),
      rows.map((r, i) => movesLine(h("span.turn-number", {}, i + 1), [r.left, r.right].map(playedCell), "", () => (close(), go(r.turn)))),
    ) : h("p.muted.moves-empty", {}, "Todavía no hay movidas.")),
    actions(h("button.text-btn", { onclick: close }, "Cerrar")),
  ], { wide: true });
}

function rowNames(ui) {
  if (!ui.classic) return [ui.me, ui.opponent];
  return ui.game.HumanStarts ? [ui.me, ui.opponent] : [ui.opponent, ui.me];
}

function playedRows(ui) {
  const turns = ui.game.Turns;
  if (!ui.classic) return turns.map((t, i) => ({ turn: i, left: played(t, t.Marks?.find((m) => m.Who === "Tú"), t.MyScore), right: played(t, t.Marks?.find((m) => m.Who !== "Tú"), t.OpponentScore) }));
  return turns.reduce((rows, t, i) => place(rows, i, t, ui), []);
}

function place(rows, i, t, ui) {
  const mine = t.Player !== ui.game.Opponent;
  const mark = t.Marks?.[0];
  if (!mark) return rows;
  const move = played(t, mark, mine ? t.MyScore : t.OpponentScore);
  const last = rows.at(-1);
  if (mine === ui.game.HumanStarts) rows.push({ turn: i, left: move, right: null });
  else if (!last || last.right) rows.push({ turn: i, left: null, right: move });
  else last.right = move;
  return rows;
}

function played(t, mark, before) {
  if (!mark) return null;
  t.cells ??= parseBoard(t.Board);
  return { text: moveOf(t, mark).text, score: mark.Score, total: before + mark.Score };
}

function playedCell(m) {
  if (!m) return h("span.turn-cell");
  return h("span.turn-cell", {}, h("span.turn-text.ellipsis", {}, m.text), h("span.turn-points", {}, m.score), h("b.total-badge", {}, m.total));
}

function movesLine(number, cells, extra, open) {
  return h("div", { class: "turn-line " + extra + (open ? " clickable" : ""), onclick: open }, number, cells[0], h("span.turn-sep", {}, "|"), cells[1]);
}
