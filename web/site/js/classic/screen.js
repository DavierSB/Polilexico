import { h, replace } from "../dom.js";
import { go } from "../router.js";
import { overrideWith } from "../theme.js";
import { engineLoading } from "../parts.js";
import { openDialog, actions } from "../dialogs.js";
import { bot, botPhoto } from "./bots.js";
import { currentSession, resumeSaved, startAgain, hasSaved } from "./session.js";
import { gameView } from "./view.js";
import { verdict, endDetail } from "./verdict.js";

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
  const rival = bot(session.setup.bot) ?? bot("HastyBot");
  overrideWith(rival.theme);
  const view = gameView(session, {
    me: { name: "Tú" },
    opponent: { name: rival.alias, photo: botPhoto(rival) },
    pausable: true,
    thinkingText: "pensando…",
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
  openDialog((close) => [
    h("h2.verdict", {}, verdict(state, rival.alias)),
    h("p.final-score", {}, `${state.myScore} – ${state.opponentScore}`),
    endDetail(state, rival.alias),
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
