import { h, replace } from "../dom.js";
import { lobbyRoute } from "./lobby.js";
import { go } from "../router.js";
import { screenHead, flag } from "../parts.js";
import { logIn, signUp } from "./api.js";
import { countries, DEFAULT_COUNTRY } from "./countries.js";
import { choiceRow } from "../classic/options.js";
import { shrink, savePhoto } from "./profile.js";

export function accountScreen(app) {
  const form = { mode: 0, name: "", country: DEFAULT_COUNTRY, password: "", email: "", photo: null };
  const body = h("div.stack.account-form.account-card");
  const redraw = () => replace(body, fields(form, redraw));
  redraw();
  app.append(h("div.screen", {}, screenHead("En línea", lobbyRoute()), body));
}

function photoPicker(form, redraw) {
  const file = h("input", { type: "file", accept: "image/*", hidden: true, onchange: (e) => pick(e.target.files[0], form, redraw) });
  const shown = form.photo ? h("img.account-photo", { src: "data:image/jpeg;base64," + form.photo, alt: "Foto" })
    : /^\p{L}/u.test(form.name) ? h("span.account-photo.initial", {}, form.name[0].toUpperCase()) : h("span.account-photo.empty", {}, "Sin foto");
  return h("div.stack.account-photo-box", {}, file,
    h("button.account-photo-btn", { type: "button", onclick: () => file.click() }, shown),
    h("button.text-btn", { type: "button", onclick: () => file.click() }, form.photo ? "Cambiar foto" : "Elegir foto"));
}

async function pick(f, form, redraw) {
  if (!f) return;
  try {
    form.photo = await shrink(f);
  } catch {
    form.photo = null;
  }
  redraw();
}

function fields(form, redraw) {
  const signing = form.mode === 0;
  const error = h("p.error", { role: "alert" });
  const submit = h("button.btn.block", { type: "submit" }, signing ? "Crear cuenta" : "Entrar");
  return h("form.stack", { onsubmit: (e) => send(e, form, error, submit) },
    choiceRow(["Crear cuenta", "Ya tengo cuenta"], form.mode, (i) => ((form.mode = i), redraw())),
    signing ? photoPicker(form, redraw) : null,
    input("Usuario", "text", form, "name", { autocomplete: "username", required: true, maxlength: 20, onchange: () => signing && !form.photo && redraw() }),
    countrySelect(form),
    input("Contraseña", "password", form, "password", { autocomplete: signing ? "new-password" : "current-password", required: true, minlength: signing ? 6 : null }),
    signing ? input("Correo (opcional)", "email", form, "email", { autocomplete: "email" }) : null,
    error,
    submit,
  );
}

function input(label, type, form, key, extra) {
  const id = "account-" + key;
  return h("div.field", {},
    h("label", { for: id }, label),
    h("input", { id, type, value: form[key], oninput: (e) => (form[key] = e.target.value), ...extra }),
  );
}

function countrySelect(form) {
  return h("div.field", {},
    h("label", { for: "account-country" }, "País"),
    h("select", { id: "account-country", onchange: (e) => (form.country = e.target.value) },
      countries().map((c) => h("option", { value: c.code, selected: c.code === form.country }, `${flag(c.code)} ${c.name}`)),
    ),
  );
}

async function send(e, form, error, submit) {
  e.preventDefault();
  submit.disabled = true;
  error.textContent = "";
  const data = { name: form.name.trim(), country: form.country, password: form.password, email: form.email.trim() };
  try {
    await (form.mode === 0 ? signUp(data) : logIn(data));
    if (form.mode === 0 && form.photo) await savePhoto(form.photo).catch(() => {});
    go(lobbyRoute(), true);
  } catch (err) {
    error.textContent = err.message;
    submit.disabled = false;
  }
}
