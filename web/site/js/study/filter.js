import { label, parse, upper } from "./keys.js";

export const MIN_TILES = 2;
export const MAX_TILES = 10;
export const FREE = ".";
const SIMPLE_LETTERS = "aeioucdlnrst";
const VOWELS = "aeiou";
export const KINDS = ["ALL", "VERBAL", "NON_VERBAL"];
export const ORDERS = { POINTS: "Importancia", ALPHABETIC: "A–Z", LENGTH: "Longitud" };

const DEFAULTS = { minLength: MIN_TILES, maxLength: MAX_TILES, contains: "", fixed: "", fixedEnd: "", kind: "ALL", order: "POINTS", vowels: null, consonants: null, simpleOnly: false, words: false, start: "", end: "" };

export function spec(fields = {}) {
  return new FilterSpec({ ...DEFAULTS, ...fields });
}

export class FilterSpec {
  constructor(fields) {
    Object.assign(this, fields);
  }

  with(changes) {
    return new FilterSpec({ ...this, ...changes });
  }

  filter() {
    return new Filter({
      minLength: this.minLength, maxLength: this.maxLength, contains: parse(this.contains), fixed: slots(this.fixed, this.maxLength),
      fixedEnd: slots(this.fixedEnd, this.maxLength), kind: this.kind, vowels: this.vowels, consonants: this.consonants,
      simpleOnly: this.simpleOnly, start: parse(this.start), end: parse(this.end),
    });
  }

  get isEmpty() {
    return FILTER_TYPES.every((t) => !t.line(this));
  }

  get unit() {
    return this.words ? "palabras" : "familias";
  }

  summary() {
    return FILTER_TYPES.map((t) => t.line(this)).filter(Boolean);
  }

  encode() {
    const clean = (s) => s.replace(/[|\t\n]/g, "");
    return [this.minLength, this.maxLength, clean(this.contains), clean(this.fixed), clean(this.fixedEnd), this.kind, this.order,
      this.vowels ?? "", this.consonants ?? "", this.simpleOnly ? "s" : "", this.words ? "p" : "f", clean(this.start), clean(this.end)].join("|");
  }

  static decode(text) {
    const c = text.split("|");
    if (c.length !== 11 && c.length !== 13) return null;
    return spec({
      minLength: Number(c[0]), maxLength: Number(c[1]), contains: c[2], fixed: c[3], fixedEnd: c[4], kind: c[5], order: c[6],
      vowels: c[7] === "" ? null : Number(c[7]), consonants: c[8] === "" ? null : Number(c[8]), simpleOnly: c[9] === "s", words: c[10] === "p",
      start: c[11] ?? "", end: c[12] ?? "",
    });
  }
}

function slots(text, maxLength) {
  const out = new Map();
  [...text].forEach((c, i) => c !== FREE && i < maxLength && out.set(i, c));
  return out;
}

class Filter {
  constructor(f) {
    Object.assign(this, f);
    this.need = counts(f.contains);
  }

  matches(unit) {
    return typeof unit === "string" ? this.wordMatches(unit) : this.kindMatches(unit) && this.inLength(shortest(unit)) && unit.words.some((w) => this.shape(w));
  }

  wordMatches(word) {
    return this.inLength(word.length) && this.shape(word);
  }

  inLength(n) {
    return n >= this.minLength && n <= this.maxLength;
  }

  kindMatches(f) {
    return this.kind === "ALL" || (this.kind === "VERBAL" ? f.verbal : f.nonVerbal);
  }

  shape(w) {
    return w.startsWith(this.start) && w.endsWith(this.end) && this.positions(w) && this.counted(w) && this.letters(w);
  }

  positions(w) {
    for (const [i, c] of this.fixed) if (w[i] !== c) return false;
    for (const [i, c] of this.fixedEnd) if (w[w.length - 1 - i] !== c) return false;
    return true;
  }

  counted(w) {
    const v = vowelsIn(w);
    return (this.vowels === null || v === this.vowels) && (this.consonants === null || w.length - v === this.consonants);
  }

  letters(w) {
    const have = counts(w);
    for (const [c, n] of this.need) if ((have.get(c) ?? 0) < n) return false;
    if (!this.simpleOnly) return true;
    for (const [c, n] of have) if (!SIMPLE_LETTERS.includes(c) && n > (this.need.get(c) ?? 0)) return false;
    return true;
  }
}

function shortest(f) {
  return Math.min(...f.words.map((w) => w.length)) - (f.verbal && !f.isWord ? 1 : 0);
}

function counts(text) {
  const out = new Map();
  for (const c of text) out.set(c, (out.get(c) ?? 0) + 1);
  return out;
}

function vowelsIn(w) {
  let n = 0;
  for (const c of w) if (VOWELS.includes(c)) n++;
  return n;
}

const count = (n, one, many) => (n === 1 ? `1 ${one}` : `${n} ${many}`);

function containsLine(contains) {
  const tiles = [...contains].map(label);
  if (!tiles.length) return null;
  if (tiles.length > 1 && new Set(tiles).size === 1) return `Con la letra ${tiles[0]} ` + (tiles.length === 2 ? "duplicada" : `${tiles.length} veces`);
  return tiles.length === 1 ? `Con la letra ${tiles[0]}` : `Con las letras ${tiles.slice(0, -1).join(", ")} y ${tiles.at(-1)}`;
}

function positionsLine(fixed, suffix) {
  if (!fixed.size) return null;
  return [...fixed].sort((a, b) => a[0] - b[0]).map(([i, c]) => `${label(c)} en la ${i + 1}ª`).join(", ") + ` posición${suffix}`;
}

export const FILTER_TYPES = [
  { id: "KIND", title: "Tipo de familia", info: "Incluir solo familias verbales o solo familias no verbales.", line: (s) => ({ VERBAL: "Familias verbales", NON_VERBAL: "Familias no verbales" })[s.kind] ?? null, clear: { kind: "ALL" } },
  { id: "LENGTH", title: "Longitud", info: "Longitud de la palabra más corta de la familia.", line: (s) => lengthLine(s.minLength, s.maxLength), clear: { minLength: MIN_TILES, maxLength: MAX_TILES } },
  { id: "COUNTS", title: "Vocales y consonantes", info: "Número de vocales y consonantes.", line: (s) => countsLine(s.vowels, s.consonants), clear: { vowels: null, consonants: null } },
  { id: "CONTAINS", title: "Letras que debe contener", info: "Letras que deben aparecer dentro de la palabra en cualquier orden.", line: (s) => lettersLine(parse(s.contains), s.simpleOnly), clear: { contains: "", simpleOnly: false } },
  { id: "START", title: "Comienzo", info: "Fija un comienzo para las palabras.", line: (s) => (parse(s.start) ? `Empieza por ${upper(parse(s.start))}` : null), clear: { start: "" } },
  { id: "END", title: "Final", info: "Fija una terminación para las palabras.", line: (s) => (parse(s.end) ? `Termina en ${upper(parse(s.end))}` : null), clear: { end: "" } },
  { id: "FIXED", title: "Letras en posición", info: "Fija letras intermedias para la palabra.", line: (s) => positionsLine(s.filter().fixed, ""), clear: { fixed: "" } },
  { id: "FIXED_END", title: "Letras en posición inversa", info: "Fija letras en posiciones contadas de atrás hacia adelante.", line: (s) => positionsLine(s.filter().fixedEnd, " desde el final"), clear: { fixedEnd: "" } },
];

function lengthLine(min, max) {
  if (min === max) return `Exactamente ${count(min, "letra", "letras")}`;
  if (min > MIN_TILES && max < MAX_TILES) return `De ${min} a ${max} letras`;
  if (min > MIN_TILES) return `Al menos ${count(min, "letra", "letras")}`;
  if (max < MAX_TILES) return `A lo más ${count(max, "letra", "letras")}`;
  return null;
}

function countsLine(vowels, consonants) {
  const parts = [vowels !== null ? count(vowels, "vocal", "vocales") : null, consonants !== null ? count(consonants, "consonante", "consonantes") : null].filter(Boolean);
  return parts.length ? "Exactamente " + parts.join(" y ") : null;
}

function lettersLine(contains, simpleOnly) {
  const letters = containsLine(contains);
  if (!letters) return simpleOnly ? "Solo letras simples" : null;
  return simpleOnly ? `${letters}; las demás, simples` : letters;
}
