import { h } from "../dom.js";
import { openDialog, actions, toast } from "../dialogs.js";
import { SIZE, bonusAt, blankFace, value } from "../tiles.js";
import { LABELS } from "../theme.js";

const WIDTH = 1080;
const MARGIN = 60;
const HEADER = 220;
const FOOTER = 90;
const JPG_QUALITY = 0.92;

export function exportDialog(picture, record) {
  openDialog((close) => [
    h("h2", {}, "Exportar partida"),
    h("div.stack", {},
      picture ? h("button.btn.block", { onclick: () => (close(), saveJpg(picture)) }, "Imagen (.jpg)") : null,
      record?.gcg ? h("button.btn.block.outlined", { onclick: () => (close(), saveGcg(record)) }, "Formato Scrabble (.gcg)") : null,
    ),
    actions(h("button.text-btn", { onclick: close }, "Cancelar")),
  ]);
}

function saveGcg(record) {
  download(new Blob([record.gcg], { type: "text/plain" }), `polilexico-${stamp(record.finishedAt)}.gcg`);
}

function saveJpg(picture) {
  const canvas = draw(picture);
  canvas.toBlob((blob) => (blob ? download(blob, `polilexico-${stamp(Date.now())}.jpg`) : toast("No se pudo exportar.")), "image/jpeg", JPG_QUALITY);
}

function download(blob, name) {
  const url = URL.createObjectURL(blob);
  const link = h("a", { href: url, download: name });
  document.body.append(link);
  link.click();
  link.remove();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}

function stamp(time) {
  const d = new Date(time);
  const two = (n) => String(n).padStart(2, "0");
  return `${d.getFullYear()}${two(d.getMonth() + 1)}${two(d.getDate())}-${two(d.getHours())}${two(d.getMinutes())}`;
}

function draw(picture) {
  const board = WIDTH - MARGIN * 2;
  const canvas = document.createElement("canvas");
  canvas.width = WIDTH;
  canvas.height = HEADER + board + FOOTER;
  const ctx = canvas.getContext("2d");
  const color = palette();
  ctx.fillStyle = color("bg");
  ctx.fillRect(0, 0, canvas.width, canvas.height);
  header(ctx, picture, color);
  squares(ctx, picture.cells, MARGIN, HEADER, board / SIZE, color);
  footer(ctx, canvas.height, color);
  return canvas;
}

function palette() {
  const style = getComputedStyle(document.documentElement);
  return (name) => style.getPropertyValue("--" + name).trim() || "#888";
}

function header(ctx, picture, color) {
  ctx.textBaseline = "middle";
  ctx.fillStyle = color("on-surface");
  ctx.font = "800 64px Mulish, sans-serif";
  ctx.textAlign = "center";
  ctx.fillText(`${picture.scores[0]} – ${picture.scores[1]}`, WIDTH / 2, 90);
  ctx.font = "700 34px Mulish, sans-serif";
  ctx.fillStyle = color("on-surface-variant");
  ctx.fillText(`${picture.names[0]}  ·  ${picture.names[1]}`, WIDTH / 2, 160, WIDTH - MARGIN * 2);
}

function squares(ctx, cells, left, top, side, color) {
  for (let i = 0; i < SIZE * SIZE; i++) {
    const x = left + (i % SIZE) * side;
    const y = top + Math.floor(i / SIZE) * side;
    const bonus = bonusAt(Math.floor(i / SIZE), i % SIZE);
    rounded(ctx, x + 2, y + 2, side - 4, side - 4, side * 0.1, color(cells[i] ? "b-tile" : bonus ? "b-" + bonus : "b-square"));
    if (cells[i]) tileText(ctx, cells[i], x, y, side, color);
    else if (bonus) label(ctx, LABELS[bonus], x, y, side, color);
  }
}

function tileText(ctx, tile, x, y, side, color) {
  ctx.textAlign = "center";
  ctx.fillStyle = color(tile.blank ? "b-blank" : "b-letter");
  ctx.font = `700 ${side * (tile.letter.length > 1 ? 0.42 : 0.58)}px Mulish, sans-serif`;
  ctx.fillText(blankFace(tile), x + side / 2, y + side * 0.5);
  if (tile.blank) return;
  ctx.font = `600 ${side * 0.22}px Mulish, sans-serif`;
  ctx.textAlign = "right";
  ctx.fillText(value(tile), x + side * 0.9, y + side * 0.8);
}

function label(ctx, text, x, y, side, color) {
  ctx.textAlign = "center";
  ctx.fillStyle = color("b-bonus-text");
  ctx.font = `600 ${side * 0.3}px Mulish, sans-serif`;
  ctx.fillText(text, x + side / 2, y + side / 2);
}

function footer(ctx, height, color) {
  ctx.textAlign = "center";
  ctx.fillStyle = color("on-surface-variant");
  ctx.font = "700 30px Mulish, sans-serif";
  ctx.fillText("Poliléxico · polilexico.github.io", WIDTH / 2, height - FOOTER / 2);
}

function rounded(ctx, x, y, w, h, r, fill) {
  ctx.fillStyle = fill;
  ctx.beginPath();
  ctx.roundRect(x, y, w, h, r);
  ctx.fill();
}
