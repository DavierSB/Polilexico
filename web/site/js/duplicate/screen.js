import { h, replace } from "../dom.js";
import { go } from "../router.js";
import { engineLoading } from "../parts.js";
import { currentDuplicate, hasSavedDuplicate, resumeDuplicate } from "./session.js";
import { duplicateView } from "./view.js";

export function duplicateGameScreen(app) {
  const holder = h("div.screen");
  app.append(holder);
  let view = null;
  open(holder, (v) => (view = v));
  return () => view?.destroy();
}

async function open(holder, keep) {
  const session = currentDuplicate() ?? (hasSavedDuplicate() ? await waitFor(holder, resumeDuplicate()) : null);
  if (!session) return go("duplicada", true);
  const view = duplicateView(session, { exit: () => go(""), resign: () => (session.resign(), go("")) });
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
