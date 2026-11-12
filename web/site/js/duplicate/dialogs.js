import { h } from "../dom.js";
import { openDialog, actions } from "../dialogs.js";
import { totals } from "./state.js";

export function movesDialog(rounds) {
  const body = h("div.moves-scroll", {}, roundLines(rounds));
  openDialog((close) => [h("h2", {}, "Movidas"), body, actions(h("button.text-btn", { onclick: close }, "Cerrar"))], { wide: true });
  body.scrollTop = body.scrollHeight;
}

function roundLines(rounds) {
  if (!rounds.length) return h("p.muted.moves-empty", {}, "Todavía no hay movidas.");
  return h("div.turns", {},
    h("div.turn-line.turn-head", {}, h("b.turn-number", {}, "#"), h("b.turn-cell", {}, "Máster"), h("b.turn-cell", {}, "Tú")),
    rounds.map((r) => h("div.turn-line", {}, h("span.turn-number", {}, r.number), roundCell(r.master, false), roundCell(r.mine, r.hit))),
  );
}

function roundCell(play, hit) {
  return h("span", { class: "turn-cell dup-cell" + (play.bingo ? " bingo" : "") },
    h("span.turn-text.ellipsis", {}, play.text), h("span", { class: "turn-points" + (hit ? " hit" : "") }, play.score + (hit ? " ✓" : "")));
}

export function roundDialog(round) {
  openDialog((close) => [
    h("h2", {}, `Ronda ${round.number}`),
    playLine("Máster", round.master),
    playLine("Tú", round.mine),
    actions(h("button.text-btn", { autofocus: true, onclick: close }, "Seguir")),
  ]);
}

function playLine(who, play) {
  return h("div.round-line", {}, h("span.muted", {}, who), h("div.row", {}, h("span.grow.round-play", {}, play.text), h("b.round-points", {}, play.score)));
}

export function endDialogs(state, skipInvalids = false) {
  if (state.revealAtEnd && !skipInvalids) return invalidsDialog(state);
  summaryDialog(state);
}

function invalidsDialog(state) {
  const plays = state.invalidPlays;
  openDialog((close) => [
    h("h2", {}, "Jugadas inválidas"),
    plays.length ? h("p", {}, `${plays.length} de tus jugadas no eran válidas y valen 0 puntos:`) : h("p", {}, "No hubo jugadas inválidas."),
    plays.map((p) => h("div.row.invalid-play", {}, h("span.muted", {}, p.number), h("span.grow", {}, p.text), h("b.error", {}, p.score))),
    actions(h("button.text-btn", { onclick: close }, "Ver resultados")),
  ], { onClose: () => summaryDialog(state) });
}

function summaryDialog(state) {
  const t = totals(state.rounds);
  openDialog((close) => [
    h("h2", {}, "Partida terminada"),
    h("div.efficiency", {},
      h("span.muted", {}, "Eficiencia"),
      h("b.efficiency-value", {}, `${t.efficiency.toLocaleString("es", { minimumFractionDigits: 1, maximumFractionDigits: 1 })} %`),
      h("span.muted.small", {}, `${t.mine} de ${t.master} puntos del máster`),
      h("b", {}, `${t.hits} aciertos de ${state.rounds.length} rondas`),
    ),
    actions(h("button.text-btn", { onclick: close }, "Cerrar")),
  ]);
}
