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
import { menuScreen } from "./screens/menu.js";
import { duplicateSetupScreen } from "./duplicate/setup.js";
import { duplicateGameScreen } from "./duplicate/screen.js";

apply();

start({
  "": homeScreen,
  clasica: classicSetupScreen,
  partida: classicGameScreen,
  "en-linea": lobbyScreen,
  cuenta: accountScreen,
  buscar: seekScreen,
  sala: onlineGameScreen,
  duplicada: duplicateSetupScreen,
  "duplicada-partida": duplicateGameScreen,
  minijuegos: menuScreen("minijuegos"),
  estudiar: menuScreen("estudiar"),
});

window.addEventListener("load", () => setTimeout(startEngine, 1500));
