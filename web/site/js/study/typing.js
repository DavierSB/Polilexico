import { BLANK, LETTERS } from "../tiles.js";

const SHIFTED = { C: "CH", L: "LL", R: "RR" };
const SENTINEL = "··";

export function typedTile(e) {
  if (e.key.length !== 1) return null;
  return tileOf(e.key, e.shiftKey);
}

function tileOf(ch, shifted = ch !== ch.toLowerCase()) {
  if (ch === "?" || ch === " ") return BLANK;
  const upper = ch.toUpperCase();
  if (shifted && SHIFTED[upper]) return SHIFTED[upper];
  return LETTERS.includes(upper) && upper.length === 1 ? upper : null;
}

export function softKeyboard({ add, back }) {
  const input = document.createElement("input");
  Object.assign(input, { className: "soft-keys", value: SENTINEL, autocomplete: "off", spellcheck: false, inputMode: "text", enterKeyHint: "done" });
  input.setAttribute("autocapitalize", "none");
  input.setAttribute("autocorrect", "off");
  input.setAttribute("aria-label", "Escribir el atril");
  const reset = () => ((input.value = SENTINEL), input.setSelectionRange(SENTINEL.length, SENTINEL.length));
  input.addEventListener("input", () => {
    const value = input.value;
    if (value.length < SENTINEL.length) back();
    else [...value.slice(SENTINEL.length)].map((c) => tileOf(c)).filter(Boolean).forEach(add);
    reset();
  });
  input.addEventListener("keydown", (e) => e.key === "Enter" && input.blur());
  return { input, open: () => (reset(), input.focus()) };
}
