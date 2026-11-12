import { h } from "../dom.js";
import { parsePlacement, spelled } from "../placement.js";
import { tokens } from "../tiles.js";

const BUDGET_MS = 30000;
export { BUDGET_MS };

export const DEEP_TITLES = { endgame: "Análisis del final", preendgame: "Análisis del pre-final" };

export function deepView(view, cells, onPick) {
  if (view.Lines) return endgameView(view, cells, onPick);
  return preEndgameView(view, cells, onPick);
}

function text(description, cells) {
  const p = parsePlacement(description.replace(/ \(\d+ pts\)$/, ""));
  return { label: p ? spelled(p, cells) : description, placement: p };
}

function preEndgameView(view, cells, onPick) {
  const moves = (view.Moves ?? []).map((m) => ({ ...m, ...text(m.Description, cells) }));
  return h("div.stack.deep", {},
    view.Verdict ? h("p.deep-verdict", {}, view.Verdict) : null,
    view.PlayedRank >= 0 ? h("span.muted.small", {}, `Tu jugada quedó #${view.PlayedRank + 1}`) : null,
    h("div.moves-scroll", {}, h("table.moves.ranked", {},
      h("thead", {}, h("tr", {}, h("th", {}, "#"), h("th", {}, "Jugada"), h("th.num", {}, "Puntos"), h("th.num", {}, "Equity"))),
      h("tbody", {}, moves.map((m, n) => h("tr", { onclick: () => onPick?.(m) },
        h("td.muted", {}, n + 1), h("td", {}, m.label, m.Verdict ? h("div.muted.small", {}, m.Verdict) : null), h("td.num", {}, m.Score), h("td.num", {}, m.Value.toFixed(1)),
      ))),
    )),
  );
}

function endgameView(view, cells, onPick) {
  if (!view.Lines?.length) return h("p.muted", {}, "La partida ya terminó en esta posición.");
  return h("div.stack.deep", {},
    h("span.muted.small", {}, view.Exact ? "Resultado exacto" : `Profundidad ${view.Depth}`),
    view.Lines.map((l) => {
      const first = text(l.Description, cells);
      return h("button.review-line.deep-line", { onclick: () => onPick?.({ label: first.label, placement: first.placement }) },
        h("div.grow.stack", {},
          h("b", {}, first.label),
          h("span.muted.small", {}, (l.Steps ?? []).slice(1).map((s) => s.Description.replace(/ \(\d+ pts\)$/, "")).join(" · ")),
          endingText(l),
        ),
        h("b.review-points", {}, (l.Spread > 0 ? "+" : "") + l.Spread),
      );
    }),
  );
}

function endingText(l) {
  const left = tokens(l.OtherLeft).join("");
  const mine = tokens(l.MoverLeft).join("");
  const parts = [mine ? `te quedan ${mine}` : null, left ? `al rival le quedan ${left}` : null].filter(Boolean);
  return parts.length ? h("span.muted.small", {}, parts.join(" · ")) : null;
}
