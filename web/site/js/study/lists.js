import { FilterSpec } from "./filter.js";
import { load, save } from "../store.js";

export const MINE = "Mis listas";
export const MAX_MEMBERS = 500;
const STALE_PERCENT = 5;
const STALE_MS = 3 * 24 * 60 * 60 * 1000;
const BOOK = "wordLists";

export class WordList {
  constructor(f) {
    Object.assign(this, { learned: new Set(), studied: 0, completions: 0, folder: [], notes: [], used: 0, ...f });
    this.memberSet = new Set(this.members);
  }

  with(changes) {
    return new WordList({ ...this, ...changes });
  }

  get isDefault() {
    return this.id.startsWith("d-");
  }

  get summary() {
    return [...this.spec.summary(), ...this.notes];
  }

  get pending() {
    return this.members.filter((m) => !this.learned.has(m));
  }

  get percent() {
    return this.members.length ? Math.floor((this.learned.size * 100) / this.members.length) : 0;
  }

  get isComplete() {
    return this.members.length > 0 && this.learned.size >= this.members.length;
  }

  inProgress(now) {
    const stale = this.learned.size * 100 < STALE_PERCENT * this.members.length && now - this.used > STALE_MS;
    return !this.isComplete && this.learned.size > 0 && !stale;
  }

  has(key) {
    return this.memberSet.has(key);
  }

  learn(keys) {
    const next = this.with({ learned: new Set([...this.learned, ...keys.filter((k) => this.memberSet.has(k))]), used: Date.now() });
    return next.isComplete && !this.isComplete ? next.with({ completions: this.completions + 1 }) : next;
  }

  restart() {
    return this.with({ learned: new Set() });
  }

  toJSON() {
    return { id: this.id, name: this.name, created: this.created, spec: this.spec.encode(), members: this.members, learned: [...this.learned], studied: this.studied, completions: this.completions, used: this.used };
  }

  static fromJSON(o) {
    const spec = FilterSpec.decode(o.spec);
    return spec ? new WordList({ ...o, spec, learned: new Set(o.learned) }) : null;
  }
}

export function savedLists() {
  return load(BOOK, []).map(WordList.fromJSON).filter(Boolean);
}

export function saveList(list) {
  save(BOOK, [...load(BOOK, []).filter((o) => o.id !== list.id), list.toJSON()]);
}

export function deleteList(id) {
  save(BOOK, load(BOOK, []).filter((o) => o.id !== id));
}

export class ListCatalog {
  constructor(text, saved) {
    const progress = new Map(saved.map((l) => [l.id, l]));
    this.rows = text.split("\n").map((l) => l.split("\t")).filter((r) => r.length === 5).map((r) => row(r, progress));
    this.lists = this.rows.map((r) => r.list).filter(Boolean);
  }

  entries(path) {
    const added = new Set();
    const key = (p) => p.join("/");
    return this.rows.flatMap(({ folder, list }) => {
      if (key(folder) === key(path)) return list ? [{ list }] : [];
      if (folder.length > path.length && key(folder.slice(0, path.length)) === key(path)) {
        const sub = folder.slice(0, path.length + 1);
        if (added.has(key(sub))) return [];
        added.add(key(sub));
        return [{ folder: this.folderOf(sub) }];
      }
      return [];
    });
  }

  find(id) {
    return this.lists.find((l) => l.id === id) ?? null;
  }

  folderOf(path) {
    return folderOf(path, this.lists.filter((l) => l.folder.slice(0, path.length).join("/") === path.join("/")));
  }
}

export function folderOf(path, lists) {
  const members = new Set(lists.flatMap((l) => l.members));
  const learned = new Set(lists.flatMap((l) => [...l.learned]).filter((k) => members.has(k)));
  return { path, name: path.at(-1), families: members.size, learned: learned.size, percent: members.size ? Math.floor((learned.size * 100) / members.size) : 0 };
}

function row(r, progress) {
  const folder = r[0].split("/");
  if (!r[1]) return { folder, list: null };
  const spec = FilterSpec.decode(r[2]);
  if (!spec) return { folder, list: null };
  const id = "d-" + r[0] + "/" + r[1];
  const base = new WordList({ id, name: r[1], created: 0, spec, members: words(r[4], " "), folder, notes: words(r[3], "|") });
  const saved = progress.get(id);
  return { folder, list: saved ? base.with({ learned: new Set([...saved.learned].filter((k) => base.has(k))), studied: saved.studied, completions: saved.completions, used: saved.used }) : base };
}

function words(text, separator) {
  return text.split(separator).filter(Boolean);
}
