import { h, replace } from "../dom.js";
import { go } from "../router.js";
import { screenHead } from "../parts.js";
import { listRecords } from "../records.js";
import { bot } from "../classic/bots.js";

export const MODES = [["classic", "Clásica"], ["duplicate", "Duplicada"], ["endgame", "Finales"]];
const DATE = new Intl.DateTimeFormat("es", { day: "numeric", month: "numeric", year: "numeric", hour: "2-digit", minute: "2-digit" });

const FOLDERS = {
  classic: ["Clásicas terminadas", "clasica"], duplicate: ["Duplicadas terminadas", "duplicada"], endgame: ["Finales terminados", "finales"],
  online: ["Partidas en línea terminadas", "clasica"],
};

export function finishedScreen(app, [mode]) {
  const [title, back] = FOLDERS[mode] ?? FOLDERS.classic;
  const body = h("div.stack");
  app.append(h("div.screen", {}, screenHead(title, back), body));
  listRecords().then((records) => replace(body, recordCards(records.filter((r) => inFolder(r, mode)))));
}

function inFolder(r, folder) {
  return folder === "online" ? !!r.online : r.summary.Mode === folder && !r.online;
}

function recordCards(shown) {
  return shown.length ? h("div.game-rows", {}, shown.map(card)) : h("p.stats-empty", {}, "Todavía no has terminado ninguna partida de este tipo.");
}

export function folderOf(record) {
  return record.online ? "online" : record.summary?.Mode ?? "classic";
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
  return h("button.finished-row", { onclick: () => go("partida-revision/" + r.id) },
    h("div.grow.stack", {}, h("b", {}, gameTitle(s)), h("span.muted.small", {}, detail)),
    h("b", { class: "game-score" + (won === true ? " won" : won === false ? " lost" : "") }, scoreText(s)),
  );
}
