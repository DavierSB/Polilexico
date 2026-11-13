import { h } from "../dom.js";

export function claimDialog(claim, opponent) {
  const layer = h("div.verdict-layer", { onclick: () => layer.remove() },
    h("div.verdict-content", {},
      h("div", { class: `verdict-title small ${tone(claim)}` }, title(claim)),
      h("div.claim-words", {}, claim.words.map((w) => h("span", { class: claim.invalid.includes(w) ? "claim-word bad" : "claim-word good" }, w))),
      h("div.verdict-line", {}, line(claim, opponent)),
    ),
  );
  document.getElementById("layer").append(layer);
}

function title(claim) {
  if (claim.byMe) return claim.upheld ? "Jugada anulada" : "Jugada válida";
  return claim.upheld ? "Te reclamaron" : "Reclamación fallida";
}

function tone(claim) {
  if (!claim.upheld) return "neutral";
  return claim.byMe ? "win" : "loss";
}

function line(claim, opponent) {
  if (claim.byMe) return claim.upheld ? `${opponent} pierde ${claim.points} puntos` : "Sigues en tu turno";
  return claim.upheld ? `Pierdes ${claim.points} puntos y el turno` : "Tu jugada es válida";
}
