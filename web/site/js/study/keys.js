const SPELLED = { C: "ch", L: "ll", R: "rr" };
const DIGRAPHS = { ch: "C", ll: "L", rr: "R" };
const ACCENTS = { á: "a", é: "e", í: "i", ó: "o", ú: "u", ü: "u" };
const WORD = "w:";

export function show(key) {
  return [...key].map((c) => SPELLED[c] ?? c).join("");
}

export function upper(key) {
  return show(key).toUpperCase();
}

export function label(tile) {
  return upper(tile);
}

export function tilesOf(key) {
  return [...key].map(label);
}

export function keyOf(tiles) {
  return tiles.map((t) => DIGRAPHS[t.toLowerCase()] ?? t.toLowerCase()).join("");
}

export function alphabetic(key) {
  return show(key).replace(/ñ/g, "n~");
}

export function entry(text) {
  return text.includes(":") ? text : wordEntry(parse(text));
}

export function wordEntry(key) {
  return WORD + key;
}

export function bare(entryText) {
  return entryText.slice(entryText.indexOf(":") + 1);
}

export function parse(text) {
  const lower = text.toLowerCase();
  let out = "";
  for (let i = 0; i < lower.length; ) {
    const digraph = DIGRAPHS[lower.slice(i, i + 2)];
    if (digraph) {
      out += digraph;
      i += 2;
      continue;
    }
    const c = ACCENTS[lower[i]] ?? lower[i];
    if ((c >= "a" && c <= "z") || c === "ñ") out += c;
    i++;
  }
  return out;
}
