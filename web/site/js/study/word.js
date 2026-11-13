import { h, replace } from "../dom.js";
import { go } from "../router.js";
import { screenHead } from "../parts.js";
import { openDialog, actions } from "../dialogs.js";
import { upper, show, entry as entryOf, bare, parse } from "./keys.js";
import { summary, sourceLabel } from "./dictionary.js";
import { withStudy, chips, label } from "./shell.js";

const OUTSIDE = "No está en el léxico de estudio (palabras válidas de hasta 9 letras).";
const NO_MEANING = "No está en el diccionario sin conexión.";
const RAE_URL = "https://dle.rae.es/";

export function dictionaryScreen(app, [given]) {
  return withStudy(app, ({ lexicon }) => {
    const random = () => lexicon.words[Math.floor(Math.random() * lexicon.words.length)].key;
    const current = given ? decodeURIComponent(given) : random();
    const body = h("div.word-screen-body");
    const head = h("div");
    app.append(h("div.screen.study-screen.word-screen", {}, head, body,
      h("div.row.word-actions", {},
        h("button.btn.outlined.grow", { onclick: () => searchDialog((e) => open(e)) }, "Buscar palabra"),
        h("button.btn.grow", { onclick: () => open(random()) }, "Palabra al azar"),
      ),
    ));
    const open = (e) => go("diccionario/" + encodeURIComponent(e), true);
    const unit = lexicon.resolve(current);
    replace(head, screenHead(upper(unit?.head ?? bare(current)), "estudiar"));
    replace(body, unit ? wordCard(unit, lexicon, (e) => open(e)) : looseCard(bare(current), lexicon));
  });
}

export function wordCard(unit, lexicon, onOpen) {
  const traits = unit.traits(unit.head).map((t) => t.label);
  const openSimilar = (w) => {
    const f = lexicon.familyOf(w);
    if (f) onOpen(f.key);
  };
  return h("div.word-card-big", {},
    h("div.word-main", {}, headWord(unit.head), chips(kindText(unit), ...traits), meaningLine(unit.head, lexicon), meaningPanel(unit.head, lexicon)),
    h("div.word-groups", {},
      h("div.word-groups.word-family", {}, details(unit, lexicon, onOpen)),
      h("div.word-groups.word-similar", {}, group("Similares", lexicon.similar(unit), lexicon, openSimilar))),
  );
}

function meaningPanel(word, lexicon) {
  const meanings = lexicon.meanings(word);
  if (!meanings.length) return null;
  const text = h("div.stack.meanings.meaning-text", {}, meanings.map((m) => meaningBlock(m, word)));
  const more = h("button.text-btn.hidden", { onclick: () => meaningsDialog(word, meanings) }, "Ver más");
  watchClip(text, more);
  return h("div.meaning-panel", {}, text, h("div.meaning-foot", {}, sourceLine(meanings), h("div.row.meaning-links", {}, more, raeLink(word))));
}

function watchClip(text, more) {
  const check = () => {
    const clipped = text.scrollHeight > text.clientHeight + 1;
    text.classList.toggle("clipped", clipped);
    more.classList.toggle("hidden", !clipped);
  };
  new ResizeObserver(check).observe(text);
}

function sourceLine(meanings) {
  return h("p.muted.small", {}, "Fuente: " + [...new Set(meanings.flatMap((m) => m.senses.map((s) => sourceLabel(s.source))))].join(", "));
}

function raeLink(word) {
  return h("a.text-btn", { href: RAE_URL + encodeURIComponent(show(word)), target: "_blank", rel: "noopener" }, "Consultar en la RAE");
}

export function looseCard(word, lexicon) {
  return h("div.word-card-big", {}, headWord(word), h("p.muted.center", {}, OUTSIDE), meaningLine(word, lexicon));
}

const HEAD_MAX = 56;
const HEAD_MIN = 24;

function headWord(key) {
  const el = h("h2.head-word", {}, upper(key));
  const observer = new ResizeObserver(() => fitHead(el));
  observer.observe(el);
  return el;
}

function fitHead(el) {
  if (!el.isConnected) return;
  let size = HEAD_MAX;
  el.style.fontSize = size + "px";
  while (size > HEAD_MIN && el.scrollWidth > el.clientWidth) el.style.fontSize = --size + "px";
}

function kindText(unit) {
  if (unit.isWord && unit.verbal && unit.nonVerbal) return "Verbal y no verbal";
  if (unit.isWord) return unit.verbal ? "Forma verbal" : "No verbo";
  return unit.verbal ? "Verbo" : "No verbo";
}

function details(unit, lexicon, onOpen) {
  const openWord = (w) => onOpen("w:" + w);
  if (unit.isWord) return lexicon.owners(unit).map((o) => group(`Familia de ${upper(o.head)}`, o.words, lexicon, openWord));
  return group("Familia", unit.verbal ? unit.words.slice(1) : unit.words, lexicon, openWord);
}

function group(title, words, lexicon, onWord) {
  if (!words.length) return null;
  return h("div.word-group", {}, label(title), h("div.stack.word-lines", {}, words.map((w) => {
    const tag = lexicon.tag(w);
    return h("button.word-line", { onclick: () => onWord(w) }, h("span.word-line-text", {}, upper(w)), tag ? h("span.muted", {}, `(${tag})`) : null);
  })));
}

export function meaningLine(word, lexicon) {
  const meanings = lexicon.meanings(word);
  if (!meanings.length) return h("a.btn.outlined.block.rae-btn", { href: RAE_URL + encodeURIComponent(show(word)), target: "_blank", rel: "noopener" }, "Consultar en la RAE");
  return h("button.meaning-line", { onclick: () => meaningsDialog(word, meanings) }, h("span.grow.ellipsis", {}, summary(meanings) ?? NO_MEANING), h("b.see-more", {}, "Ver más"));
}

function meaningsDialog(word, meanings) {
  openDialog((close) => [
    h("h2", {}, upper(word)),
    h("div.stack.meanings", {}, meanings.map((m) => meaningBlock(m, word)), sourceLine(meanings)),
    actions(raeLink(word), h("button.text-btn", { onclick: close }, "Cerrar")),
  ], { wide: true });
}

function meaningBlock(m, word) {
  const heading = m.relation || (m.lemma !== show(word) ? m.lemma : null);
  return h("div.stack.meaning-block", {}, heading ? h("b", {}, heading) : null, m.senses.map((s, i) => h("p.sense", {}, h("b.sense-n", {}, `${i + 1}. `), s.text)));
}

function searchDialog(onSearch) {
  let text = "";
  const submit = (close) => {
    if (!parse(text)) return;
    close();
    onSearch(entryOf(text));
  };
  openDialog((close) => [
    h("h2", {}, "Buscar palabra"),
    h("form", { onsubmit: (e) => (e.preventDefault(), submit(close)) }, h("div.field", {}, h("input", { autofocus: true, placeholder: "Escribe una palabra", autocapitalize: "characters", oninput: (e) => (text = e.target.value) }))),
    actions(h("button.text-btn", { onclick: close }, "Cancelar"), h("button.text-btn", { onclick: () => submit(close) }, "Buscar")),
  ]);
}
