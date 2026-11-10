import { go } from "../router.js";
import { currentSprint, startSprint } from "./session.js";
import { sprintView } from "./view.js";

export function sprintGameScreen(app) {
  const session = currentSprint();
  if (!session) return go("sprint", true);
  const view = sprintView(session, {
    exit: () => (session.close(), go("minijuegos")),
    restart: () => startSprint(session.setup).then(() => go("sprint-partida", true)),
  });
  app.append(view.el);
  return () => view.destroy();
}
