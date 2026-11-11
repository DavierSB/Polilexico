import { h, replace } from "../dom.js";
import { go } from "../router.js";
import { screenHead } from "../parts.js";
import { load, save } from "../store.js";
import { choiceRow } from "../classic/options.js";
import { listRecords } from "../records.js";
import { bot } from "../classic/bots.js";

export const MODES = [["classic", "Clásica"], ["duplicate", "Duplicada"], ["endgame", "Finales"]];
const DATE = new Intl.DateTimeFormat("es", { day: "numeric", month: "numeric", year: "numeric", hour: "2-digit", minute: "2-digit" });

export function gamesScreen(app) {
  const body = h("div.stack");
  const tabs = h("div");
  app.append(h("div.screen", {}, screenHead("Mis partidas", "", h("button.text-btn", { onclick: () => go("estadisticas") }, "Estadísticas")), tabs, body));
  let mode = load("gamesTab", "classic");
  listRecords().then((records) => {
    const draw = () => {
      replace(tabs, choiceRow(MODES.map(([, l]) => l), MODES.findIndex(([m]) => m === mode), (i) => ((mode = MODES[i][0]), save("gamesTab", mode), draw())));
      const shown = records.filter((r) => r.summary.Mode === mode);
      replace(body, shown.length ? shown.map(card) : h("div.empty-state", {}, h("b", {}, "Aún no hay partidas."), h("span", {}, "Las partidas terminadas aparecen aquí.")));
    };
    draw();
  });
}

export function alias(name) {
  return bot(name)?.alias ?? name;
}

export function gameTitle(s) {
  const mode = MODES.find(([m]) => m === s.Mode)?.[1] ?? "Partida";
  if (s.Mode === "duplicate" || !s.Opponent) return mode;
  if (s.Local) return `${mode}: ${s.Me} contra ${s.Opponent}`;
  return `${mode} contra ${alias(s.Opponent)}`;
}

export function scoreText(s) {
  if (s.Mode === "duplicate") return `${s.MyScore}/${s.OpponentScore}`;
  return s.HumanStarts ? `${s.MyScore} – ${s.OpponentScore}` : `${s.OpponentScore} – ${s.MyScore}`;
}

function card(r) {
  const s = r.summary;
  const won = s.Mode === "duplicate" ? null : s.Outcome === "win" ? true : s.Outcome === "loss" ? false : null;
  const detail = DATE.format(new Date(r.finishedAt)) + (s.Mode === "duplicate" ? ` · ${s.Hits} aciertos` : "");
  return h("button.list-card", { onclick: () => go("partida-revision/" + r.id) },
    h("div.row", {},
      h("div.grow.stack", {}, h("b.list-name", {}, gameTitle(s)), h("span.muted.small", {}, detail)),
      h("div.stack.center", {}, h("b.list-percent", {}, scoreText(s)), won === null ? null : h("span", { class: "result-chip" + (won ? " on" : "") }, won ? "Ganada" : "Perdida")),
    ),
  );
}
