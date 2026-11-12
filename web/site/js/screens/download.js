import { h } from "../dom.js";
import { openDialog } from "../dialogs.js";

export const DOWNLOAD_URL = "https://polilexico.github.io/download";

const MOBILE = /Android|iPhone|iPad|iPod|Mobile/i;

export function offerDownload() {
  if (!MOBILE.test(navigator.userAgent)) return;
  openDialog((close) => [
    h("div.download-offer", {},
      h("img", { src: "img/logo.svg", alt: "", width: 72, height: 62 }),
      h("h2", {}, "Descarga Poliléxico para Android"),
      h("a.btn.block.download-btn", { href: DOWNLOAD_URL }, "Descargar"),
      h("button.btn.block.outlined", { onclick: close }, "Ahora no"),
    ),
  ]);
}
