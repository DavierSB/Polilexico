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
import { newDuplicateScreen } from "./online/newDuplicate.js";
import { duplicateRoomScreen } from "./online/duplicateRoom.js";
import { profileScreen } from "./online/profile.js";
import { menuScreen } from "./screens/menu.js";
import { duplicateSetupScreen } from "./duplicate/setup.js";
import { duplicateGameScreen } from "./duplicate/screen.js";
import { sprintSetupScreen } from "./sprint/setup.js";
import { sprintGameScreen } from "./sprint/screen.js";
import { endgameSetupScreen } from "./classic/endgame.js";
import { recallSetupScreen } from "./recall/setup.js";
import { recallGameScreen } from "./recall/screen.js";
import { anagramSetupScreen } from "./anagram/setup.js";
import { anagramGameScreen } from "./anagram/screen.js";
import { anagrammerScreen } from "./study/anagrammer.js";
import { analyzerScreen } from "./study/analyzer.js";
import { dictionaryScreen } from "./study/word.js";
import { listsMenuScreen, catalogScreen, listScreen, studyScreen, unfinishedScreen, completedScreen } from "./study/listScreens.js";
import { testScreen } from "./study/test.js";
import { createListScreen } from "./study/create.js";
import { gamesScreen } from "./games/list.js";
import { statsScreen } from "./games/stats.js";
import { reviewScreen } from "./games/review.js";
import { READY_ROUTES } from "./screens/menu.js";

apply();
["sprint", "finales", "recuerdas", "anagrama", "anagramador", "analizador", "listas", "diccionario"].forEach((r) => READY_ROUTES.add(r));

start({
  "": homeScreen,
  clasica: classicSetupScreen,
  partida: classicGameScreen,
  "en-linea": lobbyScreen,
  cuenta: accountScreen,
  buscar: seekScreen,
  sala: onlineGameScreen,
  "crear-duplicada": newDuplicateScreen,
  "sala-duplicada": duplicateRoomScreen,
  perfil: profileScreen,
  duplicada: duplicateSetupScreen,
  "duplicada-partida": duplicateGameScreen,
  sprint: sprintSetupScreen,
  "sprint-partida": sprintGameScreen,
  finales: endgameSetupScreen,
  recuerdas: recallSetupScreen,
  "recuerdas-partida": recallGameScreen,
  anagrama: anagramSetupScreen,
  "anagrama-partida": anagramGameScreen,
  anagramador: anagrammerScreen,
  analizador: analyzerScreen,
  diccionario: dictionaryScreen,
  listas: listsMenuScreen,
  "listas-catalogo": catalogScreen,
  lista: listScreen,
  "lista-estudio": studyScreen,
  "lista-evaluar": testScreen,
  "crear-lista": createListScreen,
  "estudios-medias": unfinishedScreen,
  "estudios-culminados": completedScreen,
  partidas: gamesScreen,
  estadisticas: statsScreen,
  "partida-revision": reviewScreen,
  minijuegos: menuScreen("minijuegos"),
  estudiar: menuScreen("estudiar"),
});

window.addEventListener("load", () => setTimeout(startEngine, 300));
