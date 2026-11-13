import { h } from "../dom.js";
import { go } from "../router.js";
import { openDialog, actions, toast } from "../dialogs.js";
import { getRecord } from "../records.js";
import { confetti } from "../confetti.js";
import { spreadChart, spreadDialog, classicSpread } from "../games/spread.js";
import { exportDialog } from "../games/export.js";
import { lookUpWord } from "../study/lookup.js";

const TONES = { win: "win", loss: "loss", neutral: "neutral" };

const CONFETTI_LEAD_MS = 1500;

export function announceEnd(state, names, who, tools) {
  const reason = state.result.endReason;
  if (reason === "out" || reason === "passes" || reason === "neutral") return endingDialog(state, names, who, () => resultPopup(state, names, who, tools));
  resultPopup(state, names, who, tools);
}

export function resultPanel(state, names, who, tools) {
  return h("div.stack.result-panel", {},
    h("b.result-title", {}, `${resultTitle(state, names, who)}   ${state.myScore} – ${state.opponentScore}`),
    h("div.row.end-buttons", {},
      endButtons({ recorded: tools.recorded, endgame: state.bag === 0, words: playedWords(state, names), picture: picture(state, names), menu: tools.menu }),
      h("button.text-btn", { onclick: () => spreadDialog(classicSpread(state, names)) }, "Ventaja"),
    ),
  );
}

export function endButtons({ recorded, endgame = false, words = [], picture = null, menu }) {
  return [
    h("button.btn.small", { onclick: () => withRecord(recorded, (id) => go("partida-revision/" + id)) }, "Analizar"),
    endgame ? h("button.btn.small", { onclick: () => withRecord(recorded, (id) => go(`partida-revision/${id}/final`)) }, "Analizar final") : null,
    words.some((s) => s.words.length) ? h("button.btn.small.outlined", { onclick: () => wordsDialog(words) }, "Palabras") : null,
    h("button.btn.small.outlined", { onclick: () => withRecord(recorded, async (id) => exportDialog(picture, await getRecord(id))) }, "Exportar"),
    h("button.btn.small.outlined", { onclick: menu }, "Menú"),
  ];
}

export function wordsOf(plays) {
  return [...new Map(plays.filter((w) => /^\p{L}+$/u.test(w ?? "")).map((w) => [w.toLowerCase(), w])).values()];
}

function endingDialog(state, names, who, then) {
  const r = state.result;
  openDialog((close) => [
    h("h2", {}, endingTitle(state, names, who)),
    r.endReason === "out" || r.myDelta || r.opponentDelta ? h("div.stack.ending-summary", {},
      h("span.muted.small", {}, "Descuento por fichas"),
      deltaLine(names.me, r.myDelta),
      deltaLine(names.opponent, r.opponentDelta),
    ) : null,
    actions(h("button.text-btn", { onclick: close }, "Continuar")),
  ], { onClose: then });
}

function endingTitle(state, names, who) {
  const reason = state.result.endReason;
  if (reason === "passes") return "Juego terminado por 4 pases consecutivos";
  if (reason === "neutral") return "Juego terminado por 12 turnos neutros consecutivos";
  if (state.rack.length) return `${names.opponent} se pegó`;
  return who.local || who.watching ? `${names.me} se pegó` : "Te pegaste";
}

function deltaLine(name, delta) {
  return h("div.row.delta-line", {}, h("span.grow", {}, name), h("b", {}, (delta > 0 ? "+" : "") + (delta ?? 0)));
}

function resultPopup(state, names, who, tools) {
  const won = wonBy(state, who);
  if (won) confetti();
  setTimeout(() => openResult(state, names, who, tools), won ? CONFETTI_LEAD_MS : 0);
}

function openResult(state, names, who, tools) {
  const r = state.result;
  const tone = wonBy(state, who) ? TONES.win : r.lostOnTime || r.outcome === "loss" ? TONES.loss : TONES.neutral;
  const words = playedWords(state, names);
  openDialog((close) => [
    h("div", { class: `result-verdict ${tone}${who.local || who.watching ? " small" : ""}` }, verdictText(state, names, who)),
    r.lostOnTime || r.rivalLostOnTime ? h("div.result-line", {}, "por tiempo") : null,
    h("div.result-line.result-score", {}, `${state.myScore} – ${state.opponentScore}`),
    h("div.result-line.muted.small", {}, `${names.me} · ${names.opponent}`),
    state.moves.length ? spreadChart(classicSpread(state, names)) : null,
    h("div.stack.result-buttons", {},
      h("button.btn.block", { onclick: () => (close(), withRecord(tools.recorded, (id) => go("partida-revision/" + id))) }, "Analizar partida"),
      state.bag === 0 ? h("button.btn.block", { onclick: () => (close(), withRecord(tools.recorded, (id) => go(`partida-revision/${id}/final`))) }, "Analizar final") : null,
      h("div.row.two-buttons", {},
        words.some((s) => s.words.length) ? h("button.btn.outlined.grow", { onclick: () => wordsDialog(words) }, "Palabras") : null,
        h("button.btn.outlined.grow", { onclick: () => (close(), withRecord(tools.recorded, async (id) => exportDialog(picture(state, names), await getRecord(id)))) }, "Exportar")),
    ),
    actions(h("button.text-btn", { onclick: close }, "Cerrar")),
  ]);
}

function wonBy(state, who) {
  const r = state.result;
  if (who.local || who.watching) return r.outcome !== "tie";
  return r.outcome === "win" && !r.lostOnTime;
}

function winnerName(state, names) {
  const o = state.result.outcome;
  return o === "win" ? names.me : o === "loss" ? names.opponent : null;
}

function verdictText(state, names, who) {
  const r = state.result;
  if (who.watching) return winnerName(state, names) ? `${winnerName(state, names)} ganó` : "Empate";
  if (who.local) return winnerName(state, names) ? `¡Ganó ${winnerName(state, names)}!` : "Empate";
  if (wonBy(state, who)) return "¡Ganaste!";
  if (r.lostOnTime || r.outcome === "loss") return "Perdiste";
  return "Empate";
}

function resultTitle(state, names, who) {
  const r = state.result;
  const winner = winnerName(state, names);
  if (who.watching) return winner ? `${winner} ganó` : "Empate";
  if (who.local) return (winner ? `Ganó ${winner}` : "Empate") + (r.lostOnTime || r.rivalLostOnTime ? " por tiempo" : "");
  if (r.lostOnTime) return "Perdiste por tiempo";
  if (r.outcome === "win") return "¡Ganaste!";
  if (r.outcome === "loss") return `Ganó ${names.opponent}`;
  return "Empate";
}

function playedWords(state, names) {
  const words = (mine) => wordsOf(state.moves.filter((m) => m.kind === "play" && m.byMe === mine).map((m) => m.word));
  return [{ title: names.me, words: words(true) }, { title: names.opponent, words: words(false) }];
}

export function wordsDialog(sections) {
  openDialog((close) => [
    h("h2", {}, "Palabras de la partida"),
    h("p.muted.small", {}, "Toca una palabra para ver su ficha en el diccionario."),
    h("div.stack.words-sections", {}, sections.filter((s) => s.words.length).map((s) => h("div.stack.words-section", {},
      h("b.words-title", {}, s.title),
      s.words.map((w) => h("button.dictionary-word", { onclick: () => lookUpWord(w) }, w.toUpperCase())),
    ))),
    actions(h("button.text-btn", { onclick: close }, "Cerrar")),
  ]);
}

function picture(state, names) {
  const meFirst = state.moves[0]?.byMe !== false;
  const order = (a, b) => (meFirst ? [a, b] : [b, a]);
  return { cells: state.cells, names: order(names.me, names.opponent), scores: order(state.myScore, state.opponentScore) };
}

async function withRecord(recorded, use) {
  const id = await (typeof recorded === "function" ? recorded() : recorded);
  if (!id) return toast("No se pudo guardar la partida.");
  use(id);
}
