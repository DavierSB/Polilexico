import { Link } from "./link.js";
import { RemoteSession } from "./remote.js";
import { readTable } from "./table.js";
import { load, save } from "../store.js";
import { involvedDialog } from "./involved.js";
import { recordChat } from "./chatLog.js";
import { setting } from "../settings.js";

const NOTICE_MS = 5000;
const CHAT_SIZE = 60;
const LINGER_MS = 15000;
export const ADJOURNED = "Partida aplazada.";
export const GAME_GONE = "La partida ya no está disponible.";

const EMPTY_LOBBY = { seeks: [], invites: [], people: [], games: [], adjourned: [] };

class OnlineClient {
  constructor() {
    this.listeners = new Set();
    this.link = null;
    this.users = 0;
    this.queued = [];
    this.inbox = [];
    this.lastTo = null;
    this.declined = new Set();
    this.reset();
  }

  reset() {
    this.status = "Conectando…";
    this.connected = false;
    this.refused = null;
    this.lobby = EMPTY_LOBBY;
    this.forgetRoom();
  }

  forgetRoom() {
    this.session?.close();
    this.session = null;
    this.table = null;
    this.room = null;
    this.notice = null;
    this.ended = null;
    this.rivalOnline = true;
    this.rivalAwaySince = null;
    this.adjournAsk = null;
    this.watchers = [];
    this.chat = [];
  }

  subscribe(fn) {
    this.listeners.add(fn);
    fn(this);
    return () => this.listeners.delete(fn);
  }

  changed() {
    this.listeners.forEach((fn) => fn(this));
  }

  acquire() {
    this.users++;
    clearTimeout(this.lingering);
    if (!this.link) this.connect();
  }

  release() {
    this.users--;
    if (this.users <= 0) this.lingering = setTimeout(() => this.disconnectIfIdle(), LINGER_MS);
  }

  connect() {
    this.refused = null;
    this.link = new Link((e) => this.handle(e), (s) => this.linkStatus(s));
    this.link.open();
  }

  disconnectIfIdle() {
    if (this.users > 0 || this.seated()) return;
    this.shutDown();
  }

  shutDown() {
    this.link?.close();
    this.link = null;
    this.queued = [];
    this.forgetRoom();
    this.reset();
    this.changed();
  }

  linkStatus(text) {
    this.status = text;
    if (text) this.connected = false;
    this.changed();
  }

  send(message) {
    return this.link?.send(message) ?? false;
  }

  sendWhenOnline(message) {
    if (this.connected && this.link?.sendSure(message)) return;
    this.enqueue(message);
  }

  enqueue(message) {
    const text = JSON.stringify(message);
    this.queued = [...this.queued.filter((m) => JSON.stringify(m) !== text), message];
  }

  seated() {
    return this.room && !this.room.watching && !this.finished() && !this.ended;
  }

  finished() {
    return !!this.session?.state?.result || this.table?.phase === "finished";
  }

  publish(proposal) {
    this.ended = null;
    if (proposal.duplicate) this.forgetRoom();
    this.sendWhenOnline({ t: "create", ...proposalFields(proposal) });
  }

  startSeek(seek) {
    this.forgetRoom();
    this.roomAction("start", seek.id);
  }

  ready() {
    this.sendWhenOnline({ t: "ready" });
  }

  dismissOffer() {
    this.offer = null;
    this.changed();
  }

  challenge(person, proposal) {
    this.ended = null;
    this.sendWhenOnline({ t: "challenge", name: person.name, country: person.country, ...proposalFields(proposal) });
  }

  seen(id) {
    this.roomAction("seen", id);
  }

  roomAction(t, code) {
    this.sendWhenOnline({ t, code });
  }

  accept(seek) {
    this.forgetRoom();
    this.roomAction("join", seek.id);
  }

  watch(gameId) {
    this.forgetRoom();
    this.roomAction("watch", gameId);
  }

  resumeAdjourned(id) {
    this.forgetRoom();
    this.roomAction("resume", id);
  }

  leave() {
    this.send({ t: "leave" });
    this.forgetRoom();
    this.changed();
  }

  move(input) {
    if (!this.send({ t: "move", input })) this.flash("Sin conexión: la jugada no se envió.");
    return null;
  }

  sayInRoom(text) {
    if (this.send({ t: "chat", text })) this.heard({ from: "", text, mine: true, room: true });
  }

  tell(person, text) {
    this.lastTo = { name: person.name, country: person.country };
    if (this.send({ t: "tell", name: person.name, country: person.country, text })) this.heard({ from: person.name, country: person.country, text, mine: true, private: true });
  }

  adjourn() {
    this.send({ t: "adjourn" });
  }

  answerAdjourn(accept) {
    this.adjournAsk = null;
    this.send({ t: "adjournReply", accept });
    this.changed();
  }

  flash(text) {
    this.notice = text;
    clearTimeout(this.noticeTimer);
    this.noticeTimer = setTimeout(() => {
      if (this.notice === text) this.notice = null;
      this.changed();
    }, NOTICE_MS);
    this.changed();
  }

  heard(line) {
    const entry = { ...line, at: Date.now() };
    this.chat = [...this.chat, entry].slice(-CHAT_SIZE);
    recordChat(entry);
    if ((entry.private || entry.room) && !entry.mine) this.inbox = [...this.inbox, entry];
    this.changed();
  }

  closeChat() {
    this.inbox = [];
    this.changed();
  }

  ignoringAll() {
    return load("ignoreAll", false);
  }

  ignoreAll(on) {
    save("ignoreAll", on);
    if (on) this.inbox = [];
    this.changed();
  }

  heardLobby(e) {
    this.lobby = { seeks: e.seeks ?? [], invites: e.invites ?? [], people: e.people ?? [], games: e.games ?? [], adjourned: e.adjourned ?? [] };
    if (this.awaitingRoom && this.seated()) this.ended = GAME_GONE;
    this.awaitingRoom = false;
    if (setting("noChallenges")) this.refuseInvites();
  }

  refuseInvites() {
    this.lobby.invites.filter((s) => !s.resume && s.mode !== "duplicate" && !this.declined.has(s.id)).forEach((s) => {
      this.declined.add(s.id);
      this.roomAction("decline", s.id);
    });
  }

  async handle(e) {
    const handler = HANDLERS[e.t];
    if (handler) await handler(this, e);
    this.changed();
  }

  welcomed() {
    this.refused = null;
    this.connected = true;
    this.awaitingRoom = true;
    if (this.room?.watching) this.send({ t: "watch", code: this.room.id });
    const due = [...(this.link?.takeUnconfirmed() ?? []), ...this.queued.splice(0)];
    new Map(due.map((m) => [JSON.stringify(m), m])).forEach((m) => this.sendWhenOnline(m));
  }

  seatedIn(e) {
    this.awaitingRoom = false;
    if (this.room?.id !== e.code) this.forgetRoom();
    this.room = { id: e.code, duplicate: e.mode === "duplicate", me: e.name, host: e.host, players: e.players ?? [], first: !!e.first, watching: !!e.watching, timed: !e.untimed };
  }

  mirror(e) {
    if (!this.room) return;
    if (this.session) return this.session.state?.result ? null : this.session.replace(e.save);
    this.session = new RemoteSession(e.save, { guest: !e.first, watching: this.room.watching, remote: this });
  }

  closed(reason) {
    this.ended = reason || "La sala se cerró.";
    if (reason === ADJOURNED) this.forgetRoom();
  }

  resign() {
    this.leave();
  }

  ignored() {
    return load("ignored", []);
  }

  ignore(person) {
    save("ignored", [...this.ignored(), key(person)]);
    this.chat = this.chat.filter((l) => !(l.private && key({ name: l.from, country: l.country }) === key(person)));
    this.inbox = this.inbox.filter((l) => key({ name: l.from, country: l.country }) !== key(person));
    this.changed();
  }
}

const HANDLERS = {
  welcome: (c) => c.welcomed(),
  refused: (c, e) => {
    c.refused = e.reason;
    c.link?.close();
    c.link = null;
    c.connected = false;
  },
  error: (c, e) => (c.room ? c.flash(e.reason) : (c.ended = e.reason)),
  lobby: (c, e) => c.heardLobby(e),
  room: (c, e) => c.seatedIn(e),
  state: (c, e) => c.mirror(e),
  rejected: (c, e) => c.flash(e.reason),
  rival: (c, e) => {
    c.rivalOnline = !!e.online;
    c.rivalAwaySince = e.online ? null : Date.now() - (e.awayMs ?? 0);
  },
  adjournAsk: (c, e) => (c.adjournAsk = e.name),
  closed: (c, e) => c.closed(e.reason),
  chat: (c, e) => !c.ignoringAll() && c.heard({ from: e.name, text: e.text, room: true }),
  tell: (c, e) => !c.ignoringAll() && !c.ignored().includes(key(e)) && c.heard({ from: e.name, country: e.country, text: e.text, private: true }),
  watchers: (c, e) => (c.watchers = e.people ?? []),
  table: (c, e) => c.room && (c.table = readTable(e.view, c.room.watching ? "" : c.room.me)),
  duplicateOffer: (c, e) => (c.offer = e.code),
  involved: () => involvedDialog(),
};

function key(p) {
  return (p.name ?? "").toLowerCase() + "|" + (p.country ?? "");
}

function proposalFields(p) {
  if (p.duplicate) return { mode: "duplicate", turnMs: p.turnMs, rounds: p.rounds, live: p.live };
  return { mode: "classic", timeMs: p.timeMs, overtimeMs: p.overtimeMs, void: !p.single, untimed: !p.timed };
}

export const online = new OnlineClient();
