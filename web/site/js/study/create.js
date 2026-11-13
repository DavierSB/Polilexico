import { h, replace } from "../dom.js";
import { go } from "../router.js";
import { screenHead } from "../parts.js";
import { openDialog, actions } from "../dialogs.js";
import { stepper } from "../classic/options.js";
import { withStudy, label, dotted, big } from "./shell.js";
import { spec as newSpec, FILTER_TYPES, MIN_TILES, MAX_TILES, FREE } from "./filter.js";
import { WordList, MINE, MAX_MEMBERS, saveList } from "./lists.js";
import { label as tileLabel, parse } from "./keys.js";

const SIMPLE_NOTE = "Se consideran simples las vocales (A, E, I, O, U) y las consonantes C, D, L, N, R, S y T. Las letras que escribas arriba pueden ser cualquiera.";
const MAX_NAME = 40;

export function createListScreen(app) {
  return withStudy(app, ({ lexicon }) => {
    const ui = { lexicon, spec: newSpec(), results: null };
    ui.filters = h("div.stack");
    ui.count = h("div.stack");
    ui.save = h("button.btn.block", { onclick: () => nameDialog(ui) }, "Guardar lista");
    app.append(h("div.screen.study-screen", {}, screenHead("Crear lista", "listas"), ui.filters, h("hr.thick"), ui.count, ui.save));
    update(ui, ui.spec);
  });
}

function update(ui, spec) {
  ui.spec = spec;
  ui.results = ui.lexicon.filter(spec);
  replace(ui.filters, filterCard(ui), h("button.btn.outlined.block", { onclick: () => addDialog(ui) }, spec.isEmpty ? "Añadir filtro" : "Añadir más filtros"));
  const n = ui.results.length;
  replace(ui.count,
    h("div.row.big-line", {}, big(n.toLocaleString("es")), label("familias", "muted")),
    h("span", {}, spec.isEmpty ? "en total" : "cumplen estos filtros"),
    n > MAX_MEMBERS ? h("span.error", {}, `Se guardarán las primeras ${MAX_MEMBERS}, las más importantes.`) : null,
  );
  ui.save.disabled = !n;
}

function filterCard(ui) {
  if (ui.spec.isEmpty) return null;
  return h("div.stack", {}, label("Filtros"), h("div.stack.filter-rows", {}, FILTER_TYPES.filter((t) => t.line(ui.spec)).map((t) => h("div.row.filter-row", {},
    h("button.grow.filter-edit", { onclick: () => editor(ui, t) }, dotted(t.line(ui.spec))),
    h("button.text-btn", { "aria-label": "Quitar filtro", onclick: () => update(ui, ui.spec.with(t.clear)) }, "✕"),
  ))));
}

function addDialog(ui) {
  const available = FILTER_TYPES.filter((t) => !t.line(ui.spec));
  openDialog((close) => [
    h("h2", {}, "Añadir filtro"),
    available.length ? h("div.stack.filter-choices", {}, available.map((t) => h("button.filter-choice", { onclick: () => (close(), editor(ui, t)) }, h("b.grow", {}, t.title), h("span.muted.small", {}, t.info)))) : h("p", {}, "Ya están puestos todos los filtros. Toca uno para cambiarlo."),
    actions(h("button.text-btn", { onclick: close }, "Cancelar")),
  ]);
}

function editor(ui, type) {
  let draft = type.id === "KIND" && ui.spec.kind === "ALL" ? ui.spec.with({ kind: "VERBAL" }) : ui.spec;
  const body = h("div.stack");
  const redraw = () => replace(body, fields(type, draft, (next) => ((draft = next), redraw())));
  redraw();
  openDialog((close) => [h("h2", {}, type.title), h("p.muted.small", {}, type.info), body,
    actions(h("button.text-btn", { onclick: close }, "Cancelar"), h("button.text-btn", { onclick: () => (close(), update(ui, draft)) }, "Aplicar"))]);
}

function fields(type, s, set) {
  const editors = {
    KIND: () => [["VERBAL", "Familias verbales"], ["NON_VERBAL", "Familias no verbales"]].map(([k, text]) => h("label.row.theme-choice", {}, h("input", { type: "radio", name: "kind", checked: s.kind === k, onchange: () => set(s.with({ kind: k })) }), text)),
    LENGTH: () => h("div.row.two-steppers", {},
      labeled("Mín", stepper(s.minLength, MIN_TILES, s.maxLength, (n) => set(s.with({ minLength: n })))),
      labeled("Máx", stepper(s.maxLength, s.minLength, MAX_TILES, (n) => set(s.with({ maxLength: n }))))),
    COUNTS: () => [h("div.row.two-steppers", {}, countStepper("Vocales", s.vowels, (v) => set(s.with({ vowels: v }))), countStepper("Consonantes", s.consonants, (v) => set(s.with({ consonants: v })))), h("span.muted.small", {}, "«—» = cualquier número.")],
    CONTAINS: () => [textField(s.contains, "p. ej. aerch", (t) => set(s.with({ contains: t }))), miniTiles(parse(s.contains)), h("span.muted.small", {}, "«aa» exige dos A."),
      h("label.row.theme-choice", {}, h("input", { type: "checkbox", checked: s.simpleOnly, onchange: (e) => set(s.with({ simpleOnly: e.target.checked })) }), h("b", {}, "Las demás letras serán simples")), h("span.muted.small", {}, SIMPLE_NOTE)],
    START: () => [textField(s.start, "p. ej. des", (t) => set(s.with({ start: t }))), miniTiles(parse(s.start))],
    END: () => [textField(s.end, "p. ej. ción", (t) => set(s.with({ end: t }))), miniTiles(parse(s.end))],
    FIXED: () => positions(s, false, set),
    FIXED_END: () => positions(s, true, set),
  };
  return editors[type.id]();
}

function labeled(text, el) {
  return h("div.stack.center", {}, h("span.muted.small", {}, text), el);
}

function countStepper(text, value, set) {
  return labeled(text, h("div.row.stepper", {},
    h("button.btn.small.outlined", { disabled: value === null, onclick: () => set(value === 0 ? null : value - 1) }, "−"),
    h("span.stepper-value", {}, value === null ? "—" : value),
    h("button.btn.small.outlined", { disabled: (value ?? -1) >= MAX_TILES, onclick: () => set((value ?? -1) + 1) }, "+"),
  ));
}

function textField(value, placeholder, set) {
  const input = h("input", { value, placeholder, autocapitalize: "characters", onchange: (e) => set(e.target.value) });
  input.addEventListener("input", (e) => {
    const tiles = input.parentElement?.nextElementSibling;
    if (tiles) replace(tiles, miniTiles(parse(e.target.value)).childNodes);
  });
  return h("div.field", {}, input);
}

function miniTiles(key) {
  return h("div.row.mini-tiles-row", {}, [...key].map((c) => h("span.mini-letter", {}, tileLabel(c))));
}

function positions(s, fromEnd, set) {
  const text = fromEnd ? s.fixedEnd : s.fixed;
  const order = [...Array(s.maxLength).keys()];
  if (fromEnd) order.reverse();
  return [
    h("div.row.slot-row", {}, order.map((i) => {
      const c = text[i] && text[i] !== FREE ? text[i] : "";
      return h("input.slot-input", { value: c ? tileLabel(c) : "", placeholder: String(i + 1), "aria-label": `Posición ${i + 1}`, oninput: (e) => {
        const key = parse(e.target.value).slice(-1) || FREE;
        const next = text.padEnd(MAX_TILES, FREE).split("");
        next[i] = key;
        set(s.with({ [fromEnd ? "fixedEnd" : "fixed"]: next.join("").replace(/\.+$/, "") }));
      } });
    })),
    h("span.muted.small", {}, fromEnd ? "La casilla 1 es la última letra." : "La casilla 1 es la primera letra."),
  ];
}

function nameDialog(ui) {
  let name = "";
  openDialog((close) => [
    h("h2", {}, "Nombre de la lista"),
    h("div.field", {}, h("input", { autofocus: true, maxlength: MAX_NAME, placeholder: "p. ej. Verbos con Z", oninput: (e) => (name = e.target.value) })),
    actions(h("button.text-btn", { onclick: close }, "Cancelar"), h("button.text-btn", { onclick: () => {
      if (!name.trim()) return;
      close();
      saveList(new WordList({ id: crypto.randomUUID(), name: name.trim(), created: Date.now(), spec: ui.spec, members: ui.results.slice(0, MAX_MEMBERS).map((f) => f.key) }));
      go("listas-catalogo/" + encodeURIComponent(MINE), true);
    } }, "Guardar")),
  ]);
}
