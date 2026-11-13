import { h } from "../dom.js";
import { openDialog, actions } from "../dialogs.js";
import { parsePlacement, spelled } from "../placement.js";
import { fullBag, tokens, parseBoard, BLANK } from "../tiles.js";
import { infoButton } from "../classic/options.js";
import { alias } from "./list.js";

const RACK = 7;
const WHO = { "Tú": "Tu jugada", "Máster": "Jugada del Máster" };
const EQUITY_INFO = "El valor de la jugada, considerando su puntuación y lo que queda en la mano.";
const PRE_ENDGAME_INFO = "El equity usual, pero incorpora penalizaciones por conservar la Q sin poder colocarla o arriesgarse a robarla, así como bonificaciones por mandar la Q a la bolsa si se estima que el rival no puede colocarla.";
const FINAL_INFO = "La diferencia de puntos con la que se espera terminar la partida tras esta jugada, si los dos siguen la mejor línea encontrada. Positiva: gana quien juega. Al tocar una jugada se ve en el tablero con su línea, y con «Jugar» se sigue desde ella.";

export const EQUITY_COLUMN = { title: "Equity", info: EQUITY_INFO };
export const PRE_ENDGAME_COLUMN = { title: "Equity", info: PRE_ENDGAME_INFO };
export const FINAL_COLUMN = { title: "Final", info: FINAL_INFO };

export function moveOf(t, m) {
  const raw = m.Description.replace(/^\(Inválida (\S+ \S+):.*\)$/, "$1");
  const placement = parsePlacement(raw);
  const equity = m.HasEquity ? m.Equity : (t.Candidates ?? []).find((c) => c.Description === m.Description && c.HasEquity)?.Equity;
  return { text: placement ? spelled(placement, t.cells) : m.Description, score: m.Score, equity, placement };
}

export function pageOf(t, classic) {
  const candidates = (t.Candidates ?? []).map((c) => moveOf(t, c));
  if (!classic) candidates.forEach((c) => (c.equity = null));
  const marks = (t.Marks ?? []).map((m) => ({ who: m.Who, rank: m.Rank >= 0 ? m.Rank : null, move: { ...moveOf(t, m), ...(classic ? {} : { equity: null }) } }));
  return { candidates, marks };
}

export function valueColumn(moves) {
  if (moves.some((m) => m.final != null)) return FINAL_COLUMN;
  if (moves.some((m) => m.adjusted)) return PRE_ENDGAME_COLUMN;
  if (moves.some((m) => m.equity != null)) return EQUITY_COLUMN;
  return null;
}

export function valueText(m) {
  if (m.final != null) return (m.final > 0 ? "+" : "") + m.final;
  return m.equity == null ? "" : m.equity.toFixed(1);
}

export function valueHead(column) {
  return h("span.review-cell.review-value-head", {}, column.title, infoButton(column.title, column.info));
}

export function movesTable(ui, page, select) {
  const column = valueColumn(page.candidates);
  return h("div.review-table", {},
    h("div.review-row.review-header", {},
      h("span.review-move-cell", {}, "Jugada"), h("span.review-cell.rank-cell", {}, "#"), h("span.review-cell", {}, "Puntos"),
      column ? valueHead(column) : null),
    lines(ui, page).map(([caption, rank, m]) => lineRow(ui, caption, rank, m, !!column, select)),
  );
}

function lines(ui, page) {
  const best = page.candidates[0];
  const rows = [];
  if (best && ui.classic) rows.push([noted("Mejor jugada", best), "#1", best]);
  page.marks.forEach((m) => rows.push([noted(WHO[m.who] ?? `Jugada de ${alias(m.who)}`, m.move), m.rank != null ? `#${m.rank + 1}` : "—", m.move]));
  return rows;
}

function noted(caption, move) {
  return move.note ? `${caption} · ${move.note}` : caption;
}

function lineRow(ui, caption, rank, m, valued, select) {
  return h("button", { class: "review-row review-line" + (ui.selected?.text === m.text ? " active" : ""), onclick: () => select(m) },
    h("span.review-move-cell", {}, h("span.review-caption", {}, caption), h("b.review-move", {}, m.text)),
    h("span.review-cell.rank-cell.muted", {}, rank),
    h("b.review-cell", {}, m.score),
    valued ? h("span.review-cell.muted", {}, valueText(m) || "—") : null,
  );
}

export function candidatesTable(moves, selected, choose) {
  if (!moves.length) return h("p.muted", {}, "Sin jugadas.");
  const column = valueColumn(moves);
  return h("table.moves.ranked", {},
    h("thead", {}, h("tr", {}, h("th", {}, "#"), h("th", {}, "Jugada"), h("th.num", {}, "Puntos"), column ? h("th.num", {}, h("span.row.value-th", {}, column.title, infoButton(column.title, column.info))) : null)),
    h("tbody", {}, moves.map((m, n) => h("tr", { class: selected?.text === m.text ? "active" : "", onclick: () => choose(m) },
      h("td.muted", {}, n + 1),
      h("td", {}, h("span.row.move-with-tags", {}, h("span", {}, m.text), m.note ? h("span.move-note", {}, m.note) : null, m.playedBy ? h("span.played-by", {}, m.playedBy) : null)),
      h("td.num", {}, m.score),
      column ? h("td.num", {}, valueText(m)) : null))),
  );
}

export function turnTitle(t, count) {
  return h("b.turn-title.ellipsis", {}, `Turno ${t.Number}/${count}${t.Player ? " · " + alias(t.Player) : ""}`);
}

export function scoreBar(name, score, rack, active) {
  return h("div", { class: "player-card review-bar" + (active ? " active" : "") },
    h("span.player-name.ellipsis", {}, name),
    rack ? h("div.mini-tiles.bar-tiles", {}, rack.map((l) => h("span.mini-tile.face-up", {}, l === BLANK ? "" : l))) : null,
    h("span.grow"),
    h("span.score", {}, score));
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
