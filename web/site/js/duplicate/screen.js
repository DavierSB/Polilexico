import { h, replace } from "../dom.js";
import { go } from "../router.js";
import { engineLoading } from "../parts.js";
import { currentDuplicate, openSavedDuplicate } from "./session.js";
import { duplicateView } from "./view.js";

export function duplicateGameScreen(app, [id]) {
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
  const live = currentDuplicate();
  if (live && (!id || live.id === id)) return live;
  return id ? waitFor(holder, openSavedDuplicate(id)) : null;
}

async function open(holder, id, shown) {
  const session = await sessionFor(holder, id);
  if (shown.left) return session?.close();
  if (!session) return go("duplicada", true);
  shown.session = session;
  shown.view = duplicateView(session, { exit: () => go(""), resign: () => (session.resign(), go("")) });
  holder.replaceWith(shown.view.el);
}

async function waitFor(holder, promise) {
  const loading = engineLoading();
  replace(holder, loading.card);
  const result = await promise.catch(() => null);
  loading.stop();
  return result;
}
