import { h } from "./dom.js";
import { go } from "./router.js";
import { save } from "./store.js";
import { engineGate } from "./parts.js";

export function savedButtons(mode, inProgress, route, gates) {
  const resume = h("button.btn.outlined", { onclick: () => go(route) }, `En curso (${inProgress})`);
  if (inProgress) gates.push(engineGate(resume));
  else resume.disabled = true;
  const finished = h("button.btn.outlined", { onclick: () => (save("gamesTab", mode), go("terminadas/" + mode)) }, "Terminadas");
  return h("div.saved-buttons", {}, resume, finished);
}

export function keepScroll(holder, draw) {
  const top = holder.querySelector(".setup-scroll")?.scrollTop ?? 0;
  draw();
  const scroll = holder.querySelector(".setup-scroll");
  if (scroll) scroll.scrollTop = top;
}
