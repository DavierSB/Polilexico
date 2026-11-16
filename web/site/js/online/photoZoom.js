import { h } from "../dom.js";
import { flag } from "../parts.js";

export function zoomable(img, name, country) {
  img.classList.add("zoomable");
  img.addEventListener("click", () => img.naturalWidth && zoomPhoto(img.src, name, country));
  return img;
}

function zoomPhoto(src, name, country) {
  const view = h("div.scrim.photo-zoom", { role: "dialog", "aria-modal": "true" },
    h("img.photo-zoom-img", { src, alt: name }),
    h("p.photo-zoom-name", {}, `${name}  ${flag(country)}`));
  const onKey = (e) => e.key === "Escape" && (e.stopPropagation(), close());
  const close = () => (view.remove(), document.removeEventListener("keydown", onKey, true));
  view.addEventListener("click", close);
  document.addEventListener("keydown", onKey, true);
  document.getElementById("layer").append(view);
}
