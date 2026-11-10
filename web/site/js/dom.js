export function h(tag, attrs = {}, ...children) {
  const [name, ...classes] = tag.split(".");
  const el = document.createElement(name || "div");
  if (classes.length) el.className = classes.join(" ");
  Object.entries(attrs ?? {}).forEach(([k, v]) => setAttr(el, k, v));
  append(el, children);
  return el;
}

export function svg(markup) {
  const holder = document.createElement("span");
  holder.innerHTML = markup.trim();
  return holder.firstChild;
}

export function append(el, children) {
  children.flat(Infinity).forEach((c) => {
    if (c === null || c === undefined || c === false) return;
    el.append(c instanceof Node ? c : document.createTextNode(String(c)));
  });
  return el;
}

export function replace(el, ...children) {
  el.replaceChildren();
  return append(el, children);
}

function setAttr(el, key, value) {
  if (value === null || value === undefined || value === false) return;
  if (key.startsWith("on")) return el.addEventListener(key.slice(2).toLowerCase(), value);
  if (key === "class") return el.classList.add(...String(value).split(" ").filter(Boolean));
  if (key === "style" && typeof value === "object") return Object.assign(el.style, value);
  if (key in el && typeof value !== "string") return (el[key] = value);
  el.setAttribute(key, value === true ? "" : value);
}
