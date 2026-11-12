const PALETTE = [
  ["#F2DCA8", "#C9A96A", "#3B2A1A"],
  ["#F4B6A6", "#D2826F", "#4A1E14"],
  ["#B9E3CF", "#7FB89E", "#173A2C"],
  ["#B8D6F2", "#7FA6CC", "#142C45"],
  ["#D4C6EE", "#A08FC8", "#2A1D47"],
  ["#F2C2D8", "#CC8AA8", "#45182D"],
  ["#F1E58E", "#C8B850", "#3A3310"],
  ["#5B6B7A", "#3E4B57", "#F4F1EA"],
  ["#8E3B4A", "#6A2834", "#FBEDEF"],
  ["#3F6B55", "#2B4D3C", "#EEF6F1"],
];

const SESSION_SEED = (Math.random() * 2 ** 32) >>> 0;

export function tileColorsOf(name) {
  const [face, edge, ink] = PALETTE[mix(hash((name ?? "").trim().toLowerCase()) ^ SESSION_SEED) % PALETTE.length];
  return { face, edge, ink };
}

function hash(text) {
  let h = 0;
  for (const c of text) h = (Math.imul(h, 31) + c.codePointAt(0)) | 0;
  return h;
}

function mix(x) {
  x = Math.imul(x ^ (x >>> 16), 0x45d9f3b);
  x = Math.imul(x ^ (x >>> 16), 0x45d9f3b);
  return (x ^ (x >>> 16)) >>> 0;
}
