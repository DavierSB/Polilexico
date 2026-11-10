"use strict";

(() => {
  const S_IFDIR = 0o040000;
  const S_IFREG = 0o100000;
  const flags = { O_WRONLY: 1, O_RDWR: 2, O_CREAT: 64, O_EXCL: 128, O_TRUNC: 512, O_APPEND: 1024 };
  const files = new Map();
  const dirs = new Set(["/"]);
  const handles = new Map();
  let nextHandle = 10;
  let consoleLine = "";
  const decoder = new TextDecoder();

  const failure = (code) => Object.assign(new Error(code), { code });

  const normalize = (path) => "/" + path.split("/").filter((p) => p && p !== ".").join("/");

  const parent = (path) => normalize(path.slice(0, path.lastIndexOf("/")));

  const isDir = (path) => dirs.has(normalize(path));

  const addDirs = (path) => {
    for (let p = parent(path); !dirs.has(p); p = parent(p)) dirs.add(p);
  };

  const put = (path, bytes) => {
    const p = normalize(path);
    addDirs(p);
    files.set(p, { data: bytes, size: bytes.length, mtime: Date.now() });
  };

  const stat = (path) => {
    const p = normalize(path);
    if (dirs.has(p)) return statOf(S_IFDIR | 0o755, 0, Date.now());
    const f = files.get(p);
    if (!f) throw failure("ENOENT");
    return statOf(S_IFREG | 0o644, f.size, f.mtime);
  };

  const statOf = (mode, size, mtime) => ({
    dev: 0, ino: 0, mode, nlink: 1, uid: 0, gid: 0, rdev: 0, size, blksize: 4096,
    blocks: Math.ceil(size / 512), atimeMs: mtime, mtimeMs: mtime, ctimeMs: mtime,
    isDirectory: () => (mode & S_IFDIR) === S_IFDIR,
  });

  const openFile = (path, mode) => {
    const p = normalize(path);
    if (dirs.has(p)) return p;
    const exists = files.has(p);
    if (exists && mode & flags.O_EXCL && mode & flags.O_CREAT) throw failure("EEXIST");
    if (!exists && !(mode & flags.O_CREAT)) throw failure("ENOENT");
    if (!isDir(parent(p))) throw failure("ENOENT");
    if (!exists || mode & flags.O_TRUNC) put(p, new Uint8Array(0));
    return p;
  };

  const handle = (fd) => {
    const h = handles.get(fd);
    if (!h) throw failure("EBADF");
    return h;
  };

  const readAt = (h, buf, offset, length, position) => {
    const f = files.get(h.path);
    if (!f) throw failure("EISDIR");
    const from = position ?? h.pos;
    const n = Math.max(0, Math.min(length, f.size - from));
    buf.set(f.data.subarray(from, from + n), offset);
    if (position === null || position === undefined) h.pos += n;
    return n;
  };

  const grow = (f, size) => {
    if (size <= f.data.length) return;
    const bigger = new Uint8Array(Math.max(size, f.data.length * 2));
    bigger.set(f.data.subarray(0, f.size));
    f.data = bigger;
  };

  const writeAt = (h, buf, offset, length, position) => {
    const f = files.get(h.path);
    const from = position ?? (h.append ? f.size : h.pos);
    grow(f, from + length);
    f.data.set(buf.subarray(offset, offset + length), from);
    f.size = Math.max(f.size, from + length);
    f.mtime = Date.now();
    if (position === null || position === undefined) h.pos = from + length;
    return length;
  };

  const toConsole = (buf) => {
    consoleLine += decoder.decode(buf);
    const nl = consoleLine.lastIndexOf("\n");
    if (nl === -1) return;
    console.log(consoleLine.substring(0, nl));
    consoleLine = consoleLine.substring(nl + 1);
  };

  const listDir = (path) => {
    const p = normalize(path);
    if (!dirs.has(p)) throw failure("ENOTDIR");
    const prefix = p === "/" ? "/" : p + "/";
    const names = [...files.keys(), ...dirs].filter((k) => k.startsWith(prefix) && k !== p);
    return [...new Set(names.map((k) => k.slice(prefix.length).split("/")[0]))];
  };

  const run = (callback, work) => {
    let result;
    try {
      result = work();
    } catch (e) {
      callback(e.code ? e : failure("EIO"));
      return;
    }
    callback(null, result);
  };

  globalThis.memfs = { put, read: (path) => files.get(normalize(path))?.data.slice(0, files.get(normalize(path)).size) };

  globalThis.fs = {
    constants: flags,
    writeSync(fd, buf) {
      if (fd === 1 || fd === 2) toConsole(buf);
      else writeAt(handle(fd), buf, 0, buf.length, null);
      return buf.length;
    },
    write(fd, buf, offset, length, position, cb) {
      run(cb, () => (fd === 1 || fd === 2 ? (toConsole(buf.subarray(offset, offset + length)), length) : writeAt(handle(fd), buf, offset, length, position)));
    },
    read(fd, buf, offset, length, position, cb) {
      run(cb, () => readAt(handle(fd), buf, offset, length, position));
    },
    open(path, mode, perm, cb) {
      run(cb, () => {
        const p = openFile(path, mode);
        const fd = nextHandle++;
        handles.set(fd, { path: p, pos: 0, append: !!(mode & flags.O_APPEND) });
        return fd;
      });
    },
    close(fd, cb) { run(cb, () => { handles.delete(fd); return null; }); },
    fstat(fd, cb) { run(cb, () => stat(handle(fd).path)); },
    stat(path, cb) { run(cb, () => stat(path)); },
    lstat(path, cb) { run(cb, () => stat(path)); },
    readdir(path, cb) { run(cb, () => listDir(path)); },
    mkdir(path, perm, cb) {
      run(cb, () => {
        const p = normalize(path);
        if (dirs.has(p) || files.has(p)) throw failure("EEXIST");
        if (!isDir(parent(p))) throw failure("ENOENT");
        dirs.add(p);
        return null;
      });
    },
    unlink(path, cb) {
      run(cb, () => {
        if (!files.delete(normalize(path))) throw failure("ENOENT");
        return null;
      });
    },
    rmdir(path, cb) { run(cb, () => { dirs.delete(normalize(path)); return null; }); },
    rename(from, to, cb) {
      run(cb, () => {
        const f = files.get(normalize(from));
        if (!f) throw failure("ENOENT");
        files.delete(normalize(from));
        addDirs(normalize(to));
        files.set(normalize(to), f);
        return null;
      });
    },
    fsync(fd, cb) { run(cb, () => null); },
    ftruncate(fd, length, cb) {
      run(cb, () => {
        const f = files.get(handle(fd).path);
        f.size = Math.min(f.size, length);
        return null;
      });
    },
    chmod(path, mode, cb) { run(cb, () => null); },
    fchmod(fd, mode, cb) { run(cb, () => null); },
    chown(path, uid, gid, cb) { run(cb, () => null); },
    fchown(fd, uid, gid, cb) { run(cb, () => null); },
    lchown(path, uid, gid, cb) { run(cb, () => null); },
    utimes(path, atime, mtime, cb) { run(cb, () => null); },
    truncate(path, length, cb) { run(cb, () => null); },
    readlink(path, cb) { cb(failure("EINVAL")); },
    link(path, link, cb) { cb(failure("ENOSYS")); },
    symlink(path, link, cb) { cb(failure("ENOSYS")); },
  };

  globalThis.process = {
    getuid: () => -1, getgid: () => -1, geteuid: () => -1, getegid: () => -1,
    getgroups() { throw failure("ENOSYS"); },
    pid: -1, ppid: -1,
    umask() { throw failure("ENOSYS"); },
    cwd: () => "/",
    chdir() { throw failure("ENOSYS"); },
  };
})();
