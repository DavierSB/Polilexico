import { h, replace } from "../dom.js";
import { screenHead } from "../parts.js";
import { load, save } from "../store.js";
import { choiceRow } from "../classic/options.js";
import { listRecords, abandoned } from "../records.js";
import { account, request, token } from "../online/api.js";
import { MODES } from "./list.js";

const INFO = "Efectividad: tus puntos como porcentaje de los del máster. Acierto: un turno con el mismo puntaje que el máster.";
const fixed = (n, d = 0) => n.toLocaleString("es", { minimumFractionDigits: d, maximumFractionDigits: d });

export function statsScreen(app) {
  const body = h("div.stack");
  const tabs = h("div");
  app.append(h("div.screen", {}, screenHead("Estadísticas", "partidas"), tabs, body));
  const tabsList = [...MODES, ...(account() ? [["online", "En línea"]] : [])];
  let mode = load("statsTab", "classic");
  listRecords().then((records) => {
    const draw = () => {
      replace(tabs, choiceRow(tabsList.map(([, l]) => l), Math.max(0, tabsList.findIndex(([m]) => m === mode)), (i) => ((mode = tabsList[i][0]), save("statsTab", mode), draw())));
      if (mode === "online") return online(body);
      const games = records.map((r) => r.summary).filter((s) => s.Mode === mode);
      replace(body, rows(mode === "duplicate" ? duplicateRows(games) : classicRows(games, abandoned(mode))), mode === "duplicate" ? h("p.muted.small", {}, INFO) : null);
    };
    draw();
  });
}

function rows(list) {
  if (!list) return h("div.empty-state", {}, h("b", {}, "Aún no hay partidas."));
  return h("div.stats-table", {}, list.map(([k, v]) => h("div.row.stats-row", {}, h("span.grow", {}, k), h("b", {}, v))));
}

function average(games, f) {
  return games.length ? games.reduce((t, g) => t + f(g), 0) / games.length : 0;
}

function bestWord(games) {
  const g = games.filter((x) => x.BestWord).sort((a, b) => b.BestWordScore - a.BestWordScore)[0];
  return g ? `${g.BestWord} (${g.BestWordScore})` : "—";
}

function classicRows(games, dropped) {
  if (!games.length && !dropped) return null;
  const wins = games.filter((g) => g.Outcome === "win").length;
  const decided = games.length + dropped;
  const bingos = games.reduce((t, g) => t + g.Bingos, 0);
  return [
    ["Partidas iniciadas", decided], ["Terminadas", games.length], ["Abandonadas", dropped],
    ["Victorias", `${wins}  (${fixed(decided ? (wins * 100) / decided : 0)} %)`],
    ["Promedio de puntos", fixed(average(games, (g) => g.MyScore))], ["Puntuación máxima", games.length ? Math.max(...games.map((g) => g.MyScore)) : "—"],
    ["Scrabbles", `${bingos}  (${fixed(games.length ? bingos / games.length : 0, 1)} por partida)`], ["Palabra más valiosa", bestWord(games)],
  ];
}

function duplicateRows(games) {
  if (!games.length) return null;
  const eff = (g) => (g.OpponentScore ? (g.MyScore * 100) / g.OpponentScore : 0);
  const turns = games.reduce((t, g) => t + g.turns, 0);
  return [
    ["Partidas", games.length], ["Aciertos", `${fixed(turns ? (games.reduce((t, g) => t + g.Hits, 0) * 100) / turns : 0, 1)} %`],
    ["Efectividad promedio", `${fixed(average(games, eff), 1)} %`], ["Mejor efectividad", `${fixed(Math.max(...games.map(eff)), 1)} %`],
    ["Promedio de puntos", fixed(average(games, (g) => g.MyScore))], ["Puntuación máxima", Math.max(...games.map((g) => g.MyScore))],
    ["Scrabbles", games.reduce((t, g) => t + g.Bingos, 0)],
  ];
}

async function online(body) {
  replace(body, h("div.searching.short", {}, h("div.spinner")));
  try {
    const data = await request("GET", "/stats", null, await token());
    const list = data.rows ?? [];
    replace(body, list.length ? list.map(onlineBlock) : h("div.empty-state", {}, h("b", {}, "Aún no hay partidas en línea.")));
  } catch (e) {
    replace(body, h("div.empty-state", {}, h("b", {}, e.message)));
  }
}

export function onlineBlock(r) {
  const title = r.mode === "duplicate" ? "Duplicada" : "Clásica";
  const list = r.mode === "duplicate"
    ? [["Partidas", r.games], ["Primeros puestos", r.wins], ["Efectividad promedio", r.avg_efficiency == null ? "—" : `${fixed(r.avg_efficiency * 100, 1)} %`], ["Mayor scrabble", r.best_bingo || "—"], ["Mejor jugada", r.best_play]]
    : [["Partidas", r.games], ["Victorias", `${r.wins}  (${fixed((100 * r.wins) / Math.max(1, r.games))} %)`], ["Derrotas", r.losses], ["Empates", r.draws], ["Promedio de puntos", fixed(r.avg_score ?? 0)], ["Mayor scrabble", r.best_bingo || "—"], ["Mejor jugada", r.best_play]];
  return h("div.stack", {}, h("h3.side-title", {}, title), rows(list));
}
