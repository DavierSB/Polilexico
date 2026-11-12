const PREFIX = "lexico.tab.";

export function recall(key, fallback = null) {
  try {
    const raw = sessionStorage.getItem(PREFIX + key);
    return raw === null ? fallback : JSON.parse(raw);
  } catch {
    return fallback;
  }
}

export function forget(key) {
  try {
    sessionStorage.removeItem(PREFIX + key);
  } catch {
    return;
  }
}

export function remember(key, value) {
  try {
    sessionStorage.setItem(PREFIX + key, JSON.stringify(value));
  } catch {
    return;
  }
}
