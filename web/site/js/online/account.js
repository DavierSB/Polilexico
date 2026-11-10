import { h, replace } from "../dom.js";
import { go } from "../router.js";
import { screenHead, flag } from "../parts.js";
import { logIn, signUp } from "./api.js";
import { countries, DEFAULT_COUNTRY } from "./countries.js";
import { choiceRow } from "../classic/options.js";

export function accountScreen(app) {
  const form = { mode: 0, name: "", country: DEFAULT_COUNTRY, password: "", email: "" };
  const body = h("div.stack.account-form");
  const redraw = () => replace(body, fields(form, redraw));
  redraw();
  app.append(h("div.screen", {}, screenHead("Cuenta en línea", "en-linea"), body));
}

function fields(form, redraw) {
  const signing = form.mode === 1;
  const error = h("p.error", { role: "alert" });
  const submit = h("button.btn.block", { type: "submit" }, signing ? "Crear cuenta" : "Entrar");
  return h("form.stack", { onsubmit: (e) => send(e, form, error, submit) },
    choiceRow(["Entrar", "Crear cuenta"], form.mode, (i) => ((form.mode = i), redraw())),
    input("Usuario", "text", form, "name", { autocomplete: "username", required: true, maxlength: 20 }),
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
    await (form.mode === 1 ? signUp(data) : logIn(data));
    go("en-linea", true);
  } catch (err) {
    error.textContent = err.message;
    submit.disabled = false;
  }
}
