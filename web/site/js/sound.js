import { load, save } from "./store.js";

const SOUNDS = {
  warning: ["material_state_change_confirm_down", 1],
  last_seconds: ["material_alert_simple", 1],
  time_up: ["material_alert_high_intensity", 0.8],
  correct: ["material_state_change_confirm_up", 1],
  miss: ["material_state_change_confirm_up", 0.8],
  wrong: ["material_alert_error_01", 1],
  opponent: ["material_ui_tap_variant_01", 1],
  victory: ["material_hero_decorative_celebration_01", 1],
  game_over: ["material_hero_simple_celebration_01", 1],
};

let context = null;
const buffers = new Map();

export function soundsOn() {
  return load("sounds", true);
}

export function setSounds(on) {
  save("sounds", on);
}

export function play(cue) {
  const sound = SOUNDS[cue];
  if (!sound || !soundsOn()) return;
  audio()?.resume().catch(() => {});
  buffer(sound[0]).then((b) => b && start(b, sound[1])).catch(() => {});
}

export function cueWatcher() {
  let seen = null;
  return (count, cue) => {
    if (seen !== null && count > seen && cue) play(cue);
    seen = count;
  };
}

function audio() {
  if (!context && window.AudioContext) context = new AudioContext();
  return context;
}

function buffer(name) {
  if (!buffers.has(name)) buffers.set(name, fetch(`sounds/${name}.ogg`).then((r) => r.arrayBuffer()).then((b) => audio()?.decodeAudioData(b)).catch(() => null));
  return buffers.get(name);
}

function start(b, rate) {
  const source = context.createBufferSource();
  source.buffer = b;
  source.playbackRate.value = rate;
  source.connect(context.destination);
  source.start();
}

document.addEventListener("pointerdown", () => context?.resume().catch(() => {}), { passive: true });
