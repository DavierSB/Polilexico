export class SortedLines {
  constructor(text) {
    this.lines = text.split("\n");
    if (this.lines.at(-1) === "") this.lines.pop();
  }

  find(key) {
    let low = 0;
    let high = this.lines.length - 1;
    while (low <= high) {
      const mid = (low + high) >>> 1;
      const line = this.lines[mid];
      const tab = line.indexOf("\t");
      const k = tab < 0 ? line : line.slice(0, tab);
      if (k === key) return line;
      if (k < key) low = mid + 1;
      else high = mid - 1;
    }
    return null;
  }
}
