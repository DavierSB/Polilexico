const pending = new Map();
const watchers = new Map();
const loading = new Set();
let worker = null;
let started = null;
let lastId = 0;
let progress = { total: 0, loaded: 0, ready: false, failure: null, stalled: false };
let markReady = null;

export const readyLater = new Promise((resolve) => (markReady = resolve));

export function startEngine() {
  if (worker) return started;
  worker = new Worker("engine/worker.js");
  worker.onmessage = ({ data }) => receive(data);
  started = send("init", ["/data", "/saves"]).then(() => (update({ ready: true }), markReady()));
  started.catch((e) => update({ failure: e.message }));
  return started;
}

export function whenReady() {
  return startEngine();
}

export function isReady() {
  return progress.ready;
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

export function onLoading(fn) {
  loading.add(fn);
  fn(snapshot());
  return () => loading.delete(fn);
}

function send(name, args) {
  const id = ++lastId;
  worker.postMessage({ id, name, args });
  return new Promise((resolve, reject) => pending.set(id, { resolve, reject }));
}

function receive(data) {
  if (data.id) return settle(data);
  if (data.changed) return watchers.get(data.changed)?.();
  if (data.total) update({ total: data.total });
  if (data.loaded) update({ loaded: progress.loaded + data.loaded, stalled: false });
  if (data.stalled) update({ stalled: true });
  if (data.failed) update({ failure: data.failed });
}

function settle({ id, ok, value }) {
  const p = pending.get(id);
  pending.delete(id);
  if (ok) p.resolve(value);
  else p.reject(new Error(value));
}

function update(changes) {
  progress = { ...progress, ...changes };
  loading.forEach((fn) => fn(snapshot()));
}

function snapshot() {
  const fraction = progress.total ? Math.min(1, progress.loaded / progress.total) : 0;
  return { ...progress, fraction, percent: Math.floor(fraction * 100) };
}
