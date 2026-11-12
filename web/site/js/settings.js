import { load, save } from "./store.js";

const DEFAULTS = { liveScore: true, showUnseen: true, hidden: false, noChallenges: false };

export function setting(name) {
  return load("setting." + name, DEFAULTS[name]);
}

export function setSetting(name, on) {
  save("setting." + name, on);
}
