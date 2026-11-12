import { load, save } from "../store.js";

const KEEP = 300;

export function chatRecords() {
  return load("chatLog", []);
}

export function recordChat(line) {
  const entry = { at: line.at, who: line.from || "", country: line.country ?? "", text: line.text, mine: !!line.mine, room: !line.private };
  save("chatLog", [...chatRecords(), entry].slice(-KEEP));
}
