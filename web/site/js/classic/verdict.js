import { h } from "../dom.js";

const REASONS = {
  out: (s, rival) => (s.result.myDelta > 0 ? `Cerraste: +${s.result.myDelta} de las fichas de ${rival}.` : `${rival} cerró: +${s.result.opponentDelta}.`),
  passes: () => "Terminó por pases seguidos.",
  neutral: () => "Terminó por turnos sin puntos.",
};

export function localVerdict(state, first, second) {
  const r = state.result;
  const timed = r.lostOnTime || r.rivalLostOnTime ? " por tiempo" : "";
  if (r.outcome === "tie") return "Empate";
  return `Ganó ${r.outcome === "win" ? first : second}${timed}`;
}

export function verdict(state, rival, watchedName = null) {
  const r = state.result;
  if (watchedName) return r.outcome === "tie" ? "Empate" : `Ganó ${r.outcome === "win" ? watchedName : rival}`;
  if (r.lostOnTime) return "Perdiste por tiempo";
  if (r.outcome === "win") return "¡Ganaste!";
  if (r.outcome === "loss") return `Ganó ${rival}`;
  return "Empate";
}

export function endDetail(state, rival) {
  const lines = [];
  const reason = REASONS[state.result.endReason]?.(state, rival);
  if (reason) lines.push(reason);
  if (state.result.myPenalty) lines.push(`Descuento: −${state.result.myPenalty}.`);
  if (state.result.opponentPenalty) lines.push(`Descuento de ${rival}: −${state.result.opponentPenalty}.`);
  return lines.length ? h("p.muted", {}, lines.join(" ")) : null;
}
