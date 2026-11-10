import { go } from "../router.js";
import { currentRecall } from "./setup.js";
import { recallView } from "./view.js";

export function recallGameScreen(app) {
  const session = currentRecall();
  if (!session) return go("recuerdas", true);
  const view = recallView(session, () => go("minijuegos"));
  app.append(view.el);
  return () => view.destroy();
}
