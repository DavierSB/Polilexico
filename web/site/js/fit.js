const MIN_SCALE = 0.6;
const STEP = 0.01;
const LAPTOP = "(min-width: 900px) and (min-aspect-ratio: 1/1)";

export function fitAround(root, board) {
  return watch(root, () => {
    const parts = [...root.children].filter((el) => el !== board && el.offsetParent !== null);
    const natural = naturalHeight(parts);
    const style = getComputedStyle(root);
    const gaps = (parseFloat(style.rowGap) || 0) * parts.length;
    const room = window.innerHeight - board.getBoundingClientRect().height - padding(style) - gaps;
    zoomAll(parts, scaleFor(room, natural));
  });
}

export function fitColumn(root) {
  return watch(root, () => {
    const style = getComputedStyle(root);
    const natural = naturalHeight([...root.children]) + (parseFloat(style.rowGap) || 0) * Math.max(0, root.children.length - 1);
    zoomAll([...root.children], scaleFor(window.innerHeight - padding(style) - root.getBoundingClientRect().top - window.scrollY, natural));
  });
}

function watch(root, fit) {
  let frame = 0;
  const schedule = () => (cancelAnimationFrame(frame), (frame = requestAnimationFrame(run)));
  const run = () => (matchMedia(LAPTOP).matches ? zoomAll([...root.children], 1) : fit());
  const resize = new ResizeObserver(schedule);
  const mutate = new MutationObserver(() => [...root.children].forEach((el) => resize.observe(el)));
  [...root.children].forEach((el) => resize.observe(el));
  mutate.observe(root, { childList: true });
  window.addEventListener("resize", schedule);
  schedule();
  return () => (resize.disconnect(), mutate.disconnect(), window.removeEventListener("resize", schedule), cancelAnimationFrame(frame));
}

function naturalHeight(parts) {
  const zooms = parts.map((el) => el.style.zoom);
  parts.forEach((el) => (el.style.zoom = ""));
  const total = parts.reduce((t, el) => t + el.getBoundingClientRect().height, 0);
  parts.forEach((el, i) => (el.style.zoom = zooms[i]));
  return total;
}

function padding(style) {
  return (parseFloat(style.paddingTop) || 0) + (parseFloat(style.paddingBottom) || 0);
}

function scaleFor(room, natural) {
  if (natural <= 0) return 1;
  return Math.min(1, Math.max(MIN_SCALE, room / natural));
}

function zoomAll(parts, scale) {
  parts.forEach((el) => {
    const now = parseFloat(el.style.zoom) || 1;
    if (Math.abs(now - scale) > STEP) el.style.zoom = scale === 1 ? "" : String(scale);
  });
}
