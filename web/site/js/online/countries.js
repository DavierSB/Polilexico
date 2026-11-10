const DEFAULT_COUNTRY = "CU";

const FILE_MEMBERS = new Set(["AR", "CL", "CO", "CR", "DO", "EC", "ES", "FR", "GB", "GT", "HN", "IL", "JP", "MX", "NZ", "PA", "PE", "PR", "PY", "US", "UY", "VE"]);

const NOT_COUNTRIES = new Set(["EU", "EZ", "UN", "QO", "XA", "XB", "XK", "ZZ", "AC", "CP", "DG", "EA", "IC", "TA", "CQ"]);

let cached = null;

export function countries() {
  cached ??= build();
  return cached;
}

export function countryName(code) {
  return countries().find((c) => c.code === code)?.name ?? code;
}

function build() {
  const names = new Intl.DisplayNames(["es"], { type: "region", fallback: "none" });
  const list = [];
  for (let a = 65; a <= 90; a++) for (let b = 65; b <= 90; b++) add(list, names, String.fromCharCode(a, b));
  return list.sort((x, y) => rank(x.code) - rank(y.code) || plain(x.name).localeCompare(plain(y.name), "es"));
}

function add(list, names, code) {
  if (NOT_COUNTRIES.has(code)) return;
  const name = names.of(code);
  if (name && name !== code) list.push({ code, name });
}

function rank(code) {
  if (code === DEFAULT_COUNTRY) return 0;
  return FILE_MEMBERS.has(code) ? 1 : 2;
}

function plain(text) {
  return text.normalize("NFD").replace(/\p{Diacritic}/gu, "").toLowerCase();
}

export { DEFAULT_COUNTRY };
