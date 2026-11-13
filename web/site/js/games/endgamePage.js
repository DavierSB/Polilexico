import { h, replace } from "../dom.js";
import { call } from "../engine.js";
import { openDialog, actions } from "../dialogs.js";
import { tile } from "../board.js";
import { Placer } from "../classic/placer.js";
import { createRack } from "../classic/rack.js";
import { askBlank } from "../classic/moves.js";
import { dragFrom } from "../classic/drag.js";
import { lookUp } from "../study/lookup.js";
import { Exploration } from "./explore.js";
import { planDialog, endingLine, outcomeLine } from "./planDialog.js";
import { scoreBar, turnTitle, valueHead, FINAL_COLUMN } from "./reviewParts.js";

const NOT_IN_A_LINE = "Las fichas tienen que formar una sola línea continua.";

export function endgamePage(ui, index, players, me) {
  const page = new EndgamePage(ui, index, players, me);
  return page;
}

class EndgamePage {
  constructor(ui, index, players, me) {
    this.ui = ui;
    this.index = index;
    this.turn = ui.game.Turns[index];
    this.placer = new Placer(() => this.placerChanged());
    this.placer.askBlank = askBlank;
    this.rack = createRack(this.placer);
    this.x = new Exploration(() => call(ui.explore.open, ...ui.explore.args(index)), players, me, () => this.positionChanged());
    this.onBoardDown = (e) => this.dragPending(e);
    ui.board.el.addEventListener("pointerdown", this.onBoardDown);
  }

  close() {
    this.x.close();
    this.ui.board.el.removeEventListener("pointerdown", this.onBoardDown);
  }

  canPlay() {
    return !!this.x.position && !this.x.position.over;
  }

  onSquare(i) {
    if (this.canPlay()) return this.placer.clickSquare(i);
    const cells = this.x.position?.cells ?? this.turn.cells;
    if (cells[i]) lookUp(cells, i);
  }

  positionChanged() {
    const p = this.x.position;
    const key = p ? p.path.map((s) => s.move.text).join("|") + "#" + p.racks.join() : "";
    if (p && key !== this.synced) {
      this.synced = key;
      this.placer.sync(p.cells, p.over ? [] : p.racks[p.onTurn]);
    }
    this.draw();
  }

  placerChanged() {
    this.drawBoard();
    this.drawBottom();
  }

  draw() {
    this.drawTop();
    this.drawBoard();
    this.drawBottom();
  }

  drawTop() {
    const p = this.x.position;
    replace(this.ui.top, this.title(), p ? this.sideBar(1 - p.me) : null);
  }

  title() {
    const path = this.x.position?.path ?? [];
    const nav = (symbol, enabled, step) => h("button.btn.small.outlined.nav-btn", { disabled: !enabled, onclick: () => this.ui.goTo(this.index + step) }, symbol);
    return h("div.row.explore-title", {},
      nav("‹", this.index > 0, -1),
      h("div.grow.explore-crumbs", {}, path.length ? this.breadcrumb(path) : turnTitle(this.turn, this.ui.game.Turns.length)),
      path.length ? h("button.text-btn.compact", { onclick: () => this.x.back(1) }, "Deshacer") : null,
      nav("›", this.index < this.ui.game.Turns.length - 1, 1),
    );
  }

  breadcrumb(path) {
    const chip = (text, active, back) => h("button", { class: "path-chip" + (active ? " on" : ""), onclick: () => back && this.x.back(back) }, text);
    const row = h("div.row.path-chips", {},
      chip(`Turno ${this.turn.Number}`, false, path.length),
      path.map((s, i) => chip(`${s.move.text} ${s.move.score}`, i === path.length - 1, path.length - 1 - i)),
    );
    requestAnimationFrame(() => (row.scrollLeft = row.scrollWidth));
    return row;
  }

  sideBar(player) {
    const p = this.x.position;
    const onTurn = !p.over && p.onTurn === player;
    return scoreBar(this.x.players[player], p.scores[player], onTurn ? null : p.racks[player], onTurn);
  }

  drawBoard() {
    const p = this.x.position;
    if (!p) return this.ui.board.render({ cells: this.turn.cells });
    const pending = new Map([...this.placer.pending].map(([i, x]) => [i, x.tile]));
    this.ui.board.render({ cells: p.cells, pending, arrow: this.canPlay() ? this.placer.arrow : null });
  }

  drawBottom() {
    const p = this.x.position;
    if (!p) return replace(this.ui.bottom, this.linesTable([]));
    replace(this.ui.bottom,
      this.sideBar(p.me),
      p.over ? null : this.turnRack(p),
      this.x.notice ? h("p.notice-line", {}, this.x.notice) : null,
      p.over ? null : this.buttons(),
      p.over ? endgameResult(p, this.x.players) : this.linesTable(this.summary()),
    );
  }

  turnRack(p) {
    this.rack.render(true);
    const caption = p.onTurn !== p.me ? h("span.review-caption", {}, `Juega ${this.x.players[p.onTurn]}`) : null;
    return [caption, h("div.review-rack", {}, this.rack.el)];
  }

  buttons() {
    const c = this.placer;
    const ready = !!this.x.lines?.lines.length;
    return h("div.row.explore-buttons", {},
      h("button.btn.small", { disabled: !c.pending.size, onclick: () => this.play() }, "Jugar"),
      c.pending.size ? h("button.btn.small.tonal", { onclick: () => c.recall() }, "Recoger") : h("button.btn.small.tonal", { onclick: () => c.shuffle() }, "Mezclar"),
      h("button.btn.small.outlined", { onclick: () => this.x.play(null) }, "Pasar"),
      h("button.btn.small.outlined", { disabled: !ready, onclick: () => this.plan(this.x.lines.lines[0]) }, "Ver plan"),
      h("button.btn.small.outlined", { disabled: !ready, onclick: () => this.listDialog() }, "Ver lista"),
    );
  }

  play() {
    const input = this.placer.placementInput();
    if (!input) return this.x.flash(NOT_IN_A_LINE);
    this.x.play(input);
  }

  summary() {
    const lines = this.marked();
    const best = lines[0] ? [{ caption: "Mejor jugada", rank: rankOf(lines, 0), line: lines[0] }] : [];
    const marks = (this.x.position.path.length ? [] : this.turn.Marks ?? []).map((m) => {
      const i = lines.findIndex((l) => l.move.text === this.markText(m));
      return i < 0 ? null : { caption: markCaption(m.Who, this.ui), rank: rankOf(lines, i), line: lines[i] };
    }).filter(Boolean);
    return [...best, ...marks];
  }

  markText(m) {
    return this.ui.moveOf(this.turn, m).text;
  }

  marked() {
    const lines = this.x.lines?.lines ?? [];
    if (this.x.position.path.length) return lines;
    return lines.map((l) => ({ ...l, move: { ...l.move, playedBy: (this.turn.Marks ?? []).filter((m) => this.markText(m) === l.move.text).map((m) => this.ui.nameOf(m.Who)).join(" ") } }));
  }

  linesTable(rows, scroll = false) {
    return h("div.review-table", {},
      h("div.review-row.review-header", {},
        h("span.review-move-cell.row", {}, h("span", {}, "Jugada"), this.x.analyzing ? h("div.spinner.tiny") : null, h("span.lines-status", {}, statusText(this.x))),
        h("span.review-cell.rank-cell", {}, "#"), h("span.review-cell", {}, "Puntos"), valueHead(FINAL_COLUMN)),
      h(scroll ? "div.lines-scroll" : "div", {}, rows.map((r) => this.lineRow(r))),
    );
  }

  lineRow(r, close = null) {
    const m = r.line.move;
    return h("button.review-row.review-line", { onclick: () => (close?.(), this.plan(r.line)) },
      h("span.review-move-cell", {}, r.caption ? h("span.review-caption", {}, r.caption) : null, h("b.review-move", {}, m.text)),
      h("span.review-cell.rank-cell.muted", {}, r.rank),
      h("b.review-cell", {}, m.score),
      h("span.review-cell.muted", {}, r.line.pending ? "…" : signed(r.line.spread)),
    );
  }

  listDialog() {
    const lines = this.marked();
    openDialog((close) => [
      h("h2", {}, "Mejores jugadas"),
      h("div.review-table.moves-scroll", {}, lines.map((l, i) => this.lineRow({ caption: l.move.playedBy ? markCaption(l.move.playedBy, this.ui) : "", rank: rankOf(lines, i), line: l }, close))),
      actions(h("button.text-btn", { onclick: close }, "Cerrar")),
    ], { wide: true });
  }

  plan(line) {
    if (!line) return;
    const fresh = () => this.x.lines?.lines.find((l) => l.move.text === line.move.text) ?? line;
    planDialog(this.x.position, this.x.players, fresh);
  }

  dragPending(e) {
    const el = e.target.closest(".tile.pending");
    if (!el || e.button !== 0 || !this.canPlay()) return;
    const from = Number(el.closest(".square").dataset.index);
    const t = this.placer.pending.get(from)?.tile;
    dragFrom(el, e, () => tile(t, "pending"), (target) => {
      if (target.square) return this.placer.move(from, Number(target.square.dataset.index));
      if (target.rack) this.placer.take(from);
    });
  }
}

function endgameResult(p, players) {
  const margin = p.scores[p.me] - p.scores[1 - p.me];
  return h("div.stack.endgame-result", {},
    h("span.review-header-text", {}, "Fin de la partida"),
    p.ending.map(endingLine),
    outcomeLine(margin > 0 ? players[p.me] : players[1 - p.me], Math.abs(margin), players[p.me]),
  );
}

function rankOf(lines, i) {
  return lines[i].pending ? "—" : `#${i + 1}`;
}

function markCaption(who, ui) {
  return who === "Tú" || who === ui.me ? "Tu jugada" : `Jugada de ${ui.nameOf(who)}`;
}

function statusText(x) {
  if (x.failed) return "sin análisis";
  if (!x.lines?.depth) return "";
  if (x.lines.exact && !x.analyzing) return "exacto";
  return `prof. ${x.lines.depth}`;
}

function signed(n) {
  return (n > 0 ? "+" : "") + n;
}
