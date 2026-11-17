const ROW_CAPACITY = 5;

export function layoutDots(xs, values, frame) {
  const groups = new DotGroups(xs, frame);
  const placed = groups.all.map((g) => groups.place(g));
  const spots = placed.flat().sort((a, b) => a[0] - b[0]).map((p) => p[1]);
  const labels = groups.all.map((g, k) => [g, placed[k]]).filter(([g]) => g.length > 1).map(([g, p]) => labelOf(g, p, values, frame));
  return { spots, labels };
}

function labelOf(group, placed, values, frame) {
  const xs = placed.map((p) => p[1].x);
  const vs = group.map((i) => values[i]);
  const top = Math.min(...placed.map((p) => p[1].y));
  return { low: Math.min(...vs), high: Math.max(...vs), x: (Math.min(...xs) + Math.max(...xs)) / 2, bottom: top - frame.step / 2 };
}

class DotGroups {
  constructor(xs, frame) {
    this.xs = xs;
    this.frame = frame;
    this.maxRows = Math.max(1, Math.trunc((frame.bottom - frame.labelHeight) / frame.step));
    this.all = this.merge(xs.map((_, i) => i).sort((a, b) => xs[a] - xs[b] || a - b).map((i) => [i]));
  }

  place(group) {
    const centre = this.clampedCentre(group);
    return group.map((i, k) => [i, this.spot(k, group.length, centre)]);
  }

  merge(groups) {
    const merged = groups.reduce((acc, g) => this.join(acc, g), []);
    return merged.length === groups.length ? merged : this.merge(merged);
  }

  join(acc, group) {
    const last = acc[acc.length - 1];
    if (last && this.touches(last, group)) acc[acc.length - 1] = last.concat(group);
    else acc.push(group);
    return acc;
  }

  touches(a, b) {
    return this.centre(b) - this.halfWidth(b.length) - (this.centre(a) + this.halfWidth(a.length)) < this.frame.step;
  }

  spot(k, n, centre) {
    const per = this.perRow(n);
    const row = Math.trunc(k / per);
    const inRow = Math.min(per, n - row * per);
    return { x: centre + ((k % per) - (inRow - 1) / 2) * this.hStep(n), y: this.frame.bottom - this.frame.step / 2 - row * this.frame.step };
  }

  clampedCentre(group) {
    const { left, right, step } = this.frame;
    const half = this.halfWidth(group.length);
    const low = left + step / 2 + half;
    const high = right - step / 2 - half;
    return low > high ? (left + right) / 2 : Math.min(Math.max(this.centre(group), low), high);
  }

  centre(group) {
    return (this.xs[group[0]] + this.xs[group[group.length - 1]]) / 2;
  }

  halfWidth(n) {
    return ((Math.min(n, this.perRow(n)) - 1) / 2) * this.hStep(n);
  }

  perRow(n) {
    return Math.max(ROW_CAPACITY, Math.ceil(n / this.maxRows));
  }

  hStep(n) {
    const { left, right, step } = this.frame;
    return Math.min(step, (right - left - step) / Math.max(1, this.perRow(n) - 1));
  }
}
