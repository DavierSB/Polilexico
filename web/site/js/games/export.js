import { h } from "../dom.js";
import { openDialog, actions, toast } from "../dialogs.js";
import { SIZE, bonusAt, blankFace, value, parseBoard } from "../tiles.js";
import { parsePlacement, applyPlacement, spelled } from "../placement.js";
import { call, whenReady } from "../engine.js";
import { encodeGif } from "./gif.js";
import { alias } from "./list.js";
import { LABELS } from "../theme.js";

const WIDTH = 1080;
const MARGIN = 60;
const HEADER = 220;
const FOOTER = 90;
const JPG_QUALITY = 0.92;
const STILL_WIDTH = 600;
const CAPTION = 96;
const STILL_HEIGHT = STILL_WIDTH + CAPTION;
const DELAY_CS = 120;
const LAST_DELAY_CS = 500;
const LOGO_HEIGHT = 50;
const LOGO_MARGIN = 16;

export function exportDialog(picture, record) {
  openDialog((close) => [
    h("h2", {}, "Exportar partida"),
    h("div.stack", {},
      h("button.btn.block", { onclick: () => (close(), record ? exporting(() => recordJpg(record)) : saveJpg(picture)) }, "Imagen (.jpg)"),
      record ? h("button.btn.block", { onclick: () => (close(), exporting(() => recordGif(record))) }, "Animación (.gif)") : null,
      record?.gcg ? h("button.btn.block.outlined", { onclick: () => (close(), saveGcg(record)) }, "Formato Scrabble (.gcg)") : null,
    ),
    actions(h("button.text-btn", { onclick: close }, "Cancelar")),
  ]);
}

async function exporting(work) {
  const close = openDialog(() => [h("div.row.exporting", {}, h("div.spinner.small"), h("div.stack", {}, h("span", {}, "Exportando la partida…"), h("span.small.muted", {}, "Esto podría tardar un poco.")))], { dismissable: false });
  await new Promise((r) => setTimeout(r, 50));
  try {
    await work();
    toast("Partida exportada.");
  } catch {
    toast("No se pudo exportar la partida.");
  }
  close();
}

async function recordJpg(record) {
  const stills = await stillsOf(record);
  const canvas = await stillCanvas(stills.at(-1));
  const blob = await new Promise((r) => canvas.toBlob(r, "image/jpeg", JPG_QUALITY));
  download(blob, `polilexico-${stamp(record.finishedAt)}.jpg`);
}

async function recordGif(record) {
  const stills = await stillsOf(record);
  const frames = [];
  for (const [i, still] of stills.entries()) {
    const canvas = await stillCanvas(still);
    frames.push({ pixels: canvas.getContext("2d").getImageData(0, 0, STILL_WIDTH, STILL_HEIGHT).data, delayCs: i === stills.length - 1 ? LAST_DELAY_CS : DELAY_CS });
    if (i % 5 === 4) await new Promise((r) => setTimeout(r));
  }
  download(encodeGif(STILL_WIDTH, STILL_HEIGHT, frames), `polilexico-${stamp(record.finishedAt)}.gif`);
}

async function stillsOf(record) {
  await whenReady();
  const game = await call("reviewOpen", record.text);
  const turns = game.Turns ?? [];
  if (!turns.length) throw new Error("vacía");
  const duplicate = game.Mode === "duplicate";
  const opponent = duplicate ? "Máster" : alias(game.Opponent);
  const score = (mine, theirs) => `Tú ${mine} – ${theirs} ${opponent}`;
  const title = duplicate ? "Duplicada" : `Partida contra ${opponent}`;
  const first = parseBoard(turns[0].Board);
  return [{ cells: first, score: score(turns[0].MyScore, turns[0].OpponentScore), caption: title }, ...turns.map((t, i) => {
    const next = turns[i + 1];
    const cells = parseBoard(t.Board);
    const mark = duplicate ? t.Marks?.find((m) => m.Who !== "Tú") : t.Marks?.[0];
    const p = mark && parsePlacement(mark.Description.replace(/ \(\d+ pts\)$/, ""));
    const after = p ? applyPlacement(cells, p) : next ? parseBoard(next.Board) : cells;
    const total = next ? score(next.MyScore, next.OpponentScore) : score(game.MyScore, game.OpponentScore);
    return { cells: after, score: total, caption: mark ? `${alias(mark.Who)}: ${p ? spelled(p, cells) : mark.Description} (${mark.Score})` : "" };
  })];
}

let logo = null;

async function stillCanvas(still) {
  logo ??= new Promise((resolve) => {
    const img = new Image();
    img.onload = () => resolve(img);
    img.onerror = () => resolve(null);
    img.src = "img/logo.svg";
  });
  const image = await logo;
  const canvas = Object.assign(document.createElement("canvas"), { width: STILL_WIDTH, height: STILL_HEIGHT });
  const ctx = canvas.getContext("2d");
  const color = palette();
  ctx.fillStyle = color("bg");
  ctx.fillRect(0, 0, STILL_WIDTH, STILL_HEIGHT);
  if (image) ctx.drawImage(image, LOGO_MARGIN, (CAPTION - LOGO_HEIGHT) / 2, LOGO_HEIGHT * 1.16, LOGO_HEIGHT);
  ctx.textAlign = "center";
  ctx.textBaseline = "middle";
  ctx.fillStyle = color("b-bonus-text");
  ctx.font = "700 30px Mulish, sans-serif";
  ctx.fillText(still.score, STILL_WIDTH / 2, CAPTION * 0.32, STILL_WIDTH - 2 * (LOGO_MARGIN * 2 + LOGO_HEIGHT * 1.16));
  ctx.font = "400 24px Mulish, sans-serif";
  ctx.fillText(still.caption, STILL_WIDTH / 2, CAPTION * 0.74, STILL_WIDTH - 2 * LOGO_MARGIN);
  squares(ctx, still.cells, 0, CAPTION, STILL_WIDTH / SIZE, color);
  return canvas;
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
