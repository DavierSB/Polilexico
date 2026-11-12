const THRESHOLD = 6;

export function draggable(el, { ghost, drop, canStart = () => true }) {
  el.style.touchAction = "none";
  el.addEventListener("pointerdown", (e) => {
    if (e.button !== 0 || !canStart()) return;
    dragFrom(el, e, ghost, drop);
  });
}

export function dragFrom(el, down, ghost, drop) {
  let floating = null;
  const move = (e) => {
    if (!floating && distance(down, e) < THRESHOLD) return;
    floating ??= lift(el, ghost());
    place(floating, e);
    highlight(e);
  };
  const up = (e) => {
    window.removeEventListener("pointermove", move);
    window.removeEventListener("pointerup", up);
    window.removeEventListener("pointercancel", up);
    if (!floating) return;
    land(el, floating);
    suppressClick(el);
    drop(targetAt(e));
  };
  window.addEventListener("pointermove", move);
  window.addEventListener("pointerup", up);
  window.addEventListener("pointercancel", up);
}

function distance(a, b) {
  return Math.hypot(a.clientX - b.clientX, a.clientY - b.clientY);
}

function lift(el, ghost) {
  const box = el.getBoundingClientRect();
  ghost.classList.add("floating-tile");
  ghost.style.width = box.width + "px";
  ghost.style.height = box.height + "px";
  document.body.append(ghost);
  el.classList.add("dragging");
  return ghost;
}

function place(ghost, e) {
  ghost.style.left = e.clientX - ghost.offsetWidth / 2 + "px";
  ghost.style.top = e.clientY - ghost.offsetHeight / 2 + "px";
}

function land(el, ghost) {
  ghost.remove();
  el.classList.remove("dragging");
  document.querySelectorAll(".square.drop").forEach((s) => s.classList.remove("drop"));
}

function highlight(e) {
  const target = targetAt(e);
  document.querySelectorAll(".square.drop").forEach((s) => s !== target.square && s.classList.remove("drop"));
  target.square?.classList.add("drop");
}

function targetAt(e) {
  const under = document.elementFromPoint(e.clientX, e.clientY);
  return {
    square: under?.closest(".square") ?? null,
    slot: under?.closest(".rack-tile") ?? null,
    rack: under?.closest(".rack") ?? null,
    pin: under?.closest(".pin-slot") ?? null,
  };
}

function suppressClick(el) {
  const stop = (e) => (e.stopPropagation(), e.preventDefault());
  el.addEventListener("click", stop, { capture: true, once: true });
  setTimeout(() => el.removeEventListener("click", stop, { capture: true }), 0);
}
