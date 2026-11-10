import { h } from "./dom.js";
import { icon } from "./icons.js";
import { go } from "./router.js";
import { onLoading } from "./engine.js";

export function screenHead(title, backTo = "", ...extra) {
  return h("header.screen-head", {},
    h("button.icon-btn", { "aria-label": "Volver", onclick: () => go(backTo) }, icon("back")),
    h("h1", {}, title),
    extra,
  );
}

export function brand() {
  return h("a.brand", { href: "#/" },
    h("img", { src: "img/logo.svg", alt: "", width: 48, height: 41 }),
    h("span", {}, "Poliléxico"),
  );
}

export function flag(country) {
  if (!country || country.length !== 2) return "";
  return [...country.toUpperCase()].map((c) => String.fromCodePoint(0x1f1e6 + c.charCodeAt(0) - 65)).join("");
}

export function nameFlag(name, country) {
  return h("span.name-flag.ellipsis", {}, name, country ? " " + flag(country) : "");
}

export function initialAvatar(name, size = 40) {
  return h("span.initial", { style: { width: size + "px", height: size + "px", fontSize: size * 0.45 + "px" } }, (name || "?")[0].toUpperCase());
}

export function engineLoading() {
  const bar = h("div", {});
  const percent = h("span.muted", {});
  const card = h("div.card.loading-card", {}, h("div.row", {}, h("span.grow", {}, "Preparando el motor"), percent), h("div.progress", {}, bar));
  const stop = onLoading((p) => {
    bar.style.width = p.percent + "%";
    percent.textContent = loadingLabel(p);
  });
  return { card, stop };
}

export function loadingLabel(p) {
  if (p.failure) return "Error";
  if (p.stalled) return "Reintentando…";
  if (p.ready) return "";
  return p.percent >= 100 ? "Preparando…" : `${p.percent} %`;
}

export function engineGate(button, pack = "engine") {
  const badge = h("span.badge.loading-badge", {});
  button.append(badge);
  return onLoading((p) => {
    button.disabled = !p.ready;
    badge.textContent = loadingLabel(p);
    badge.classList.toggle("hidden", p.ready);
  }, pack);
}

export function formatClock(ms) {
  const negative = ms < 0;
  const total = Math.ceil(Math.abs(ms) / 1000);
  const text = `${Math.floor(total / 60)}:${String(total % 60).padStart(2, "0")}`;
  return negative ? "−" + text : text;
}

export function parseDuration(text) {
  const m = /^\s*(\d{1,3})(?::(\d{1,2}))?\s*$/.exec(text ?? "");
  if (!m) return null;
  const seconds = Number(m[2] ?? 0);
  return seconds < 60 ? (Number(m[1]) * 60 + seconds) * 1000 : null;
}
