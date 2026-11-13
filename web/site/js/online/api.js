import { load, save } from "../store.js";

const DEFAULT_SERVER = "https://polilexico1.onrender.com";
const OFFLINE = "No se pudo conectar con el servidor.";
const REFRESH_MARGIN_MS = 60_000;

export function server() {
  const asked = new URLSearchParams(location.search).get("server");
  if (asked) save("server", asked);
  return load("server", DEFAULT_SERVER).replace(/\/$/, "");
}

export function socketUrl() {
  return server().replace(/^http/, "ws") + "/ws";
}

export async function request(method, path, body = null, token = null) {
  const response = await fetch(server() + path, {
    method,
    headers: { ...(body ? { "Content-Type": "application/json" } : {}), ...(token ? { Authorization: "Bearer " + token } : {}) },
    body: body ? JSON.stringify(body) : null,
  }).catch(() => null);
  if (!response) throw new Error(OFFLINE);
  const data = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(data.error || OFFLINE);
  return data;
}

export function account() {
  return load("session", null);
}

export async function signUp(form) {
  return keep(await request("POST", "/signup", form));
}

export async function logIn(form) {
  return keep(await request("POST", "/login", form));
}

export function renamed(name, country) {
  const s = account();
  if (s) save("session", { ...s, name, country });
}

export function logOut() {
  save("session", null);
}

export async function token() {
  const s = account();
  if (!s) return null;
  if (s.expiresAt - Date.now() > REFRESH_MARGIN_MS) return s.accessToken;
  try {
    return keep(await request("POST", "/refresh", { refreshToken: s.refreshToken })).accessToken;
  } catch (e) {
    if (e.message !== OFFLINE) logOut();
    return null;
  }
}

function keep(data) {
  const session = {
    accessToken: data.accessToken, refreshToken: data.refreshToken, name: data.name, country: data.country,
    expiresAt: Date.now() + (data.expiresIn ?? 3600) * 1000,
  };
  save("session", session);
  return session;
}

export async function presence() {
  return request("GET", "/presence").then((d) => d.online).catch(() => null);
}
