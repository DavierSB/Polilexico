import { h, replace } from "../dom.js";
import { go } from "../router.js";
import { flag } from "../parts.js";
import { confirm } from "../dialogs.js";
import { save } from "../store.js";
import { setting } from "../settings.js";
import { account } from "./api.js";
import { online } from "./client.js";
import { photoAvatar, profileDialog } from "./profile.js";
import { classicTime, duplicateDetail } from "./lobby.js";
import { remindAdjourned } from "./reminders.js";

const ONLINE_ROUTES = ["sala", "sala-duplicada"];
const GAME_ROUTES = ["partida", "duplicada-partida", "sprint-partida", "recuerdas-partida", "anagrama-partida"];
const LEAVE_GAME = "¿Dejar esta partida y aceptar el reto?";

export function watchInvites() {
  const holder = h("div.invite-pop");
  document.body.append(holder);
  if (account() && !setting("hidden")) online.acquire();
  const shown = { key: null };
  const draw = () => {
    const next = current(online, shown.key);
    if ((next?.key ?? null) !== shown.key) (replace(holder, next?.el), (shown.key = next?.key ?? null));
    remindAdjourned(online, canInterrupt(online) && !inGame() && !holder.childElementCount);
  };
  online.subscribe(draw);
  window.addEventListener("hashchange", draw);
}

function current(c, kept) {
  if (!canInterrupt(c)) return null;
  const invites = c.lobby.invites.filter((s) => s.mode !== "duplicate" && (s.resume || !setting("noChallenges")));
  const invite = invites.find((s) => "invite:" + s.id === kept) ?? invites[0];
  if (invite) return { key: "invite:" + invite.id, el: inviteCard(c, invite) };
  const seek = c.offer && c.lobby.seeks.find((s) => s.id === c.offer);
  return seek && !inGame() ? { key: "offer:" + seek.id, el: offerCard(c, seek) } : null;
}

function canInterrupt(c) {
  return !ONLINE_ROUTES.includes(route()) && (!c.room || c.room.watching);
}

function route() {
  return location.hash.replace(/^#\/?/, "").split("/")[0];
}

function inGame() {
  return GAME_ROUTES.includes(route());
}

function inviteCard(c, s) {
  const title = s.resume ? `${s.host} ${flag(s.country)} quiere retomar la partida aplazada` : `${s.host} ${flag(s.country)} te reta`;
  return card(s, title, `${classicTime(s)} · ${s.void ? "Void" : "Single"}`,
    ["Rechazar", () => c.roomAction("decline", s.id)],
    ["Aceptar", () => accept(() => c.accept(s), "classic")]);
}

function offerCard(c, s) {
  return card(s, `${s.host} ${flag(s.country)} creó una duplicada`, duplicateDetail(s),
    ["Ahora no", () => c.dismissOffer()],
    ["Unirse", () => (c.dismissOffer(), join(() => c.accept(s), "duplicate"))]);
}

function card(s, title, detail, [no, onNo], [yes, onYes]) {
  return h("div.card.invite-card", {},
    h("div.row.invite-head", {}, h("button.avatar-btn.opens-profile", { "aria-label": `Perfil de ${s.host}`, onclick: () => profileDialog({ name: s.host, country: s.country }, true) }, photoAvatar(s.host, s.country, 44)), h("div.grow", {}, h("b", {}, title), h("div.muted.small", {}, detail))),
    h("div.row.invite-buttons", {}, h("button.btn.outlined.grow", { onclick: onNo }, no), h("button.btn.grow", { onclick: onYes }, yes)),
  );
}

async function accept(take, mode) {
  if (inGame() && !(await confirm(LEAVE_GAME, "Aceptar", "Cancelar"))) return;
  join(take, mode);
}

function join(take, mode) {
  take();
  save(`tab.${mode}`, "online");
  go(mode === "duplicate" ? "duplicada" : "clasica");
}
