import { h } from "../dom.js";
import { formatClock, initialAvatar, flag } from "../parts.js";
import { BLANK } from "../tiles.js";

export function playerCard({ name, country, photo, mine }) {
  const nameEl = h("span", {}, name);
  const score = h("span.score");
  const clock = h("span.clock.hidden");
  const tiles = h("div.mini-tiles");
  const status = h("span.player-status.muted");
  const el = h("div", { class: "player-card" + (mine ? " mine" : "") },
    avatar(name, photo, mine),
    h("div.player-main", {}, h("div.row", {}, h("span.player-name.ellipsis", {}, nameEl, country ? " " + flag(country) : ""), status), tiles),
    score,
    clock,
  );
  return { el, update: (data) => update({ el, score, clock, tiles, status, nameEl }, data) };
}

function avatar(name, photo, mine) {
  if (mine) return null;
  if (photo) return h("img.avatar", { src: photo, alt: "", width: 44, height: 44 });
  return initialAvatar(name, 44);
}

function update(parts, { score, active, ms, timed, tileCount, letters, status, name }) {
  if (name !== undefined) parts.nameEl.textContent = name;
  parts.el.classList.toggle("active", !!active);
  parts.score.textContent = score;
  parts.clock.classList.toggle("hidden", !timed);
  if (timed) clockText(parts.clock, ms, active);
  parts.status.textContent = status ?? "";
  if (letters) faceUpTiles(parts.tiles, letters);
  else if (tileCount !== undefined) miniTiles(parts.tiles, tileCount);
}

function clockText(el, ms, active) {
  el.textContent = formatClock(ms);
  el.classList.toggle("running", !!active);
  el.classList.toggle("overtime", ms < 0);
}

function miniTiles(el, count) {
  if (el.childElementCount === count && !el.querySelector(".face-up")) return;
  el.replaceChildren(...Array.from({ length: count }, () => h("span.mini-tile")));
}

function faceUpTiles(el, letters) {
  if (el.dataset.letters === letters.join(" ")) return;
  el.dataset.letters = letters.join(" ");
  el.replaceChildren(...letters.map((l) => h("span.mini-tile.face-up", {}, l === BLANK ? "" : l)));
}
