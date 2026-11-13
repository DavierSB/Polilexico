import { h, replace } from "../dom.js";
import { go } from "../router.js";
import { screenHead, engineGate } from "../parts.js";
import { confirm } from "../dialogs.js";
import { load, save } from "../store.js";
import { withStudy, label, chips, dotted, big, progressBar } from "./shell.js";
import { ListCatalog, MINE, savedLists, saveList, deleteList, folderOf } from "./lists.js";
import { wordCard } from "./word.js";
import { homeFrame } from "../screens/home.js";

const LISTS_INFO = "Aquí aparecen listas ya confeccionadas, con las palabras ordenadas por su importancia para el Scrabble. Se dejaron fuera las palabras de uso más común en español, para que no estorben a la hora de estudiar.\n\nAdemás, en «Crear lista» se pueden armar listas propias con los filtros que se quieran.";
const PACES = [2, 3, 5, 8];

export function catalogOf(data) {
  return new ListCatalog(data.catalogText, savedLists());
}

export function allLists(data) {
  return [...catalogOf(data).lists, ...savedLists().filter((l) => !l.isDefault)];
}

export function findList(data, id) {
  return id.startsWith("d-") ? catalogOf(data).find(id) : savedLists().find((l) => l.id === id) ?? null;
}

export function listsMenuScreen(app) {
  const gates = [];
  const entry = (text, route, main = true, enabled = true) => {
    const b = h("button", { class: "btn block" + (main ? "" : " outlined"), disabled: !enabled, onclick: () => go(route) }, text);
    if (enabled) gates.push(engineGate(b, "study"));
    return b;
  };
  const unfinished = savedLists().filter((l) => l.inProgress(Date.now())).length;
  const done = homeFrame(app, screenHead("Listas", "estudiar", infoButton()), [
    entry("Estudiar listas", "listas-catalogo"),
    entry("Crear lista", "crear-lista"),
    entry(`Estudios a medias (${unfinished})`, "estudios-medias", false, unfinished > 0),
    entry("Estudios culminados", "estudios-culminados", false),
  ]);
  return () => (done(), gates.forEach((stop) => stop()));
}

function infoButton() {
  return h("button.info-btn", { "aria-label": "Más información", onclick: () => import("../dialogs.js").then(({ openDialog, actions }) => openDialog((close) => [h("h2", {}, "Listas"), LISTS_INFO.split("\n\n").map((p) => h("p", {}, p)), actions(h("button.text-btn", { onclick: close }, "Entendido"))])) }, "i");
}

export function catalogScreen(app, path) {
  return withStudy(app, (data) => {
    const page = pageFor(data, path);
    app.append(h("div.screen.study-screen", {}, screenHead(page.title, path.length ? "listas-catalogo/" + path.slice(0, -1).map(encodeURIComponent).join("/") : "listas"), pageBody(page)));
  });
}

function pageFor(data, path) {
  if (path.join("/") === MINE) {
    const mine = savedLists().filter((l) => !l.isDefault).sort((a, b) => b.created - a.created);
    return { title: MINE, entries: mine.map((list) => ({ list })), mine: true };
  }
  const catalog = catalogOf(data);
  const entries = catalog.entries(path);
  if (!path.length) entries.push({ folder: folderOf([MINE], savedLists().filter((l) => !l.isDefault)) });
  return { title: path.at(-1) ?? "Listas", entries, emptyNote: "Pronto habrá listas en esta carpeta." };
}

function pageBody(page) {
  if (page.entries.length) return h("div.stack", {}, page.entries.map(entryCard));
  if (page.mine) return h("div.empty-state", {}, h("b", {}, "No hay listas"), h("span", {}, "Crea una para que la puedas estudiar."), h("button.btn", { onclick: () => go("crear-lista") }, "Crear lista"));
  return h("div.empty-state", {}, h("b", {}, "Todavía no hay listas"), h("span", {}, page.emptyNote));
}

function entryCard(e) {
  if (e.folder) return card(e.folder.name, e.folder.families, e.folder.percent, () => go("listas-catalogo/" + e.folder.path.map(encodeURIComponent).join("/")));
  return card(e.list.name, e.list.members.length, e.list.percent, () => go("lista/" + encodeURIComponent(e.list.id)));
}

function card(name, families, percent, onclick) {
  return h("button.list-card", { onclick },
    h("div.row", {}, h("div.grow.stack", {}, h("b.list-name", {}, name), chips(families === 1 ? "1 familia" : `${families} familias`)), h("b.list-percent", {}, `${percent}%`)),
    progressBar(percent),
  );
}

export function listScreen(app, [id]) {
  return withStudy(app, (data) => {
    const list = findList(data, id);
    if (!list) return go("listas", true);
    app.append(h("div.screen.study-screen", {},
      screenHead(list.name, "listas"),
      h("div.stack", {}, h("div.row.big-line", {}, big(`${list.percent}%`), label("completado", "muted")), progressBar(list.percent), chips(`${list.learned.size} de ${list.members.length} aprendidas`)),
      filterSummary(list.summary),
      h("div.row.list-actions", {},
        h("button.btn.grow", { onclick: () => go("lista-estudio/" + encodeURIComponent(list.id)) }, "Estudiar"),
        h("button.btn.grow", { onclick: () => go("lista-evaluar/" + encodeURIComponent(list.id)) }, "Evaluar"),
      ),
      list.isDefault ? null : h("button.text-btn.danger", { onclick: () => remove(list) }, "Borrar lista"),
    ));
  });
}

export function filterSummary(lines) {
  return h("div.stack.filter-summary", {}, label("Filtros"), (lines.length ? lines : ["Ninguno: todas las familias"]).map(dotted));
}

async function remove(list) {
  if (!(await confirm(`¿Borrar la lista «${list.name}»?`, "Borrar", "Cancelar"))) return;
  deleteList(list.id);
  go("listas-catalogo/" + encodeURIComponent(MINE), true);
}

export function studyScreen(app, [id]) {
  return withStudy(app, (data) => {
    const list = findList(data, id);
    if (!list) return go("listas", true);
    const state = { at: Math.min(Math.max(list.studied, 0), Math.max(0, list.members.length - 1)), playing: false, seconds: load("studyPace", 3), timer: null };
    const line = h("div.row");
    const body = h("div.word-screen-body");
    const buttons = h("div.row.word-actions");
    app.append(h("div.screen.study-screen.word-screen", {}, screenHead(list.name, "lista/" + encodeURIComponent(id)), line, body, buttons));
    const move = (to) => {
      state.at = ((to % list.members.length) + list.members.length) % list.members.length;
      saveList(list.with({ studied: state.at, used: Date.now() }));
      draw();
    };
    const draw = () => {
      clearTimeout(state.timer);
      if (state.playing) state.timer = setTimeout(() => (state.at < list.members.length - 1 ? move(state.at + 1) : ((state.playing = false), draw())), state.seconds * 1000);
      replace(line, label(`${state.at + 1} de ${list.members.length}`, "grow"), h("button.text-btn", { onclick: () => pace() }, `${state.seconds} s`));
      const unit = data.lexicon.find(list.members[state.at]);
      replace(body, unit ? wordCard(unit, data.lexicon, (e) => go("diccionario/" + encodeURIComponent(e))) : h("p", {}, "Esta entrada ya no está en el léxico."));
      replace(buttons,
        h("button.btn.outlined.grow.study-btn", { onclick: () => move(state.at - 1) }, "Anterior"),
        h("button.btn.tonal.grow.study-btn", { onclick: () => ((state.playing = !state.playing), draw()) }, state.playing ? "Pausar" : "▶ Reproducir"),
        h("button.btn.grow.study-btn", { onclick: () => move(state.at + 1) }, "Siguiente"),
      );
    };
    const pace = () => {
      state.seconds = PACES[(PACES.indexOf(state.seconds) + 1) % PACES.length] ?? 3;
      save("studyPace", state.seconds);
      draw();
    };
    draw();
    return () => clearTimeout(state.timer);
  });
}

export function unfinishedScreen(app) {
  return withStudy(app, (data) => {
    const lists = allLists(data).filter((l) => l.inProgress(Date.now())).sort((a, b) => b.percent - a.percent);
    app.append(h("div.screen.study-screen", {}, screenHead("Estudios a medias", "listas"),
      lists.length ? h("div.stack", {}, lists.map((list) => entryCard({ list }))) : h("div.empty-state", {}, h("b", {}, "Todavía no hay listas"), h("span", {}, "Las listas que empieces a estudiar saldrán aquí.")),
    ));
  });
}

export function completedScreen(app) {
  return withStudy(app, (data) => {
    const lists = allLists(data);
    const finished = lists.filter((l) => l.completions > 0).sort((a, b) => b.completions - a.completions);
    const studies = finished.reduce((t, l) => t + l.completions, 0);
    const families = new Set([...lists.flatMap((l) => [...l.learned]), ...finished.flatMap((l) => l.members)]).size;
    app.append(h("div.screen.study-screen", {}, screenHead("Estudios culminados", "listas"),
      h("div.row.totals", {}, total(families, families === 1 ? "familia aprendida" : "familias aprendidas"), total(studies, studies === 1 ? "estudio culminado" : "estudios culminados")),
      finished.length ? h("div.stack", {}, finished.map((l) => h("button.list-card", { onclick: () => go("lista/" + encodeURIComponent(l.id)) },
        h("div.row", {}, h("div.grow.stack", {}, h("b.list-name", {}, l.name), chips(l.completions === 1 ? "Culminada 1 vez" : `Culminada ${l.completions} veces`)), h("b.list-percent", {}, `×${l.completions}`)),
      ))) : h("div.empty-state", {}, h("b", {}, "Ninguno todavía"), h("span", {}, "Una lista queda culminada al aprender todas sus familias en Evaluar.")),
    ));
  });
}

function total(n, text) {
  return h("div.total", {}, big(String(n)), h("span.muted", {}, text));
}
