import { h } from "../dom.js";
import { openDialog } from "../dialogs.js";

export const DOWNLOAD_URL = "https://polilexico.github.io/download";

const MOBILE = /Android|iPhone|iPad|iPod|Mobile/i;
const PACKAGE = "app.lexico";
const OPEN_URL = `intent://abrir#Intent;scheme=polilexico;package=${PACKAGE};S.browser_fallback_url=${encodeURIComponent(DOWNLOAD_URL)};end`;

export async function offerDownload() {
  if (!MOBILE.test(navigator.userAgent)) return;
  const offer = (await appInstalled()) ? OPEN_OFFER : DOWNLOAD_OFFER;
  openDialog((close) => [offerCard(offer, close)]);
}

const DOWNLOAD_OFFER = { title: "Descarga Poliléxico para Android", action: "Descargar", href: DOWNLOAD_URL };
const OPEN_OFFER = { title: "Abre Poliléxico", action: "Abrir", href: OPEN_URL };

function offerCard(offer, close) {
  return h("div.download-offer", {},
    h("img", { src: "img/logo.svg", alt: "", width: 72, height: 62 }),
    h("h2", {}, offer.title),
    h("a.btn.block.download-btn", { href: offer.href }, offer.action),
    h("button.btn.block.outlined", { onclick: close }, "Ahora no"),
  );
}

async function appInstalled() {
  try {
    const apps = (await navigator.getInstalledRelatedApps?.()) ?? [];
    return apps.some((app) => app.id === PACKAGE);
  } catch {
    return false;
  }
}
