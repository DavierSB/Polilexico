import { h, replace } from "../dom.js";
import { openDialog, actions } from "../dialogs.js";
import { flag, nameFlag } from "../parts.js";
import { account } from "./api.js";
import { online } from "./client.js";
import { photoAvatar } from "./profile.js";
import { chatRecords } from "./chatLog.js";

const MAX_CHARS = 140;
const GLYPH = "💬";
const composing = { open: false };
const popup = { close: null, draw: null };

export function watchMessages() {
  online.subscribe((c) => (c.inbox.length && !composing.open ? showPopup(c) : popup.close?.()));
}

export function messagesButton() {
  const badge = h("span.msg-badge.hidden");
  const el = h("button.icon-btn.messages-btn", { "aria-label": "Mensajes", onclick: () => writeMessage() }, h("span.glyph", {}, GLYPH), badge);
  const stop = online.subscribe((c) => {
    badge.textContent = c.inbox.length;
    badge.classList.toggle("hidden", !c.inbox.length);
  });
  return { el, stop };
}

export function writeMessage(to = null) {
  composing.open = true;
  popup.close?.();
  online.acquire();
  const subscription = { stop: null };
  openDialog((close) => composer(to, close, subscription), { wide: true, onClose: () => (subscription.stop?.(), (composing.open = false), online.release()) });
}

function contacts(c) {
  const me = account();
  return c.lobby.people.filter((p) => !(p.name === me?.name && p.country === me?.country)).sort((a, b) => a.name.localeCompare(b.name));
}

function find(c, nick) {
  const wanted = nick.trim().toLowerCase();
  return wanted ? contacts(c).find((p) => p.name.toLowerCase() === wanted) ?? null : null;
}

function composer(preset, close, subscription) {
  const nick = h("input.msg-to", { value: (preset ?? online.lastTo)?.name ?? "", maxlength: 16, autocomplete: "off", "aria-label": "Para" });
  const prefix = h("span.msg-flag");
  const missing = h("span.small.error.hidden", {}, "No está conectado");
  const text = h("textarea.msg-text", { rows: 3, maxlength: MAX_CHARS, "aria-label": "Mensaje", placeholder: "Mensaje" });
  const counter = h("span.small.muted.msg-count", {}, `0 / ${MAX_CHARS}`);
  const listen = h("button.text-btn.hidden", { onclick: () => online.ignoreAll(false) }, "Volver a recibir mensajes");
  const sendBtn = h("button.btn.small", { onclick: () => send(nick.value, text.value, close) }, "Enviar");
  const refresh = () => {
    const to = find(online, nick.value);
    prefix.textContent = to?.country ? flag(to.country) : "";
    missing.classList.toggle("hidden", !nick.value.trim() || !!to);
    counter.textContent = `${text.value.length} / ${MAX_CHARS}`;
    listen.classList.toggle("hidden", !online.ignoringAll());
    sendBtn.disabled = !to || !text.value.trim();
  };
  nick.addEventListener("input", refresh);
  text.addEventListener("input", refresh);
  subscription.stop = online.subscribe(refresh);
  const pick = () => pickContact((p) => ((nick.value = p.name), refresh()));
  return [
    h("div.row.msg-title", {}, h("h2.grow", {}, "Mensaje"), h("button.text-btn", { onclick: historyDialog }, "Historial")),
    h("div.stack.msg-form", {},
      h("label.msg-field", {}, h("span.msg-label", {}, "Para"), h("span.row.msg-input", {}, prefix, nick, h("button.text-btn.msg-pick", { "aria-label": "Elegir", onclick: pick }, "▾"))),
      missing,
      h("label.msg-field", {}, h("span.msg-label", {}, "Mensaje"), text),
      counter,
      listen,
    ),
    actions(h("button.text-btn", { onclick: close }, "Cancelar"), sendBtn),
  ];
}

function send(nick, text, close) {
  const to = find(online, nick);
  if (!to || !text.trim()) return;
  online.tell(to, text.trim());
  close();
}

function pickContact(chosen) {
  openDialog((close) => {
    const people = contacts(online);
    return [
      h("h2", {}, "Escribir a"),
      h("div.stack.dialog-list", {},
        people.length ? [h("h3.side-title", {}, "Conectados"), people.map((p) => contactRow(p, () => (close(), chosen(p))))] : h("p", {}, "No hay nadie más conectado."),
      ),
      actions(h("button.text-btn", { onclick: close }, "Cerrar")),
    ];
  }, { wide: true });
}

function contactRow(p, onclick) {
  return h("button.player-row.contact-row", { onclick }, photoAvatar(p.name, p.country, 40), h("span.grow", {}, nameFlag(p.name, p.country)));
}

export function historyDialog() {
  const records = chatRecords();
  openDialog((close) => {
    const list = records.length ? h("div.stack.history-list", {}, records.map(bubble)) : emptyHistory();
    requestAnimationFrame(() => (list.scrollTop = list.scrollHeight));
    return [h("h2", {}, "Historial"), list, actions(h("button.text-btn", { onclick: close }, "Cerrar"))];
  }, { wide: true });
}

function emptyHistory() {
  return h("div.empty-state", {}, h("span.glyph-big", {}, GLYPH), h("span", {}, "Aún no hay mensajes"));
}

function bubble(r) {
  return h("div", { class: "history-bubble" + (r.mine ? " mine" : "") },
    r.room && r.mine ? roomAvatar() : photoAvatar(r.who, r.country, 32),
    h("div.grow", {}, h("div.row", {}, h("b.grow.ellipsis", {}, route(r)), h("span.small.muted", {}, stamp(r.at))), h("div.history-text", {}, r.text)),
  );
}

function roomAvatar() {
  return h("span.room-avatar", {}, "👥");
}

function route(r) {
  const other = r.country ? `${r.who} ${flag(r.country)}` : r.who;
  if (r.mine && r.room) return "Tú → Toda la sala";
  if (r.mine) return `Tú → ${other}`;
  if (r.room) return `${other} → Toda la sala`;
  return `${other} → Tú`;
}

function stamp(at) {
  const d = new Date(at);
  const day = new Intl.DateTimeFormat("es", { day: "numeric", month: "short" }).format(d).replace(".", "");
  return `${day} ${String(d.getHours()).padStart(2, "0")}:${String(d.getMinutes()).padStart(2, "0")}`;
}

function showPopup(c) {
  if (popup.draw) return popup.draw(c);
  const box = h("div.chat-popup");
  popup.draw = (next) => replace(box, popupBody(next));
  popup.close = openDialog(() => box, { onClose: () => ((popup.close = null), (popup.draw = null), online.inbox.length && online.closeChat()) });
  popup.draw(c);
}

function popupBody(c) {
  const last = c.inbox[c.inbox.length - 1];
  const older = c.inbox.length - 1;
  return [
    h("b.popup-from", {}, `${last.from} dice:`),
    h("p.popup-quote", {}, `“${last.text}”`),
    older ? h("span.small.muted", {}, older === 1 ? "+1 anterior" : `+${older} anteriores`) : null,
    h("button.btn.block", { onclick: () => (c.closeChat(), writeMessage({ name: last.from, country: last.country })) }, "Responder"),
    h("button.btn.block.outlined", { onclick: () => c.closeChat() }, "Cerrar"),
    h("button.text-btn.small-link", { onclick: () => c.ignore({ name: last.from, country: last.country }) }, `Ignorar mensajes de ${last.from}`),
    h("button.text-btn.small-link", { onclick: () => c.ignoreAll(true) }, "Ignorar todos los mensajes"),
  ];
}
