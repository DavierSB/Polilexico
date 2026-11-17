import { h } from "../dom.js";
import { initialAvatar } from "../parts.js";
import { server } from "../online/api.js";
import { bot, botPhoto } from "../classic/bots.js";

const SIZE = 22;
const YOU = "Tú";
const SEAT_COUNTRY = /^(.*) \(([A-Z]{2})\)$/;

export function reviewPlayers(game, record, opponent) {
  const kind = kindOf(game, record);
  const me = bottomName(game, kind);
  return { me, faces: facesOf(game, kind, me, opponent) };
}

export function facePhoto(face, name) {
  if (!face) return null;
  if (face.bot) return botImage(name);
  const url = photoUrl(face, name);
  return url ? personImage(url, name) : initialAvatar(name, SIZE);
}

function kindOf(game, record) {
  if (record.watched) return "watched";
  if (record.online || game.OpponentID) return "online";
  return game.Local ? "local" : "bot";
}

function bottomName(game, kind) {
  const named = kind === "watched" || kind === "local";
  return game.Mode !== "duplicate" && named && game.Me ? game.Me : YOU;
}

function facesOf(game, kind, me, opponent) {
  if (game.Mode === "duplicate" || kind === "local") return new Map();
  if (kind === "bot") return new Map([[opponent, { bot: true }]]);
  const rival = [opponent, { country: game.OpponentCountry, id: game.OpponentID }];
  if (kind === "online") return new Map([rival]);
  return new Map([[me, { country: game.MeCountry, id: game.MeID }], rival]);
}

function botImage(name) {
  const b = bot(name);
  return b ? h("img.avatar", { src: botPhoto(b), alt: "", width: SIZE, height: SIZE }) : null;
}

function personImage(url, name) {
  const img = h("img.avatar", { src: url, alt: "", width: SIZE, height: SIZE, loading: "lazy" });
  img.addEventListener("error", () => img.replaceWith(initialAvatar(name, SIZE)), { once: true });
  return img;
}

function photoUrl(face, name) {
  if (face.id) return `${server()}/photo?id=${encodeURIComponent(face.id)}`;
  const found = SEAT_COUNTRY.exec(name);
  const country = face.country || found?.[2];
  if (!country) return null;
  return `${server()}/photo?name=${encodeURIComponent(found?.[1] ?? name)}&country=${encodeURIComponent(country)}`;
}
