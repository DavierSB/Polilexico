import { h, replace } from "../dom.js";
import { lobbyRoute, adjournedWith, resumeWith, challenge } from "./lobby.js";
import { go } from "../router.js";
import { nameFlag, initialAvatar, flag } from "../parts.js";
import { icon } from "../icons.js";
import { openDialog, actions, confirm, toast } from "../dialogs.js";
import { account, request, token, server, logOut, renamed } from "./api.js";
import { countryName, countries } from "./countries.js";
import { online } from "./client.js";
import { writeMessage, messagesButton } from "./messages.js";
import { onlineBlock } from "../games/stats.js";

const PHOTO_SIDE = 256;
const MAX_BYTES = 60000;
let photoVersion = Date.now();

export function photoAvatar(name, country, size = 40) {
  const img = h("img.avatar", { src: `${server()}/photo?name=${encodeURIComponent(name)}&country=${encodeURIComponent(country ?? "")}&v=${photoVersion}`, alt: "", width: size, height: size, loading: "lazy" });
  img.addEventListener("error", () => img.replaceWith(initialAvatar(name, size)), { once: true });
  return img;
}

export function accountButtons() {
  if (!account()) return { el: null, stop: () => {} };
  const messages = messagesButton();
  return { el: h("div.row.head-actions", {}, messages.el, profileButton()), stop: messages.stop };
}

export function profileButton() {
  const me = account();
  if (!me) return null;
  return h("button.icon-btn.profile-btn", { "aria-label": "Perfil en línea", onclick: () => go("perfil") }, photoAvatar(me.name, me.country, 32));
}

function challengeAction(person, close) {
  const game = adjournedWith(online, person);
  if (game) return h("button.text-btn", { disabled: !!game.busy, onclick: () => (close(), resumeWith(online, game)) }, "Retomar");
  return h("button.text-btn", { onclick: () => (close(), challenge(person)) }, "Retar");
}

export function profileDialog(person) {
  const body = h("div.stack", {}, h("div.searching.short", {}, h("div.spinner")));
  openDialog((close) => [
    h("div.row.profile-head", {}, photoAvatar(person.name, person.country, 64), h("div.stack", {}, h("h2", {}, nameFlag(person.name, person.country)), h("span.muted", {}, countryName(person.country)))),
    body,
    actions(
      h("button.text-btn", { onclick: () => (close(), writeMessage(person)) }, "Escribir"),
      person.playing ? null : challengeAction(person, close),
      h("button.text-btn", { onclick: close }, "Cerrar"),
    ),
  ], { wide: true });
  stats(`/profile?name=${encodeURIComponent(person.name)}&country=${encodeURIComponent(person.country)}`, body);
}

async function stats(path, body) {
  try {
    const data = await request("GET", path, null, await token());
    const rows = data.rows ?? [];
    replace(body, rows.length ? rows.map(onlineBlock) : h("p.muted", {}, "Aún no hay partidas en línea."));
  } catch (e) {
    replace(body, h("p.error", {}, e.message));
  }
}

export function profileScreen(app) {
  const me = account();
  if (!me) return go("cuenta", true);
  const ui = { data: { name: me.name, country: me.country, email: "" }, editing: false, busy: false, notice: null };
  ui.screen = h("div.screen.profile-screen");
  ui.draw = () => drawProfile(ui);
  app.append(ui.screen);
  ui.draw();
  checkPhoto(ui);
  token().then((t) => request("GET", "/account", null, t)).then((a) => ((ui.data = { name: a.name, country: a.country, email: a.email ?? "" }), ui.draw())).catch(() => {});
}

async function checkPhoto(ui) {
  const r = await fetch(`${server()}/photo?name=${encodeURIComponent(ui.data.name)}&country=${encodeURIComponent(ui.data.country)}&v=${photoVersion}`).catch(() => null);
  ui.hasPhoto = !!r?.ok;
  ui.draw();
}

function drawProfile(ui) {
  const back = ui.editing ? () => ((ui.editing = false), (ui.notice = null), ui.draw()) : () => history.length > 1 ? history.back() : go(lobbyRoute());
  replace(ui.screen,
    h("header.screen-head", {}, h("button.icon-btn", { "aria-label": "Volver", onclick: back }, icon("back")), h("h1", {}, ui.editing ? "Editar perfil" : "Mi perfil")),
    h("div.stack.big-identity", {}, photoAvatar(ui.data.name, ui.data.country, 120), h("h2", {}, `${ui.data.name}  ${flag(ui.data.country)}`)),
    ui.editing ? editFields(ui) : profileView(ui),
    ui.notice ? h("p.error", {}, ui.notice) : null,
  );
}

function profileView(ui) {
  const d = ui.data;
  const row = (label, value) => h("div.row.data-row", {}, h("span.grow.muted", {}, label), h("b.ellipsis", {}, value));
  return [
    h("div.stack.data-card", {}, row("Usuario", d.name), row("País", `${flag(d.country)}  ${countryName(d.country)}`), row("Correo", d.email || "Sin correo"), row("Contraseña", "••••••")),
    h("div.row.two-buttons", {},
      h("button.btn.outlined.grow", { onclick: () => statsDialog() }, "Estadísticas"),
      h("button.btn.grow", { onclick: () => ((ui.editing = true), (ui.notice = null), ui.draw()) }, "Editar")),
    h("button.text-btn.block", { onclick: () => leave() }, "Cerrar sesión"),
  ];
}

function editFields(ui) {
  const edited = { ...ui.data, password: "" };
  const file = h("input", { type: "file", accept: "image/*", hidden: true, onchange: (e) => upload(e.target.files[0], () => checkPhoto(ui), (m) => ((ui.notice = m), ui.draw())) });
  const save = h("button.btn.grow", { disabled: ui.busy, onclick: () => saveProfile(ui, edited) }, ui.busy ? h("div.spinner.tiny") : "Guardar");
  const valid = () => (save.disabled = ui.busy || edited.name.trim().length < 3 || (edited.password && edited.password.length < 6));
  const field = (label, key, type = "text", extra = {}) => h("div.field", {}, h("label", {}, label), h("input", { type, value: edited[key], oninput: (e) => ((edited[key] = e.target.value), valid()), ...extra }));
  return [
    h("div.row.photo-buttons", {}, file,
      h("button.text-btn", { onclick: () => file.click() }, ui.hasPhoto ? "Cambiar foto" : "Poner foto"),
      ui.hasPhoto ? h("button.text-btn", { onclick: () => removePhoto(() => checkPhoto(ui), (m) => ((ui.notice = m), ui.draw())) }, "Quitar foto") : null),
    field("Usuario", "name", "text", { maxlength: 20 }),
    h("div.field", {}, h("label", {}, "País"), h("select", { onchange: (e) => (edited.country = e.target.value) }, countries().map((c) => h("option", { value: c.code, selected: c.code === edited.country }, `${flag(c.code)} ${c.name}`)))),
    field("Correo (opcional)", "email", "email"),
    field("Contraseña nueva (opcional)", "password", "password", { autocomplete: "new-password" }),
    h("div.row.two-buttons", {}, h("button.btn.outlined.grow", { onclick: () => ((ui.editing = false), ui.draw()) }, "Cancelar"), save),
  ];
}

async function saveProfile(ui, edited) {
  ui.busy = true;
  ui.draw();
  try {
    const saved = await request("PUT", "/account", { name: edited.name.trim(), country: edited.country, password: edited.password, email: edited.email.trim() }, await token());
    renamed(saved.name, saved.country);
    online.shutDown();
    ui.data = { name: saved.name, country: saved.country, email: saved.email ?? "" };
    ui.editing = false;
    ui.notice = null;
  } catch (e) {
    ui.notice = e.message;
  }
  ui.busy = false;
  ui.draw();
}

function statsDialog() {
  const body = h("div.stack.stats-dialog", {}, h("div.spinner"));
  openDialog((close) => [h("h2", {}, "Estadísticas"), body, actions(h("button.text-btn", { onclick: close }, "Cerrar"))]);
  stats("/stats", body);
}

async function removePhoto(done, fail) {
  try {
    await request("PUT", "/photo", { data: "" }, await token());
    photoVersion = Date.now();
    done();
  } catch (e) {
    fail(e.message);
  }
}

async function upload(f, done, fail = toast) {
  if (!f) return;
  try {
    const data = await shrink(f);
    await request("PUT", "/photo", { data }, await token());
    photoVersion = Date.now();
    done();
  } catch (e) {
    fail(e.message || "No se pudo leer esa foto.");
  }
}

export async function savePhoto(data) {
  await request("PUT", "/photo", { data }, await token());
  photoVersion = Date.now();
}

export async function shrink(f) {
  const bitmap = await createImageBitmap(f);
  const side = Math.min(bitmap.width, bitmap.height);
  const canvas = Object.assign(document.createElement("canvas"), { width: PHOTO_SIDE, height: PHOTO_SIDE });
  canvas.getContext("2d").drawImage(bitmap, (bitmap.width - side) / 2, (bitmap.height - side) / 2, side, side, 0, 0, PHOTO_SIDE, PHOTO_SIDE);
  for (let quality = 0.85; quality > 0.3; quality -= 0.1) {
    const url = canvas.toDataURL("image/jpeg", quality);
    const data = url.slice(url.indexOf(",") + 1);
    if (data.length * 0.75 < MAX_BYTES) return data;
  }
  throw new Error("La foto es demasiado grande.");
}

async function leave() {
  if (!(await confirm("¿Seguro que quieres cerrar sesión?", "Cerrar sesión"))) return;
  online.shutDown();
  logOut();
  go("", true);
}
