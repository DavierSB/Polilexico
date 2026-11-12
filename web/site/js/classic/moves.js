import { setting } from "../settings.js";
import { h } from "../dom.js";
import { LETTERS, BLANK } from "../tiles.js";
import { placedSquares } from "../placement.js";
import { openDialog, actions } from "../dialogs.js";
import { face } from "./rack.js";

const PHONE = "(max-width: 899px)";
const BINGO_TILES = 7;
const VOWELS = new Set(["A", "E", "I", "O", "U"]);

const KIND_TEXT = { pass: "Pasó", exchange: "Cambió", invalid: "Inválida" };

export function movesTable(state, names) {
  if (!state.moves.length) return h("p.muted.moves-empty", {}, "Aún no hay jugadas.");
  return h("table.moves", {},
    h("thead", {}, h("tr", {}, h("th", {}, "#"), h("th", {}, "Jugador"), h("th", {}, "Jugada"), h("th.num", {}, "Pts"), h("th.num", {}, "Total"))),
    h("tbody", {}, state.moves.map((m, i) => moveRow(m, i, names)).reverse()),
  );
}

function moveRow(m, i, names) {
  const total = m.byMe ? m.myTotal : m.opponentTotal;
  return h("tr", { class: m.byMe ? "mine" : "" },
    h("td.muted", {}, i + 1),
    h("td.ellipsis", {}, m.byMe ? names.me : names.opponent),
    h("td", {}, moveText(m)),
    h("td.num", {}, m.kind === "play" ? m.score : m.score || "—"),
    h("td.num", {}, total),
  );
}

function moveText(m) {
  if (m.kind === "play") return h("span", {}, h("span.muted.coords", {}, m.coords), " ", h("b", {}, m.word));
  if (m.kind === "exchange") return `${KIND_TEXT.exchange} ${m.count}`;
  return KIND_TEXT[m.kind] ?? m.kind;
}

export function movesDialog(state, names) {
  const body = h("div.moves-scroll", {}, turnsTable(state, names));
  openDialog((close) => [h("h2", {}, "Movidas"), body, actions(h("button.text-btn", { onclick: close }, "Cerrar"))], { wide: true });
  body.scrollTop = body.scrollHeight;
}

function turnsTable(state, names) {
  if (!state.moves.length) return h("p.muted.moves-empty", {}, "Todavía no hay movidas.");
  const meFirst = state.moves[0].byMe;
  const order = (mine, theirs) => (meFirst ? [mine, theirs] : [theirs, mine]);
  return h("div.turns", {},
    turnLine(h("b.turn-number", {}, "#"), order(names.me, names.opponent).map((n) => h("b.turn-cell.ellipsis", {}, n)), "turn-head"),
    byTurn(state.moves, meFirst).map((turn, i) => turnLine(h("span.turn-number", {}, i + 1), turn.map(turnCell))),
    endingLines(state, order),
  );
}

function byTurn(moves, meFirst) {
  return moves.reduce((turns, m) => {
    const last = turns.at(-1);
    if (m.byMe === meFirst) turns.push([m, null]);
    else if (!last || last[1]) turns.push([null, m]);
    else last[1] = m;
    return turns;
  }, []);
}

function turnLine(number, cells, extra = "") {
  return h("div", { class: "turn-line " + extra }, number, cells[0], h("span.turn-sep", {}, "|"), cells[1]);
}

function turnCell(m) {
  if (!m) return h("span.turn-cell");
  const bingo = !!m.placement && placedSquares(m.placement).length === BINGO_TILES;
  return h("span", { class: "turn-cell" + (bingo ? " bingo" : "") },
    h("span.turn-text.ellipsis", {}, turnText(m)), h("span.turn-points", {}, m.score), totalBadge(m.byMe ? m.myTotal : m.opponentTotal));
}

function turnText(m) {
  if (m.kind === "play") return `${m.coords} ${m.word}`;
  if (m.kind === "exchange") return "cambio " + (m.tiles || m.count);
  if (m.kind === "pass") return "pase";
  return "inválida";
}

function totalBadge(total) {
  return h("b.total-badge", {}, total);
}

function endingLines(state, order) {
  const r = state.result;
  const timed = r && (r.myPenalty || r.opponentPenalty);
  const ending = r && (r.myDelta || r.opponentDelta);
  if (!timed && !ending) return null;
  return [
    h("div.turn-dots"),
    timed ? labelLine("tiempo", order(-r.myPenalty, -r.opponentPenalty).map((d) => h("b.turn-delta", {}, signed(d)))) : null,
    ending ? labelLine("descuento", order(r.myDelta, r.opponentDelta).map((d) => h("b.turn-delta", {}, signed(d)))) : null,
    labelLine("final", order(state.myScore, state.opponentScore).map(totalBadge)),
  ];
}

function labelLine(label, values) {
  return turnLine(h("span.turn-number"), values.map((v) => h("span.turn-cell", {}, h("span.turn-text", {}, label), v)));
}

function signed(n) {
  return n > 0 ? `+${n}` : `${n}`;
}

export function bagDialog(state, title = null) {
  if (matchMedia(PHONE).matches) return phoneBag(state, title);
  if (!title && !setting("showUnseen")) return bagCount(state);
  const counts = letterCounts(state.unseen);
  openDialog((close) => [
    h("h2", {}, title ? `${title}: ${state.unseen.length}` : `Por salir: ${state.unseen.length}`),
    h("div.bag-grid", {}, [...counts].map(([l, n]) => h("div.bag-item", {}, h("div.rack-tile.small", {}, face(l)), h("span.muted", {}, "×" + n)))),
    title ? null : h("p.muted", {}, `${state.bag} en la bolsa · ${state.opponentTiles} en el atril rival`),
    actions(h("button.text-btn", { onclick: close }, "Cerrar")),
  ]);
}

function letterCounts(tiles) {
  const counts = new Map();
  tiles.forEach((t) => counts.set(t, (counts.get(t) ?? 0) + 1));
  return new Map([...LETTERS, BLANK].filter((l) => counts.has(l)).map((l) => [l, counts.get(l)]));
}

function phoneBag(state, title) {
  const heading = title ?? `Por salir (bolsa ${state.bag} + atril del rival)`;
  const full = !!title || setting("showUnseen");
  openDialog((close) => [
    h("h2", {}, heading),
    full ? h("p.small.bag-summary", {}, bagSummary(state.unseen)) : h("p", {}, `${state.unseen.length} fichas`),
    full ? h("div.letter-chips", {}, [...letterCounts(state.unseen)].map(([l, n]) => h("span.letter-chip", {}, h("span.chip-count", {}, n), h("b.chip-letter", {}, l)))) : null,
    actions(h("button.text-btn", { onclick: close }, "Cerrar")),
  ]);
}

function bagSummary(tiles) {
  const vowels = tiles.filter((t) => VOWELS.has(t)).length;
  const blanks = tiles.filter((t) => t === BLANK).length;
  const blankText = blanks === 0 ? "" : blanks === 1 ? " · 1 comodín" : ` · ${blanks} comodines`;
  return `${tiles.length} fichas · ${vowels} vocales · ${tiles.length - vowels - blanks} consonantes${blankText}`;
}

function bagCount(state) {
  openDialog((close) => [h("h2", {}, `En la bolsa: ${state.bag}`), actions(h("button.text-btn", { onclick: close }, "Cerrar"))]);
}

export function askBlank() {
  return new Promise((resolve) => {
    let chosen = null;
    openDialog((close) => [
      h("h2", {}, "Comodín"),
      h("div.blank-grid", {}, LETTERS.map((l) => h("button.rack-tile.small", { onclick: () => ((chosen = l), close()) }, h("span", { class: "rack-letter" + (l.length > 1 ? " digraph" : "") }, l)))),
      actions(h("button.text-btn", { onclick: close }, "Cancelar")),
    ], { onClose: () => resolve(chosen) });
  });
}
