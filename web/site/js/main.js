import { apply } from "./theme.js";
import { startEngine } from "./engine.js";
import { start } from "./router.js";
import { homeScreen } from "./screens/home.js";
import { classicSetupScreen } from "./classic/setup.js";
import { classicGameScreen } from "./classic/screen.js";
import { accountScreen } from "./online/account.js";
import { lobbyScreen } from "./online/lobby.js";
import { seekScreen } from "./online/seek.js";
import { onlineGameScreen } from "./online/game.js";

apply();

start({
  "": homeScreen,
  clasica: classicSetupScreen,
  partida: classicGameScreen,
  "en-linea": lobbyScreen,
  cuenta: accountScreen,
  buscar: seekScreen,
  sala: onlineGameScreen,
});

window.addEventListener("load", () => setTimeout(startEngine, 1500));
