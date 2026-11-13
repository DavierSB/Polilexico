import { h, replace } from "../dom.js";
import { go } from "../router.js";
import { overrideWith } from "../theme.js";
import { engineLoading } from "../parts.js";
import { openDialog, actions } from "../dialogs.js";
import { bot, botPhoto } from "./bots.js";
import { currentSession, resumeSaved, startAgain, hasSaved } from "./session.js";
import { gameView } from "./view.js";
import { verdict, endDetail, localVerdict } from "./verdict.js";
import { endTools } from "../games/endTools.js";
import { confetti } from "../confetti.js";

export function classicGameScreen(app) {
  const holder = h("div.screen");
  app.append(holder);
  let view = null;
  open(holder, (v) => (view = v));
  return () => view?.destroy();
}

async function open(holder, keep) {
  const session = currentSession() ?? (hasSaved() ? await waitFor(holder, resumeSaved()) : null);
  if (!session) return go("clasica", true);
  const players = session.setup.players;
  const rival = bot(session.setup.bot) ?? bot("HastyBot");
  if (!players) overrideWith(rival.theme);
  const view = gameView(session, {
    me: { name: players ? players[0] : "Tú" },
    opponent: players ? { name: players[1] } : { name: rival.alias, photo: botPhoto(rival) },
    local: !!players,
    pausable: true,
    thinkingText: players ? null : "pensando…",
    exit: () => go(""),
    resign: () => (session.resign(), go("")),
    onEnd: (state) => endDialog(state, session, rival),
  });
  keep(view);
  holder.replaceWith(view.el);
}

async function waitFor(holder, promise) {
  const loading = engineLoading();
  replace(holder, loading.card);
  const result = await promise.catch(() => null);
  loading.stop();
  return result;
}

function endDialog(state, session, rival) {
  const players = session.setup.players;
  const second = players ? players[1] : rival.alias;
  if (!players && state.result.outcome === "win") confetti();
  openDialog((close) => [
    h("h2.verdict", {}, players ? localVerdict(state, players[0], second) : verdict(state, second)),
    h("p.final-score", {}, `${state.myScore} – ${state.opponentScore}`),
    endDetail(state, second),
    endTools(() => session.recorded, state, { me: players ? players[0] : "Tú", opponent: second }, close),
    actions(
      h("button.text-btn", { onclick: () => (close(), go("")) }, "Salir"),
      h("button.text-btn", { onclick: close }, "Ver tablero"),
      h("button.text-btn", { onclick: () => (close(), rematch(session)) }, session.setup.endgame ? "Otro final" : "Revancha"),
    ),
  ]);
}

async function rematch(session) {
  await startAgain(session.setup);
  go("partida", true);
}
