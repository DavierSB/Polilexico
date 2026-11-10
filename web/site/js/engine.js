const pending = new Map();
const watchers = new Map();
const listeners = { engine: new Set(), study: new Set() };
const packFiles = new Map();
let worker = null;
let started = null;
let lastId = 0;
const progress = { engine: blank(), study: blank() };
let markReady = null;

export const readyLater = new Promise((resolve) => (markReady = resolve));

function blank() {
  return { total: 0, loaded: 0, ready: false, failure: null, stalled: false };
}

export function startEngine() {
  if (worker) return started;
  worker = new Worker("engine/worker.js");
  worker.onmessage = ({ data }) => receive(data);
  started = send("init", ["/data", "/saves"]).then(() => (update("engine", { ready: true }), markReady()));
  started.catch((e) => update("engine", { failure: e.message }));
  return started;
}

export function whenReady() {
  return startEngine();
}

export function isReady() {
  return progress.engine.ready;
}

export function call(name, ...args) {
  startEngine();
  return send(name, args);
}

export function watch(match, fn) {
  watchers.set(match, fn);
}

export function unwatch(match) {
  watchers.delete(match);
}

export function onLoading(fn, pack = "engine") {
  listeners[pack].add(fn);
  fn(snapshot(pack));
  return () => listeners[pack].delete(fn);
}

export function loadPack(name) {
  startEngine();
  if (!packFiles.has(name)) packFiles.set(name, new Promise((resolve) => {
    packFiles.set(name + ":resolve", resolve);
    worker.postMessage({ pack: name });
  }));
  return packFiles.get(name);
}

function send(name, args) {
  const id = ++lastId;
  worker.postMessage({ id, name, args });
  return new Promise((resolve, reject) => pending.set(id, { resolve, reject }));
}

function receive(data) {
  if (data.id) return settle(data);
  if (data.changed) return watchers.get(data.changed)?.();
  if (data.files) return packFiles.get(data.pack + ":resolve")?.(data.files);
  const p = progress[data.pack];
  if (!p) return;
  if (data.total) update(data.pack, { total: data.total });
  if (data.loaded) update(data.pack, { loaded: p.loaded + data.loaded, stalled: false });
  if (data.stalled) update(data.pack, { stalled: true });
  if (data.failed) update(data.pack, { failure: data.failed });
  if (data.done && data.pack === "study") update("study", { ready: true });
}

function settle({ id, ok, value }) {
  const p = pending.get(id);
  pending.delete(id);
  if (ok) p.resolve(value);
  else p.reject(new Error(value));
}

function update(pack, changes) {
  progress[pack] = { ...progress[pack], ...changes };
  listeners[pack].forEach((fn) => fn(snapshot(pack)));
}

function snapshot(pack) {
  const p = progress[pack];
  const fraction = p.total ? Math.min(1, p.loaded / p.total) : 0;
  return { ...p, fraction, percent: Math.floor(fraction * 100) };
}
