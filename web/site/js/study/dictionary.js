import { SortedLines } from "./lines.js";

const SOURCES = { w: ["Wikcionario", true], d: ["DRAE 1914", true], e: ["Wiktionary en inglés", false], g: ["Estudio de gentilicios", true] };
const SAME_SENSE = 0.8;
const SAME_ENGLISH_SENSE = 0.6;
const SHORTEST_COMPARED = 3;
const FUNCTION_WORDS = new Set("de del la las el los lo un una unos unas y o u e a al en con por para que se su sus como es son le les mas muy sin sobre the of or and to an in on for from with by as is be its it that which this".split(" "));
const DIGRAPHS = { C: "ch", L: "ll", R: "rr" };

export class Dictionary {
  constructor(files) {
    this.words = new SortedLines(files["words.txt"]);
    this.senses = new SortedLines(files["senses.txt"]);
    this.demonyms = new SortedLines(files["demonyms.txt"]);
    this.relations = files["relations.txt"].split("\n").filter(Boolean).map(parseRelation);
  }

  meanings(word) {
    const demonym = this.demonym(word);
    return distinct([...(demonym ? [demonym] : []), ...this.entries(word).map((e) => this.meaning(e)).filter(Boolean)]);
  }

  demonym(word) {
    const line = this.demonyms.find(word);
    if (!line) return null;
    return { lemma: plain(word), relation: "", senses: [{ text: line.slice(line.indexOf("\t") + 1), pos: "", source: "g", category: "a" }] };
  }

  entries(word) {
    const spelled = plain(word);
    const line = this.words.find(word);
    if (!line) return [{ lemma: spelled, relation: DIRECT, middle: null }];
    return line.slice(line.indexOf("\t") + 1).split(";").map((text) => parseEntry(spelled, text, this.relations));
  }

  meaning(entry) {
    const line = this.senses.find(entry.lemma);
    if (!line) return null;
    const parsed = parseSenseLine(line);
    return { lemma: parsed.lemma, relation: describe(entry.relation, parsed.lemma, entry.middle), senses: ordered(entry.relation, parsed.senses) };
  }
}

export function sourceLabel(code) {
  return SOURCES[code]?.[0] ?? "";
}

function plain(word) {
  return [...word].map((c) => DIGRAPHS[c] ?? c).join("");
}

function relative(base, code) {
  const digits = code.match(/^\d*/)[0];
  return base.slice(0, base.length - Number(digits || 0)) + code.slice(digits.length);
}

const DIRECT = { via: "directa", parts: [], category: "" };

function parseRelation(line) {
  const f = line.split("\t");
  return { via: f[0], parts: (f[1] ?? "").split("; ").filter(Boolean), category: f[2] ?? "" };
}

function parseEntry(spelled, text, relations) {
  const f = text.split(",");
  return { lemma: relative(spelled, f[0]), relation: relations[Number(f[1])] ?? DIRECT, middle: f[2] !== undefined ? relative(spelled, f[2]) : null };
}

function describe(rel, lemma, middle) {
  if (rel.via === "directa" || !rel.parts.length) return "";
  const text = rel.parts.map((part, i) => phrase(part, i === 0 && middle !== null && rel.parts.length > 1 ? middle : lemma)).join(", ");
  return text.charAt(0).toUpperCase() + text.slice(1);
}

function phrase(part, target) {
  return part.endsWith("-") ? `${part} + ${target}` : `${part} de ${target}`;
}

function ordered(rel, senses) {
  const fits = (s) => (rel.category === "verbo" ? s.category === "v" : rel.category === "nominal" ? s.category === "s" || s.category === "a" : false);
  return [...senses].sort((a, b) => fits(b) - fits(a));
}

function parseSenseLine(line) {
  const fields = line.split("\t");
  const groups = [];
  for (let i = 1; i + 4 <= fields.length; i += 4) groups.push(fields.slice(i, i + 4));
  return { lemma: groups[0]?.[0] || fields[0], senses: groups.map((g) => ({ text: g[3], pos: g[2], source: SOURCES[g[1][0]] ? g[1][0] : "w", category: g[1][1] ?? "o" })) };
}

function distinct(meanings) {
  const kept = [];
  return meanings.map((m) => ({ ...m, senses: m.senses.filter((s) => keep(s, kept)) })).filter((m) => m.senses.length);
}

function keep(sense, kept) {
  const words = vocabulary(sense.text);
  if (kept.some(([other, seen]) => overlap(words, seen) >= threshold(sense, other))) return false;
  kept.push([sense, words]);
  return true;
}

function threshold(a, b) {
  return !SOURCES[a.source][1] && !SOURCES[b.source][1] ? SAME_ENGLISH_SENSE : SAME_SENSE;
}

function overlap(a, b) {
  const smaller = Math.min(a.size, b.size);
  if (smaller < SHORTEST_COMPARED) return 0;
  let common = 0;
  a.forEach((w) => b.has(w) && common++);
  return common / smaller;
}

function vocabulary(text) {
  const bare = text.toLowerCase().normalize("NFD").replace(/[̀-ͯ]+/g, "");
  return new Set((bare.match(/[a-zñ]+/g) ?? []).filter((w) => !FUNCTION_WORDS.has(w)));
}

const LONG = 60;
const SHORTEST_CLAUSE = 12;
const LEAD = /^(?:(?:[Ee]n|[Pp]or) [^,:]{1,40}[,:] *|[Dd]icho de [^,:]{1,60}[,:] *|[Cc]oloquial[,:]? *)+/;
const SENTENCE_END = /(?<=[a-záéíóúüñ)])[.;](?: |$)/;
const TRAILING_NOTE = /\s*\([^)]*\)$/;

export function summary(meanings) {
  const first = meanings.find((m) => m.senses.length)?.senses[0];
  return first ? summaryLine(first.text) : null;
}

function summaryLine(text) {
  const sentence = text.trim().replace(LEAD, "").split(SENTENCE_END)[0];
  const comma = sentence.indexOf(", ");
  const short = sentence.length > LONG && comma >= SHORTEST_CLAUSE ? sentence.slice(0, comma) : sentence;
  const clause = short.replace(TRAILING_NOTE, "").trim().replace(/[.;,]+$/, "");
  return clause.charAt(0).toUpperCase() + clause.slice(1);
}
