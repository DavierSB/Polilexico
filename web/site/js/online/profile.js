import { h, replace } from "../dom.js";
import { go } from "../router.js";
import { screenHead, nameFlag, initialAvatar } from "../parts.js";
import { openDialog, actions, confirm, toast } from "../dialogs.js";
import { account, request, token, server, logOut } from "./api.js";
import { countryName } from "./countries.js";
import { online } from "./client.js";
import { tellDialog } from "./tell.js";
import { onlineBlock } from "../games/stats.js";

const PHOTO_SIDE = 256;
const MAX_BYTES = 60000;
let photoVersion = Date.now();

export function photoAvatar(name, country, size = 40) {
  const img = h("img.avatar", { src: `${server()}/photo?name=${encodeURIComponent(name)}&country=${encodeURIComponent(country ?? "")}&v=${photoVersion}`, alt: "", width: size, height: size, loading: "lazy" });
  img.addEventListener("error", () => img.replaceWith(initialAvatar(name, size)), { once: true });
  return img;
}

export function profileDialog(person) {
  const body = h("div.stack", {}, h("div.searching.short", {}, h("div.spinner")));
  openDialog((close) => [
    h("div.row.profile-head", {}, photoAvatar(person.name, person.country, 64), h("div.stack", {}, h("h2", {}, nameFlag(person.name, person.country)), h("span.muted", {}, countryName(person.country)))),
    body,
    actions(
      h("button.text-btn", { onclick: () => (close(), tellDialog(person)) }, "Escribir"),
      person.playing ? null : h("button.text-btn", { onclick: () => (close(), go("buscar/" + encodeURIComponent(person.name) + "/" + person.country)) }, "Retar"),
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
  const photo = h("div.profile-photo");
  const drawPhoto = () => replace(photo, photoAvatar(me.name, me.country, 96));
  drawPhoto();
  const file = h("input", { type: "file", accept: "image/*", hidden: true, onchange: (e) => upload(e.target.files[0], drawPhoto) });
  const body = h("div.stack");
  app.append(h("div.screen", {}, screenHead("Perfil en línea", "en-linea"),
    h("div.card.profile-card", {},
      h("button.photo-btn", { "aria-label": "Cambiar foto", onclick: () => file.click() }, photo, h("span.small.accent", {}, "Cambiar foto")), file,
      h("div.stack", {}, h("h2", {}, nameFlag(me.name, me.country)), h("span.muted", {}, countryName(me.country))),
    ),
    body,
    h("button.text-btn.danger", { onclick: () => leave() }, "Cerrar sesión"),
  ));
  stats("/stats", body);
}

async function upload(f, done) {
  if (!f) return;
  try {
    const data = await shrink(f);
    await request("PUT", "/photo", { data }, await token());
    photoVersion = Date.now();
    done();
    toast("Foto actualizada.");
  } catch (e) {
    toast(e.message || "No se pudo subir la foto.");
  }
}

async function shrink(f) {
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
