import { h } from "../dom.js";
import { openDialog, actions } from "../dialogs.js";
import { totals } from "./state.js";

export function roundsTable(rounds) {
  if (!rounds.length) return h("p.muted.moves-empty", {}, "Aún no ha terminado ninguna ronda.");
  return h("table.moves.dup-moves", {},
    h("thead", {}, h("tr", {}, h("th", {}, "#"), h("th", {}, "Máster"), h("th.num", {}, "Pts"), h("th", {}, "Tú"), h("th.num", {}, "Pts"))),
    h("tbody", {}, rounds.map(roundRow).reverse()),
  );
}

function roundRow(r) {
  return h("tr", {},
    h("td.muted", {}, r.number),
    h("td", {}, playText(r.master)),
    h("td.num", { class: r.master.bingo ? "marked" : "" }, r.master.score),
    h("td", {}, playText(r.mine)),
    h("td.num", { class: r.hit || r.mine.bingo ? "marked" : "" }, r.mine.score + (r.hit ? " ✓" : "")),
  );
}

function playText(play) {
  const [coords, word] = play.text.split(" ");
  if (!word) return h("span.muted", {}, play.text);
  return h("span", {}, h("span.muted.coords", {}, coords), " ", h("b", {}, word));
}

export function movesDialog(rounds) {
  openDialog((close) => [h("h2", {}, "Movidas"), h("div.moves-scroll", {}, roundsTable(rounds)), actions(h("button.text-btn", { onclick: close }, "Cerrar"))], { wide: true });
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
