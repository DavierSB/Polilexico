import { alphabetic, bare } from "./keys.js";

const WORD = "w:";
const TRAITS = [["i", "Intransitivo", "intr."], ["m", "Impersonal"], ["u", "Unipersonal"], ["d", "Defectivo"], ["p", "Participio irregular"], ["x", "Desuso", "desus."]];
const VERB_ENDINGS = ["ar", "er", "ir"];

export class Family {
  constructor({ verbal, nonVerbal, points = 0, permille = 0, code = "", words, isWord = false, owners = [], index = -1 }) {
    Object.assign(this, { verbal, nonVerbal, points, permille, code, words, isWord, owners, index });
  }

  get head() {
    return this.words[0];
  }

  get key() {
    return (this.isWord ? "w:" : this.verbal ? "v:" : "n:") + this.head;
  }

  traits(word) {
    if (!this.verbal || this.isWord) return [];
    const codes = this.code.split(" ").find((c) => c.startsWith(word + ":"))?.split(":")[1] ?? "";
    return TRAITS.filter(([c]) => codes.includes(c)).map(([, label, tag]) => ({ label, tag }));
  }
}

class WordTable {
  constructor(lines) {
    this.words = [];
    this.kinds = [];
    this.permilles = [];
    this.owners = [];
    for (const line of lines) {
      const c = line.split("\t");
      if (c.length !== 4) continue;
      this.kinds.push(c[0][0]);
      this.permilles.push(Number(c[1]));
      this.owners.push(c[2] ? c[2].split(",").filter(Boolean).map(Number) : []);
      this.words.push(c[3]);
    }
    this.alphabetic = [...this.words.keys()].sort((a, b) => (this.words[a] < this.words[b] ? -1 : this.words[a] > this.words[b] ? 1 : 0));
    this.cache = new Map();
  }

  get size() {
    return this.words.length;
  }

  get(i) {
    if (!this.cache.has(i)) this.cache.set(i, new Family({ verbal: this.kinds[i] !== "n", nonVerbal: this.kinds[i] !== "v", permille: this.permilles[i], words: [this.words[i]], isWord: true, owners: this.owners[i] }));
    return this.cache.get(i);
  }

  find(word) {
    let low = 0;
    let high = this.size - 1;
    while (low <= high) {
      const mid = (low + high) >>> 1;
      const w = this.words[this.alphabetic[mid]];
      if (w === word) return this.get(this.alphabetic[mid]);
      if (w < word) low = mid + 1;
      else high = mid - 1;
    }
    return null;
  }

  byFamily(count) {
    const out = Array.from({ length: count }, () => []);
    this.owners.forEach((owners, word) => owners.forEach((o) => o < count && out[o].push(word)));
    return out;
  }

  all() {
    return Array.from({ length: this.size }, (_, i) => this.get(i));
  }
}

export class Lexicon {
  constructor(files, dictionary) {
    this.families = files["families.tsv"].split("\n").map(parseFamily).filter(Boolean).map((f, index) => Object.assign(f, { index }));
    this.wordLines = files["words.tsv"];
    this.similarLines = files["similar.tsv"].split("\n");
    this.dictionary = dictionary;
    this.byKey = new Map(this.families.map((f) => [f.key, f]));
    this.sorted = new Map();
  }

  get table() {
    this._table ??= new WordTable(this.wordLines.split("\n"));
    return this._table;
  }

  get words() {
    this._words ??= this.table.all();
    return this._words;
  }

  get members() {
    if (!this._members) {
      const pairs = this.families.flatMap((f, i) => f.words.map((w) => [w, i]));
      this._members = new Map();
      pairs.forEach(([w, i]) => !this._members.has(w) && this._members.set(w, i));
    }
    return this._members;
  }

  units(words) {
    return words ? this.words : this.families;
  }

  filter(spec) {
    const filter = spec.filter();
    return this.ordered(spec.words, spec.order).filter((f) => filter.matches(f));
  }

  find(key) {
    return key.startsWith(WORD) ? this.word(key.slice(WORD.length)) : this.byKey.get(key) ?? null;
  }

  word(key) {
    return this.table.find(key);
  }

  familyOf(member) {
    const i = this.members.get(member);
    if (i !== undefined) return this.families[i];
    const owner = this.word(member)?.owners[0];
    return owner !== undefined ? this.families[owner] : null;
  }

  resolve(entryText) {
    return this.find(entryText) ?? this.familyOf(bare(entryText));
  }

  owners(word) {
    return word.owners.map((i) => this.families[i]);
  }

  forms(unit) {
    if (unit.isWord) return [unit];
    this._forms ??= this.table.byFamily(this.families.length);
    return (this._forms[unit.index] ?? []).map((i) => this.table.get(i));
  }

  similar(unit) {
    const index = unit.isWord ? unit.owners[0] : unit.index;
    if (index === undefined) return [];
    return (this.similarLines[index] ?? "").split(" ").filter((e) => /^[1-8]/.test(e)).map((e) => e.slice(1));
  }

  meanings(wordKey) {
    return this.dictionary.meanings(wordKey);
  }

  unitsOf(wordKey) {
    const word = this.word(wordKey);
    return word ? [word, ...this.owners(word)] : [];
  }

  tag(word) {
    const unit = this.word(word);
    if (!unit) return null;
    if (unit.verbal) return this.owners(unit).find((o) => o.verbal)?.traits(unit.head).find((t) => t.tag)?.tag ?? null;
    return VERB_ENDINGS.some((e) => word.endsWith(e)) ? "NoVerbo" : null;
  }

  ordered(words, order) {
    const id = `${words}:${order}`;
    if (!this.sorted.has(id)) this.sorted.set(id, sortUnits(this.units(words), order));
    return this.sorted.get(id);
  }
}

function parseFamily(line) {
  const c = line.split("\t");
  if (c.length !== 5) return null;
  const verbal = c[0] === "v";
  return new Family({ verbal, nonVerbal: !verbal, points: Number(c[1]), permille: Number(c[2]), code: c[3], words: c[4].split(" ") });
}

function sortUnits(units, order) {
  if (order === "POINTS") return units;
  const key = order === "ALPHABETIC" ? (u) => alphabetic(u.head) : (u) => u.head.length;
  const keys = units.map(key);
  return [...units.keys()].sort((a, b) => (keys[a] < keys[b] ? -1 : keys[a] > keys[b] ? 1 : a - b)).map((i) => units[i]);
}
