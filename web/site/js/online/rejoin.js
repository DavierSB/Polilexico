import { go } from "../router.js";
import { online } from "./client.js";
import { roomRoute } from "./lobby.js";

export function watchRejoin() {
  online.subscribe(rejoinIfLive);
}

function rejoinIfLive(c) {
  const route = c.rejoining && !c.ended && !c.finished() ? roomRoute(c) : null;
  if (!route) return;
  c.rejoining = false;
  c.parked = null;
  if (location.hash !== "#/" + route) go(route);
}
