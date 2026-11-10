import { overrideWith } from "./theme.js";

let routes = {};
let cleanup = null;

export function start(table) {
  routes = table;
  window.addEventListener("hashchange", show);
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
  const [name, ...rest] = location.hash.replace(/^#\/?/, "").split("/");
  const screen = routes[name] ?? routes[""];
  cleanup?.();
  overrideWith(null);
  const app = document.getElementById("app");
  app.replaceChildren();
  window.scrollTo(0, 0);
  cleanup = screen(app, rest.map(decodeURIComponent)) ?? null;
}
