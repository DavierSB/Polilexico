import { h } from "../dom.js";
import { go } from "../router.js";
import { toast } from "../dialogs.js";
import { getRecord } from "../records.js";
import { spreadOf, spreadChart } from "./spread.js";
import { exportDialog } from "./export.js";

export function endTools(recorded, state, names, close) {
  return [
    spreadChart(classicSpread(state, names)),
    h("div.end-tools", {},
      h("button.btn.small", { onclick: () => withRecord(recorded, (id) => (close(), go("partida-revision/" + id))) }, "Analizar"),
      h("button.btn.small.outlined", { onclick: () => withRecord(recorded, async (id) => exportDialog(picture(state, names), await getRecord(id))) }, "Exportar"),
    ),
  ];
}

export function classicSpread(state, names) {
  const totals = [[0, 0], ...state.moves.map((m) => [m.myTotal, m.opponentTotal]), [state.myScore, state.opponentScore]];
  const last = totals.at(-1), before = totals.at(-2);
  const points = totals.length > 1 && last[0] === before[0] && last[1] === before[1] ? totals.slice(0, -1) : totals;
  return spreadOf(names.me, names.opponent, state.moves[0]?.byMe !== false, points);
}

function picture(state, names) {
  const meFirst = state.moves[0]?.byMe !== false;
  const order = (a, b) => (meFirst ? [a, b] : [b, a]);
  return { cells: state.cells, names: order(names.me, names.opponent), scores: order(state.myScore, state.opponentScore) };
}

async function withRecord(recorded, use) {
  const id = await recorded;
  if (!id) return toast("La partida aún se está guardando.");
  use(id);
}
