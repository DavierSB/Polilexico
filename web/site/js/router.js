import { overrideWith } from "./theme.js";

let routes = {};
let cleanup = null;
let shown = 0;
let shownHash = null;

export function start(table) {
  routes = table;
  window.addEventListener("hashchange", () => location.hash !== shownHash && show());
  show();
}

export function go(path, replaceCurrent = false) {
  const target = "#/" + path;
  if (location.hash === target) return show();
  if (replaceCurrent) {
    history.replaceState(null, "", target);
    return show();
  }
  location.hash = target;
}

function show() {
  const turn = ++shown;
  shownHash = location.hash;
  const [name, ...rest] = location.hash.replace(/^#\/?/, "").split("/");
  const screen = routes[name] ?? routes[""];
  leave();
  overrideWith(null);
  const app = document.getElementById("app");
  app.replaceChildren();
  window.scrollTo(0, 0);
  const done = screen(app, rest.map(decodeURIComponent)) ?? null;
  if (turn === shown) cleanup = done;
  else done?.();
}

function leave() {
  const previous = cleanup;
  cleanup = null;
  previous?.();
}
