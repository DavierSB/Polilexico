"use strict";

importScripts("memfs.js", "wasm_exec.js");

const CACHE = "lexico-engine";
const waiting = [];
const packs = {};
let ready = false;
let packsKnown;
const packsReady = new Promise((r) => (packsKnown = r));

globalThis.lexicoReady = () => {
  ready = true;
  waiting.splice(0).forEach(send);
};

globalThis.lexicoReply = (id, ok, value) => postMessage({ id, ok, value: JSON.parse(value) });

globalThis.lexicoChanged = (match) => postMessage({ changed: match });

globalThis.onmessage = (e) => {
  if (e.data.pack) return deliver(e.data.pack);
  if (ready) send(e.data);
  else waiting.push(e.data);
};

function send({ id, name, args }) {
  globalThis.lexicoCall(id, name, JSON.stringify(args));
}

async function start() {
  const manifest = await (await fetch("manifest.json", { cache: "no-cache" })).json();
  const store = await openStore(manifest.files);
  ["engine", "study"].forEach((name) => (packs[name] = pack(manifest.files.filter((f) => f.pack === name))));
  packsKnown();
  ["engine", "study"].forEach((name) => postMessage({ pack: name, total: packs[name].files.reduce((t, f) => t + f.bytes, 0) }));
  await download("engine", store);
  await startEngine();
  download("study", store).catch((e) => postMessage({ pack: "study", failed: String(e) }));
}

function pack(files) {
  let done;
  return { files, parts: new Map(), done: new Promise((r) => (done = r)), finish: () => done() };
}

async function startEngine() {
  const files = await inflateAll(packs.engine);
  packs.engine.files.forEach((f, i) => f.path !== "engine.wasm" && memfs.put("/" + f.path, files[i]));
  const go = new Go();
  const wasm = await WebAssembly.instantiate(files[packs.engine.files.findIndex((f) => f.path === "engine.wasm")], go.importObject);
  packs.engine.parts.clear();
  go.run(wasm.instance);
}

async function deliver(name) {
  await packsReady;
  await packs[name].done;
  const files = await inflateAll(packs[name]);
  const out = Object.fromEntries(packs[name].files.map((f, i) => [f.path.split("/").pop(), files[i].buffer]));
  postMessage({ pack: name, files: out }, Object.values(out));
}

async function openStore(files) {
  if (!globalThis.caches) return null;
  const store = await caches.open(CACHE);
  const wanted = new Set(files.flatMap((f) => partUrls(f)));
  const stale = (await store.keys()).filter((r) => !wanted.has(r.url));
  await Promise.all(stale.map((r) => store.delete(r)));
  return store;
}

function partUrls(f) {
  return Array.from({ length: f.parts }, (_, i) => new URL(`parts/${f.name}.${String(i).padStart(3, "0")}.bin?v=${f.hash}`, location.href).href);
}

async function download(name, store) {
  const p = packs[name];
  for (const f of p.files) {
    const urls = partUrls(f);
    for (const url of urls) p.parts.set(url, await part(name, url, store));
  }
  p.finish();
  postMessage({ pack: name, done: true });
}

async function part(name, url, store) {
  const cached = store && (await store.match(url));
  if (cached) return report(name, await cached.arrayBuffer());
  const bytes = await fetchRetrying(name, url);
  await store?.put(url, new Response(bytes));
  return report(name, bytes);
}

async function fetchRetrying(name, url) {
  for (let wait = 2000; ; wait = Math.min(wait * 2, 60000)) {
    const response = await fetch(url).catch(() => null);
    if (response?.ok) return response.arrayBuffer();
    postMessage({ pack: name, stalled: true });
    await new Promise((r) => setTimeout(r, wait));
  }
}

function report(name, bytes) {
  postMessage({ pack: name, loaded: bytes.byteLength });
  return bytes;
}

function inflateAll(p) {
  return Promise.all(p.files.map((f) => inflate(new Blob(partUrls(f).map((u) => p.parts.get(u))))));
}

async function inflate(blob) {
  const stream = blob.stream().pipeThrough(new DecompressionStream("gzip"));
  return new Uint8Array(await new Response(stream).arrayBuffer());
}

start().catch((e) => postMessage({ pack: "engine", failed: String(e) }));
