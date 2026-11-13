const LITERAL_BITS = 8;
const CLEAR = 1 << LITERAL_BITS;
const END = CLEAR + 1;
const MAX_CODE = (1 << 12) - 1;
const BLOCK = 255;
const PALETTE_SIZE = 256;
const BUCKETS = 1 << 15;

export function encodeGif(width, height, frames) {
  const palette = paletteOf(frames.map((f) => f.pixels));
  const out = new Bytes();
  header(out, width, height, palette);
  let previous = null;
  frames.forEach((f) => {
    const indices = palette.indices(f.pixels);
    frame(out, width, height, indices, previous, f.delayCs);
    previous = indices;
  });
  out.byte(0x3b);
  return new Blob([out.result()], { type: "image/gif" });
}

class Bytes {
  constructor() {
    this.parts = [];
    this.buffer = new Uint8Array(1 << 16);
    this.size = 0;
  }

  byte(b) {
    if (this.size === this.buffer.length) this.flush();
    this.buffer[this.size++] = b;
  }

  bytes(list) {
    list.forEach((b) => this.byte(b));
  }

  short(n) {
    this.byte(n & 0xff);
    this.byte((n >> 8) & 0xff);
  }

  flush() {
    this.parts.push(this.buffer.slice(0, this.size));
    this.size = 0;
  }

  result() {
    this.flush();
    return new Blob(this.parts);
  }
}

function header(out, width, height, palette) {
  out.bytes([..."GIF89a"].map((c) => c.charCodeAt(0)));
  out.short(width);
  out.short(height);
  out.bytes([0xf7, 0, 0]);
  out.bytes(palette.rgb());
  out.bytes([0x21, 0xff, 0x0b]);
  out.bytes([..."NETSCAPE2.0"].map((c) => c.charCodeAt(0)));
  out.bytes([3, 1, 0, 0, 0]);
}

function frame(out, width, height, indices, previous, delayCs) {
  const area = changedArea(width, height, indices, previous);
  out.bytes([0x21, 0xf9, 4, 4]);
  out.short(delayCs);
  out.bytes([0, 0]);
  out.byte(0x2c);
  [area.x, area.y, area.width, area.height].forEach((n) => out.short(n));
  out.byte(0);
  lzw(out, crop(width, indices, area));
}

function changedArea(width, height, indices, previous) {
  if (!previous) return { x: 0, y: 0, width, height };
  let top = height, bottom = -1, left = width, right = -1;
  for (let y = 0; y < height; y++) {
    for (let x = 0; x < width; x++) {
      const i = y * width + x;
      if (indices[i] === previous[i]) continue;
      if (y < top) top = y;
      if (y > bottom) bottom = y;
      if (x < left) left = x;
      if (x > right) right = x;
    }
  }
  if (bottom < 0) return { x: 0, y: 0, width: 1, height: 1 };
  return { x: left, y: top, width: right - left + 1, height: bottom - top + 1 };
}

function crop(width, indices, a) {
  const out = new Uint8Array(a.width * a.height);
  for (let y = 0; y < a.height; y++) out.set(indices.subarray((a.y + y) * width + a.x, (a.y + y) * width + a.x + a.width), y * a.width);
  return out;
}

function lzw(out, indices) {
  const s = { table: new Map(), block: new Uint8Array(BLOCK), blockSize: 0, bits: 0, bitCount: 0, width: LITERAL_BITS + 1, hi: END, overflow: CLEAR << 1 };
  const put = (b) => {
    s.block[s.blockSize++] = b;
    if (s.blockSize === BLOCK) writeBlock();
  };
  const writeBlock = () => {
    out.byte(s.blockSize);
    for (let i = 0; i < s.blockSize; i++) out.byte(s.block[i]);
    s.blockSize = 0;
  };
  const emit = (code) => {
    s.bits |= code << s.bitCount;
    s.bitCount += s.width;
    while (s.bitCount >= 8) {
      put(s.bits & 0xff);
      s.bits >>>= 8;
      s.bitCount -= 8;
    }
  };
  const reset = () => Object.assign(s, { width: LITERAL_BITS + 1, hi: END, overflow: CLEAR << 1 }, s.table.clear());
  const nextCode = () => {
    s.hi++;
    if (s.hi === s.overflow) {
      s.width++;
      s.overflow <<= 1;
    }
    if (s.hi < MAX_CODE) return true;
    emit(CLEAR);
    reset();
    return false;
  };
  out.byte(LITERAL_BITS);
  emit(CLEAR);
  let code = indices[0];
  for (let i = 1; i < indices.length; i++) {
    const literal = indices[i];
    const key = (code << 8) | literal;
    const known = s.table.get(key);
    if (known !== undefined) {
      code = known;
      continue;
    }
    emit(code);
    if (nextCode()) s.table.set(key, s.hi);
    code = literal;
  }
  emit(code);
  nextCode();
  emit(END);
  if (s.bitCount > 0) put(s.bits & 0xff);
  if (s.blockSize > 0) writeBlock();
  out.byte(0);
}

function bucket(r, g, b) {
  return ((r >> 3) << 10) | ((g >> 3) << 5) | (b >> 3);
}

function paletteOf(frames) {
  const counts = new Uint32Array(BUCKETS);
  const sums = new Float64Array(BUCKETS * 3);
  frames.forEach((px) => {
    for (let i = 0; i < px.length; i += 4) {
      const k = bucket(px[i], px[i + 1], px[i + 2]);
      counts[k]++;
      sums[k * 3] += px[i];
      sums[k * 3 + 1] += px[i + 1];
      sums[k * 3 + 2] += px[i + 2];
    }
  });
  const top = [...counts.keys()].filter((k) => counts[k] > 0).sort((a, b) => counts[b] - counts[a]).slice(0, PALETTE_SIZE);
  const colors = top.map((k) => [0, 1, 2].map((c) => Math.round(sums[k * 3 + c] / counts[k])));
  const lookup = new Int16Array(BUCKETS).fill(-1);
  const nearest = (r, g, b) => {
    let best = 0, distance = Infinity;
    colors.forEach(([cr, cg, cb], i) => {
      const d = (cr - r) ** 2 + (cg - g) ** 2 + (cb - b) ** 2;
      if (d < distance) (distance = d), (best = i);
    });
    return best;
  };
  return {
    rgb: () => Array.from({ length: PALETTE_SIZE * 3 }, (_, i) => colors[Math.floor(i / 3)]?.[i % 3] ?? 0),
    indices: (px) => {
      const out = new Uint8Array(px.length / 4);
      for (let i = 0, j = 0; i < px.length; i += 4, j++) {
        const k = bucket(px[i], px[i + 1], px[i + 2]);
        if (lookup[k] < 0) lookup[k] = nearest(px[i], px[i + 1], px[i + 2]);
        out[j] = lookup[k];
      }
      return out;
    },
  };
}
