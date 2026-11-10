import { h } from "../dom.js";
import { LETTERS, BLANK } from "../tiles.js";
import { openDialog, actions } from "../dialogs.js";
import { face } from "./rack.js";

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
  openDialog((close) => [h("h2", {}, "Movidas"), h("div.moves-scroll", {}, movesTable(state, names)), actions(h("button.text-btn", { onclick: close }, "Cerrar"))], { wide: true });
}

export function bagDialog(state) {
  const counts = new Map();
  state.unseen.forEach((t) => counts.set(t, (counts.get(t) ?? 0) + 1));
  const order = [...LETTERS, BLANK].filter((l) => counts.has(l));
  openDialog((close) => [
    h("h2", {}, `Por salir: ${state.unseen.length}`),
    h("div.bag-grid", {}, order.map((l) => h("div.bag-item", {}, h("div.rack-tile.small", {}, face(l)), h("span.muted", {}, "×" + counts.get(l))))),
    h("p.muted", {}, `${state.bag} en la bolsa · ${state.opponentTiles} en el atril rival`),
    actions(h("button.text-btn", { onclick: close }, "Cerrar")),
  ]);
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
