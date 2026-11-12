const COLORS = ["#66BB6A", "#FFCA28", "#42A5F5", "#EC407A", "#FF7043", "#AB47BC"];
const PIECES = 110;
const DURATION_MS = 3600;

export function confetti() {
  const canvas = document.createElement("canvas");
  canvas.className = "confetti";
  document.body.append(canvas);
  const pieces = Array.from({ length: PIECES }, randomPiece);
  const started = performance.now();
  const frame = (now) => {
    const t = Math.min((now - started) / DURATION_MS, 1);
    paint(canvas, pieces, t);
    if (t < 1) requestAnimationFrame(frame);
    else canvas.remove();
  };
  requestAnimationFrame(frame);
}

function randomPiece() {
  return {
    x: Math.random(), delay: Math.random() * 0.45, speed: 0.9 + Math.random() * 0.7, sway: 6 + Math.random() * 14,
    phase: Math.random() * 6.3, spin: 200 + Math.random() * 700, width: 6 + Math.random() * 6,
    color: COLORS[Math.floor(Math.random() * COLORS.length)],
  };
}

function paint(canvas, pieces, t) {
  const ratio = window.devicePixelRatio || 1;
  canvas.width = innerWidth * ratio;
  canvas.height = innerHeight * ratio;
  const ctx = canvas.getContext("2d");
  ctx.scale(ratio, ratio);
  ctx.globalAlpha = t < 0.8 ? 1 : (1 - t) / 0.2;
  pieces.forEach((p) => piece(ctx, p, t));
}

function piece(ctx, p, t) {
  const w = p.width;
  const y = innerHeight * ((t - p.delay) * p.speed * 1.4) - w * 2;
  if (y < -w * 2 || y > innerHeight) return;
  const x = innerWidth * p.x + Math.sin(t * 9 + p.phase) * p.sway;
  ctx.save();
  ctx.translate(x, y);
  ctx.rotate((t * p.spin * Math.PI) / 180);
  ctx.fillStyle = p.color;
  ctx.fillRect(-w / 2, -w / 4, w, w / 2);
  ctx.restore();
}
