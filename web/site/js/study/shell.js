import { h, replace } from "../dom.js";
import { onLoading } from "../engine.js";
import { loadingLabel } from "../parts.js";
import { studyData } from "./data.js";

export function withStudy(app, render) {
  const bar = h("div");
  const percent = h("span.muted", {});
  const card = h("div.card.loading-card", {}, h("div.row", {}, h("span.grow", {}, "Preparando el diccionario"), percent), h("div.progress", {}, bar));
  const holder = h("div.screen", {}, card);
  app.append(holder);
  const stop = onLoading((p) => {
    bar.style.width = p.percent + "%";
    percent.textContent = loadingLabel(p);
  }, "study");
  let cleanup = null;
  let gone = false;
  studyData().then((data) => {
    stop();
    if (gone) return;
    holder.remove();
    cleanup = render(data);
  });
  return () => {
    gone = true;
    stop();
    cleanup?.();
  };
}

export function label(text, extra = "") {
  return h("span", { class: "list-label " + extra }, text.toUpperCase());
}

export function chips(...texts) {
  return h("div.row.list-chips", {}, texts.filter(Boolean).map((t) => h("span.list-chip", {}, t.toUpperCase())));
}

export function dotted(text) {
  return h("div.row.dotted", {}, h("span.dot-mark"), h("b", {}, text));
}

export function big(text) {
  return h("b.big-number", {}, text);
}

export function progressBar(percent) {
  return h("div.progress", {}, h("div", { style: { width: percent + "%" } }));
}

export function replaceAll(el, ...children) {
  return replace(el, ...children);
}
