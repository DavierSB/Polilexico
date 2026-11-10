import { load, save } from "./store.js";

const NUMBER_LABELS = { dl: "2L", tl: "3L", dw: "2P", tw: "3P", center: "★" };

const PALETTES = {
  Polimita: {
    primary: "#E2A146", "on-primary": "#2A140C", secondary: "#F2831E", tertiary: "#E8C07A", bg: "#1B1411",
    "surface-variant": "#3A2C24", "on-surface": "#FFF1D6", "on-surface-variant": "#D9C4B0", outline: "#9E8A7A",
    "outline-variant": "#4F4037", "primary-container": "#5A3A12", "on-primary-container": "#FFDDA6",
    "secondary-container": "#4A2C1A", "on-secondary-container": "#FFDCC2",
    c0: "#140E0B", c1: "#231A16", c2: "#281E19", c3: "#32271F", c4: "#3D3029",
  },
  Hoja: {
    primary: "#8FD46A", "on-primary": "#10200C", secondary: "#F07AA8", tertiary: "#D8E36A", bg: "#0E140D",
    "surface-variant": "#263323", "on-surface": "#EEF4EA", "on-surface-variant": "#C3CFB8", outline: "#8A9A80",
    "outline-variant": "#364433", "primary-container": "#2E5A1E", "on-primary-container": "#D2F0B8",
    "secondary-container": "#5A1E38", "on-secondary-container": "#FFD6E6",
    c0: "#090F08", c1: "#151D14", c2: "#192218", c3: "#212B1F", c4: "#2A3528",
  },
  Celeste: {
    primary: "#74ACDF", "on-primary": "#0B1E33", secondary: "#F6B40E", tertiary: "#B9D6F0", bg: "#0F1620",
    "surface-variant": "#24303D", "on-surface": "#E8F0F8", "on-surface-variant": "#BFCBD8", outline: "#8595A6",
    "outline-variant": "#35414F", "primary-container": "#1F4466", "on-primary-container": "#CDE5FA",
    "secondary-container": "#4A3A0C", "on-secondary-container": "#FFE3A0",
    c0: "#0A1018", c1: "#151D28", c2: "#19222E", c3: "#212B38", c4: "#2A3543",
  },
  Noche: {
    primary: "#7FA6E8", "on-primary": "#0A1A33", secondary: "#E8D9B5", tertiary: "#A9B8D6", bg: "#10141C",
    "surface-variant": "#262C38", "on-surface": "#E4E8F0", "on-surface-variant": "#BCC3D0", outline: "#8A93A3",
    "outline-variant": "#353C4A", "primary-container": "#263F6B", "on-primary-container": "#D6E3FF",
    "secondary-container": "#45402F", "on-secondary-container": "#F5ECD4",
    c0: "#0B0E14", c1: "#161B24", c2: "#1A2029", c3: "#222833", c4: "#2B323E",
  },
};

const BOARDS = {
  Polimita: {
    dark: board("#1B1411", "#2A201B", "#80601C", "#763B19", "#DE5445", "#E6320A", "#FFF1DE", "#F5E6CC", "#2A140C", "#D9522A", "#FFC85A", "#F7B27A", "#E2A146", "#2A140C", "#E2A146", "#FF6B4A", "#9E8A7A"),
    light: board("#E6D3B3", "#F8EDD6", "#F2CF8A", "#C67D10", "#EAB8A9", "#F0340A", "#4A2A18", "#3B2418", "#FFF1D6", "#E2A146", "#8A3B12", "#B8741E", "#E2A146", "#2A140C", "#E2A146", "#E0402A", "#8A6E58"),
  },
  Hoja: {
    dark: board("#0E140D", "#1A2419", "#4DAC55", "#138626", "#BB547F", "#AA1855", "#F2F6EE", "#F6F3E6", "#10200C", "#D9457F", "#F2D36B", "#F3B6CC", "#8FD46A", "#10200C", "#8FD46A", "#FF6B9A", "#8A9A80"),
    light: board("#DCD6BE", "#F5F2E3", "#BBE2A7", "#218329", "#EBB2CA", "#B81E5E", "#1E2A18", "#1F3A1A", "#F6F3E6", "#F07AA8", "#4A7A1E", "#A8336A", "#8FD46A", "#10200C", "#8FD46A", "#FF6B9A", "#7A8A70"),
  },
  Celeste: {
    dark: board("#0F1620", "#1C2733", "#5D97C6", "#1257A5", "#BE8E46", "#E09600", "#E8F0F8", "#F4F7FA", "#0B1E33", "#34597A", "#F6D26B", "#A9CFEF", "#74ACDF", "#0B1E33", "#74ACDF", "#FF6B5A", "#8595A6"),
    light: board("#DCD8CC", "#F6F2E7", "#A9CFEA", "#185CAA", "#ECD09C", "#E69900", "#1A2838", "#163252", "#F4F7FA", "#F6B40E", "#2F5F94", "#8A6A10", "#74ACDF", "#0B1E33", "#74ACDF", "#FF6B5A", "#75808C"),
  },
  Noche: {
    dark: board("#1B1E24", "#2B2F37", "#5B8AB9", "#1C519C", "#B65D6C", "#A02233", "#B9C0CC", "#E9E4D4", "#1B1E24", "#3E7FC1", "#FFD166", "#9FE3B0", "#7FA6E8", "#0A1A33", "#7FA6E8", "#FF5252", "#8A93A3"),
    light: board("#D8D1BF", "#F3EEE1", "#B5CAE8", "#27519B", "#E6B7BD", "#A32939", "#1B1E24", "#1E2A44", "#F2ECDA", "#E8D9B5", "#4A6FB0", "#2F6E4E", "#7FA6E8", "#0A1A33", "#7FA6E8", "#FF5252", "#7A8090"),
  },
};

function board(lines, square, dl, tl, dw, tw, bonusText, tile, letter, blank, latest, pending, arrow, arrowInk, scoreInk, highScoreInk, coords) {
  return {
    "b-lines": lines, "b-square": square, "b-dl": dl, "b-tl": tl, "b-dw": dw, "b-tw": tw, "b-center": dw,
    "b-bonus-text": bonusText, "b-tile": tile, "b-letter": letter, "b-blank": blank, "b-latest": latest,
    "b-pending": pending, "b-arrow": arrow, "b-arrow-ink": arrowInk, "b-score-ink": scoreInk,
    "b-high-score-ink": highScoreInk, "b-coords": coords,
  };
}

export const THEMES = Object.keys(PALETTES);

export const LABELS = NUMBER_LABELS;

let current = { name: load("theme", "Polimita"), light: load("lightBoard", false) };
let override = null;

export function theme() {
  return current;
}

export function choose(name) {
  current = { ...current, name };
  override = null;
  save("theme", name);
  apply();
}

export function chooseLight(light) {
  current = { ...current, light };
  save("lightBoard", light);
  apply();
}

export function overrideWith(name) {
  override = name;
  apply();
}

export function swatches(name) {
  const look = BOARDS[name][current.light ? "light" : "dark"];
  return [PALETTES[name].primary, look["b-tile"], look["b-tw"]];
}

export function apply() {
  const name = PALETTES[override] ? override : PALETTES[current.name] ? current.name : "Polimita";
  const vars = { ...PALETTES[name], ...BOARDS[name][current.light ? "light" : "dark"] };
  const root = document.documentElement.style;
  Object.entries(vars).forEach(([k, v]) => root.setProperty("--" + k, v));
  document.querySelector('meta[name="theme-color"]')?.setAttribute("content", vars.bg);
}
