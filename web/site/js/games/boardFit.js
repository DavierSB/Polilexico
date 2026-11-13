const MIN_BOARD = 260;
const LAPTOP = "(min-width: 900px) and (min-aspect-ratio: 1/1)";

export function fitBoard(root, board, above, below) {
  const fit = () => {
    if (matchMedia(LAPTOP).matches) return (board.style.width = "");
    const width = board.parentElement.clientWidth;
    const used = above.reduce((t, el) => t + el.offsetHeight, 0) + below.reduce((t, el) => t + el.offsetHeight, 0);
    const rest = window.innerHeight - used - chrome(root);
    board.style.width = Math.min(width, Math.max(Math.min(MIN_BOARD, width), rest)) + "px";
  };
  const observer = new ResizeObserver(() => requestAnimationFrame(fit));
  [...above, ...below].forEach((el) => observer.observe(el));
  window.addEventListener("resize", fit);
  fit();
  return () => (observer.disconnect(), window.removeEventListener("resize", fit));
}

function chrome(root) {
  const style = getComputedStyle(root);
  const gaps = parseFloat(style.rowGap) || 0;
  return parseFloat(style.paddingTop) + parseFloat(style.paddingBottom) + gaps * 2 + 8;
}
