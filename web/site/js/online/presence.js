import { presence } from "./api.js";
import { online } from "./client.js";

const POLL_MS = 30000;
const listeners = new Set();
let polled = null;
let timer = null;

export function onPresence(fn) {
  listeners.add(fn);
  const stop = online.subscribe(() => emit());
  if (!timer) poll();
  emit();
  return () => {
    stop();
    listeners.delete(fn);
    if (!listeners.size) (clearTimeout(timer), (timer = null));
  };
}

function count() {
  return online.connected ? online.lobby.people.length + 1 : polled;
}

function emit() {
  const n = count();
  listeners.forEach((fn) => fn(n));
}

async function poll() {
  polled = await presence();
  emit();
  timer = setTimeout(poll, POLL_MS);
}

export function onlineLabel(n) {
  return n ? `En línea (${n})` : "En línea";
}
