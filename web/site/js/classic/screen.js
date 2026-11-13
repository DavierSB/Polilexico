import { h, replace } from "../dom.js";
import { go } from "../router.js";
import { overrideWith } from "../theme.js";
import { engineLoading } from "../parts.js";
import { bot, botPhoto } from "./bots.js";
import { currentSession, openSaved } from "./session.js";
import { gameView } from "./view.js";
import { announceEnd, resultPanel } from "./ending.js";

export function classicGameScreen(app, [id]) {
  const holder = h("div.screen");
  app.append(holder);
  const shown = { view: null, session: null, left: false };
  open(holder, id, shown);
  return () => leave(shown);
}

function leave(shown) {
  shown.left = true;
  shown.view?.destroy();
  shown.session?.close();
}

async function sessionFor(holder, id) {
  const live = currentSession();
  if (live && (!id || live.id === id)) return live;
  return id ? waitFor(holder, openSaved(id)) : null;
}

async function open(holder, id, shown) {
  const session = await sessionFor(holder, id);
  if (shown.left) return session?.close();
  if (!session) return go("clasica", true);
  shown.session = session;
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
    onEnd: (state, names) => announceEnd(state, names, { local: !!players }),
    endPanel: (state, names) => resultPanel(state, names, { local: !!players }, { recorded: () => session.recorded, menu: () => go("") }),
  });
  shown.view = view;
  holder.replaceWith(view.el);
}

async function waitFor(holder, promise) {
  const loading = engineLoading();
  replace(holder, loading.card);
  const result = await promise.catch(() => null);
  loading.stop();
  return result;
}
