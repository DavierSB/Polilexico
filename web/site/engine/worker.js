"use strict";

importScripts("memfs.js", "wasm_exec.js");

const CACHE = "lexico-engine";
const waiting = [];
let ready = false;

globalThis.lexicoReady = () => {
  ready = true;
  waiting.splice(0).forEach(send);
};

globalThis.lexicoReply = (id, ok, value) => postMessage({ id, ok, value: JSON.parse(value) });

globalThis.lexicoChanged = (match) => postMessage({ changed: match });

globalThis.onmessage = (e) => (ready ? send(e.data) : waiting.push(e.data));

function send({ id, name, args }) {
  globalThis.lexicoCall(id, name, JSON.stringify(args));
}

async function start() {
  const manifest = await (await fetch("manifest.json", { cache: "no-cache" })).json();
  postMessage({ total: manifest.bytes });
  const store = await openStore(manifest.version);
  const files = [];
  for (const f of manifest.files) files.push(await assemble(f, manifest.version, store));
  manifest.files.forEach((f, i) => f.path !== "engine.wasm" && memfs.put("/" + f.path, files[i]));
  const go = new Go();
  const wasm = await WebAssembly.instantiate(files[manifest.files.findIndex((f) => f.path === "engine.wasm")], go.importObject);
  postMessage({ downloaded: true });
  go.run(wasm.instance);
}

async function openStore(version) {
  if (!globalThis.caches) return null;
  const store = await caches.open(CACHE);
  const stale = (await store.keys()).filter((r) => !r.url.includes("v=" + version));
  await Promise.all(stale.map((r) => store.delete(r)));
  return store;
}

async function assemble(file, version, store) {
  const parts = [];
  for (let i = 0; i < file.parts; i++) parts.push(await part(file, i, version, store));
  return inflate(new Blob(parts));
}

async function part(file, i, version, store) {
  const url = new URL(`parts/${file.name}.${String(i).padStart(3, "0")}.bin?v=${version}`, location.href).href;
  const cached = store && (await store.match(url));
  if (cached) return report(await cached.arrayBuffer());
  const bytes = await fetchRetrying(url);
  await store?.put(url, new Response(bytes));
  return report(bytes);
}

async function fetchRetrying(url) {
  for (let wait = 2000; ; wait = Math.min(wait * 2, 60000)) {
    const response = await fetch(url).catch(() => null);
    if (response?.ok) return response.arrayBuffer();
    postMessage({ stalled: true });
    await new Promise((r) => setTimeout(r, wait));
  }
}

function report(bytes) {
  postMessage({ loaded: bytes.byteLength });
  return bytes;
}

async function inflate(blob) {
  const stream = blob.stream().pipeThrough(new DecompressionStream("gzip"));
  return new Uint8Array(await new Response(stream).arrayBuffer());
}

start().catch((e) => postMessage({ failed: String(e) }));
