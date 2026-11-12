const FONT = "Twemoji Country Flags";
const SAMPLE = "\u{1F1FA}\u{1F1F8}";
const SIZE = 20;

export function ensureFlags() {
  if (drawsFlags()) return;
  const style = document.createElement("style");
  style.textContent = `@font-face { font-family: "${FONT}"; unicode-range: U+1F1E6-1F1FF, U+1F3F4, U+E0062-E007F; src: url("fonts/twemoji_flags.woff2") format("woff2"); font-display: swap; }`;
  document.head.append(style);
  document.documentElement.classList.add("flag-font");
}

function drawsFlags() {
  const ctx = document.createElement("canvas").getContext("2d", { willReadFrequently: true });
  if (!ctx) return true;
  ctx.canvas.width = ctx.canvas.height = SIZE;
  ctx.font = `${SIZE - 4}px sans-serif`;
  ctx.textBaseline = "top";
  ctx.fillText(SAMPLE, 0, 0);
  return colored(ctx.getImageData(0, 0, SIZE, SIZE).data);
}

function colored(data) {
  for (let i = 0; i < data.length; i += 4) {
    if (data[i + 3] && (data[i] !== data[i + 1] || data[i + 1] !== data[i + 2])) return true;
  }
  return false;
}
