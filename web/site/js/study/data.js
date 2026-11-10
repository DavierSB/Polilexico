import { loadPack } from "../engine.js";
import { Dictionary } from "./dictionary.js";
import { Lexicon } from "./lexicon.js";

let loaded = null;

export function studyData() {
  loaded ??= loadPack("study").then(decode);
  return loaded;
}

function decode(buffers) {
  const decoder = new TextDecoder();
  const files = Object.fromEntries(Object.entries(buffers).map(([name, b]) => [name, decoder.decode(b)]));
  const dictionary = new Dictionary(files);
  return { dictionary, lexicon: new Lexicon(files, dictionary), catalogText: files["default_lists.tsv"] };
}
