import { h, replace } from "../dom.js";
import { screenHead } from "../parts.js";
import { load, save } from "../store.js";
import { confirm } from "../dialogs.js";
import { choiceRow } from "../classic/options.js";
import { bot } from "../classic/bots.js";
import { listRecords, abandoned } from "../records.js";
import { account, request, token } from "../online/api.js";
import { countSaved } from "../savedGames.js";
import { alias } from "./list.js";
import { dotStrip, scoreScatter } from "./statsCharts.js";

const EFFICIENCY_NOTE = "Efectividad: tus puntos como porcentaje de los del máster. Acierto: un turno con el mismo puntaje que el máster.";
const RESET_TEXT = "¿Reiniciar las estadísticas? Tus partidas no se borran (siguen en «Mis partidas»), pero solo contarán las que juegues a partir de ahora.";
const SOURCES = [["all", "Todo"], ["online", "En línea"], ["offline", "Offline"]];
const MODES = [["classic", "Clásica"], ["duplicate", "Duplicada"]];
const fixed = (n, d = 0) => n.toLocaleString("es", { minimumFractionDigits: d, maximumFractionDigits: d });

export function statsScreen(app) {
  const ui = { mode: "classic", source: "all", scatter: false, records: null, online: undefined };
  ui.tabs = h("div.mode-tabs", { role: "tablist" });
  ui.body = h("div.stack.stats-body");
  ui.draw = () => draw(ui);
  app.append(h("div.screen", {}, screenHead("Mis estadísticas", "", h("button.text-btn", { onclick: () => reset(ui) }, "Reiniciar")), ui.tabs, ui.body));
  listRecords().then((records) => ((ui.records = records), ui.draw()));
  fetchOnline(ui);
  ui.draw();
}

async function fetchOnline(ui) {
  if (!account()) return ((ui.online = { error: "Sin perfil en línea." }), ui.draw());
  try {
    const data = await request("GET", "/stats", null, await token());
    ui.online = { rows: data.rows ?? [] };
  } catch (e) {
    ui.online = { error: e.message || "Sin conexión." };
  }
  ui.draw();
}

async function reset(ui) {
  if (!(await confirm(RESET_TEXT, "Reiniciar", "Cancelar"))) return;
  save("statsReset", { at: Date.now(), abandoned: { classic: abandoned("classic"), duplicate: abandoned("duplicate") } });
  ui.draw();
}

function draw(ui) {
  replace(ui.tabs, MODES.map(([m, label]) => h("button", { class: "mode-tab" + (ui.mode === m ? " on" : ""), onclick: () => ((ui.mode = m), ui.draw()) }, label)));
  const page = pageFor(ui);
  replace(ui.body,
    choiceRow(SOURCES.map(([, l]) => l), SOURCES.findIndex(([s]) => s === ui.source), (i) => ((ui.source = SOURCES[i][0]), ui.draw())),
    page.rows.length ? [statsTable(page.rows), page.note ? h("p.muted.small", {}, page.note) : null, page.games.length ? charts(ui, page.games) : null] : h("p.stats-empty", {}, page.empty),
  );
}

function pageFor(ui) {
  if (ui.source === "online") return onlinePage(ui);
  if (!ui.records) return { rows: [], empty: "Cargando…", games: [] };
  const stats = offlineStats(ui);
  if (ui.source === "offline") return offlinePage(ui, stats);
  return allPage(ui, stats);
}

function offlinePage(ui, s) {
  const empty = emptyText();
  if (!s.started) return { rows: [], empty, games: [] };
  const games = s.list.map(chartGame);
  if (ui.mode === "classic") return { rows: classicRows(s), empty, games };
  return { rows: duplicateRows(s), empty, note: s.games ? EFFICIENCY_NOTE : null, games };
}

function onlinePage(ui) {
  if (ui.online === undefined) return { rows: [], empty: "Cargando…", games: [] };
  if (ui.online.error) return { rows: [], empty: ui.online.error, games: [] };
  const row = rowFor(ui.mode, ui.online.rows);
  if (!row) return { rows: [], empty: "Aún no hay partidas en línea.", games: [] };
  return { rows: onlineRows(row), empty: "", games: [] };
}

function allPage(ui, s) {
  const row = ui.online?.rows ? rowFor(ui.mode, ui.online.rows) : null;
  const t = totals(s, row);
  if (!t.games && !s.started) return { rows: [], empty: emptyText(), games: [] };
  const best = t.bestPlay > 0 ? String(t.bestPlay) : "—";
  const rows = ui.mode === "classic"
    ? [["Partidas", t.games], ["Victorias", `${t.wins}  (${fixed(t.decided ? (t.wins * 100) / t.decided : 0)} %)`], ["Promedio de puntos", fixed(t.averageScore)], ["Mejor jugada", best]]
    : [["Partidas", t.games], ["Efectividad promedio", t.averageEfficiency == null ? "—" : `${fixed(t.averageEfficiency, 1)} %`], ["Promedio de puntos", fixed(t.averageScore)], ["Mejor jugada", best]];
  return { rows, empty: "", games: [] };
}

function offlineStats(ui) {
  const since = load("statsReset", null);
  const list = ui.records.filter((r) => chosen(ui.mode, r) && (!since || r.finishedAt >= since.at)).map((r) => ({ ...r.summary, finishedAt: r.finishedAt }));
  const dropped = Math.max(0, abandoned(ui.mode) - (since?.abandoned?.[ui.mode] ?? 0));
  const open = countSaved(ui.mode);
  const wins = list.filter((g) => g.Outcome === "win").length;
  const best = list.filter((g) => g.BestWord).sort((a, b) => b.BestWordScore - a.BestWordScore)[0];
  return { list, games: list.length, abandoned: dropped, started: list.length + dropped + open, wins, best };
}

function chosen(mode, r) {
  const s = r.summary ?? {};
  return !r.online && !r.watched && !s.Local && s.Mode === mode && (mode !== "classic" || !!bot(s.Opponent));
}

function emptyText() {
  return load("statsReset", null) ? "Aún no hay partidas desde que reiniciaste las estadísticas." : "Aún no hay partidas.";
}

function average(list, f) {
  return list.length ? list.reduce((t, g) => t + f(g), 0) / list.length : 0;
}

function efficiencyOf(g) {
  return g.OpponentScore ? (g.MyScore * 100) / g.OpponentScore : 0;
}

function bestWordText(s) {
  return s.best ? `${s.best.BestWord} (${s.best.BestWordScore})` : "—";
}

function countRows(s) {
  return [["Partidas iniciadas", s.started], ["Terminadas", s.games], ["Abandonadas", s.abandoned]];
}

function classicRows(s) {
  const decided = s.games + s.abandoned;
  const rows = [...countRows(s), ["Victorias", `${s.wins}  (${fixed(decided ? (s.wins * 100) / decided : 0)} %)`]];
  if (!s.games) return rows;
  const bingos = s.list.reduce((t, g) => t + g.Bingos, 0);
  return [...rows,
    ["Promedio de puntos", fixed(average(s.list, (g) => g.MyScore))], ["Puntuación máxima", Math.max(...s.list.map((g) => g.MyScore))],
    ["Scrabbles", `${bingos}  (${fixed(bingos / s.games, 1)} por partida)`], ["Palabra más valiosa", bestWordText(s)]];
}

function duplicateRows(s) {
  if (!s.games) return countRows(s);
  const turns = s.list.reduce((t, g) => t + g.turns, 0);
  return [...countRows(s),
    ["Aciertos", `${fixed(turns ? (s.list.reduce((t, g) => t + g.Hits, 0) * 100) / turns : 0, 1)} %`],
    ["Efectividad promedio", `${fixed(average(s.list, efficiencyOf), 1)} %`], ["Mejor efectividad", `${fixed(Math.max(...s.list.map(efficiencyOf)), 1)} %`],
    ["Promedio de puntos", fixed(average(s.list, (g) => g.MyScore))], ["Puntuación máxima", Math.max(...s.list.map((g) => g.MyScore))],
    ["Scrabbles", s.list.reduce((t, g) => t + g.Bingos, 0)], ["Palabra más valiosa", bestWordText(s)]];
}

function rowFor(mode, rows) {
  return rows.find((r) => r.mode === mode && r.games > 0) ?? null;
}

function onlineRows(r) {
  if (r.mode === "duplicate") return [["Partidas", r.games], ["Primeros puestos", r.wins], ["Efectividad promedio", r.avg_efficiency == null ? "—" : `${fixed(r.avg_efficiency * 100, 1)} %`], ["Mayor scrabble", r.best_bingo || "—"], ["Mejor jugada", r.best_play]];
  return [["Partidas", r.games], ["Victorias", `${r.wins}  (${fixed((100 * r.wins) / Math.max(1, r.games))} %)`], ["Derrotas", r.losses], ["Empates", r.draws], ["Promedio de puntos", fixed(r.avg_score ?? 0)], ["Mayor scrabble", r.best_bingo || "—"], ["Mejor jugada", r.best_play]];
}

function totals(s, row) {
  const online = row?.games ?? 0;
  const weighted = (parts) => {
    const count = parts.reduce((t, [, n]) => t + n, 0);
    return count ? parts.reduce((t, [v, n]) => t + v * n, 0) / count : 0;
  };
  const effParts = [s.games ? [average(s.list, efficiencyOf), s.games] : null, row?.avg_efficiency != null ? [row.avg_efficiency * 100, online] : null].filter(Boolean);
  return {
    games: s.games + online, decided: s.games + s.abandoned + online, wins: s.wins + (row?.wins ?? 0),
    averageScore: weighted([[average(s.list, (g) => g.MyScore), s.games], [row?.avg_score ?? 0, online]]),
    averageEfficiency: effParts.length ? weighted(effParts) : null,
    bestPlay: Math.max(s.best?.BestWordScore ?? 0, row?.best_play ?? 0),
  };
}

function statsTable(rows) {
  return h("div.stats-table", {}, rows.map(([k, v]) => h("div.row.stats-row", {}, h("span.grow", {}, k), h("b", {}, v))));
}

function charts(ui, games) {
  const note = h("p.muted.small", {}, "Toca un punto para ver esa partida.");
  if (ui.mode === "duplicate") return [h("b.chart-title", {}, "Efectividad"), dotStrip(games, "Efectividad por partida (% de los puntos del máster)", "%"), note];
  return [
    choiceRow(["Tus puntos", "Tú vs rival"], ui.scatter ? 1 : 0, (i) => ((ui.scatter = i === 1), ui.draw())),
    ui.scatter ? scoreScatter(games) : dotStrip(games, "Tus puntos"),
    note,
  ];
}

function chartGame(g) {
  const duplicate = g.Mode === "duplicate";
  return {
    myScore: g.MyScore, opponentScore: g.OpponentScore, value: duplicate ? Math.trunc(efficiencyOf(g)) : g.MyScore,
    title: `Tú ${g.MyScore} – ${g.OpponentScore} ${alias(g.Opponent)}`,
    lines: [
      { win: "Ganaste", loss: "Perdiste", tie: "Empate" }[g.Outcome] ?? null,
      duplicate ? `Eficiencia: ${fixed(efficiencyOf(g), 1)} %` : null,
      duplicate ? `Aciertos: ${g.Hits} de ${g.turns}` : null,
      g.BestWord ? `Palabra más valiosa: ${g.BestWord} (${g.BestWordScore})` : null,
      g.Bingos > 0 ? `Scrabbles: ${g.Bingos}` : null,
      new Date(g.finishedAt).toLocaleString("es", { day: "numeric", month: "short", hour: "2-digit", minute: "2-digit" }),
    ].filter(Boolean),
  };
}

export function onlineBlock(r) {
  return h("div.stack.stats-card", {}, h("b", {}, r.mode === "duplicate" ? "Duplicada" : "Clásica"), onlineRows(r).map(([k, v]) => h("div.row", {}, h("span.grow", {}, k), h("b", {}, v))));
}
