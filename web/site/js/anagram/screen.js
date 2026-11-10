import { go } from "../router.js";
import { currentAnagram } from "./setup.js";
import { anagramView } from "./view.js";

export function anagramGameScreen(app) {
  const session = currentAnagram();
  if (!session) return go("anagrama", true);
  const view = anagramView(session, () => go("minijuegos"));
  app.append(view.el);
  return () => view.destroy();
}
