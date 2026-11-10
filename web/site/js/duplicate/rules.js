export const DUPLICATE_RULES = [
  ["En cada ronda debes colocar la jugada de mayor valor que encuentres."],
  ["Tendrás 3 minutos por ronda y se jugará a bolsa completa, aunque puedes ajustar tanto el tiempo por ronda como la cantidad de rondas a jugar.", "Duración"],
  ["Hasta la ronda 15, el atril debe tener menos de 6 vocales y menos de 6 consonantes. Desde la ronda 16, al menos una vocal y una consonante. Si antes de la ronda 16 entre la bolsa y el atril queda una sola vocal o una sola consonante, se aplica desde entonces la regla de la ronda 16.\n\nLos comodines no cuentan como vocales ni consonantes, y la Y cuenta siempre como consonante.", "Atril válido"],
  ["Si el atril no es válido, se devuelven todas sus fichas a la bolsa y se sacan siete nuevas (pueden volver a salir las mismas). Si en la bolsa quedan menos de siete, se sacan todas. Se repite hasta conseguir un atril válido. Si con las fichas que quedan ya no se puede formar ninguno, la partida termina.", "Cambio de fichas"],
  ["Sumas los puntos de tu jugada. Si no juegas a tiempo, o en modo Single o Al Final realizas una jugada no válida, 0 puntos. Hay acierto cuando tu jugada hace los mismos puntos que la jugada maestra.\n\nEn modo Al Final no se sabe durante la partida si una jugada es válida: el marcador la cuenta con sus puntos. Al terminar se muestran las jugadas inválidas y después el resultado real.", "Puntuación"],
  ["Si varias jugadas empatan en puntos, se selecciona como jugada maestra aquella que no usa comodín y deja el mejor atril. La valoración del atril está dada por el motor de juego usado en esta aplicación.", "Desempate entre jugadas"],
];

export const ROUNDS_TEXT = "Cuántas rondas se juegan. A bolsa completa, la partida sigue hasta que ya no se puede formar un atril válido.";
export const VOID_HINT = "si intentas colocar una jugada inválida, no se te permite y continúas tu turno.";
export const SINGLE_HINT = "si colocas una jugada inválida, obtienes 0 puntos en esa ronda.";
export const AT_END_HINT = "como en Single, una jugada inválida vale 0 puntos, pero no se sabe hasta el final: durante la partida cuenta con sus puntos. Al terminar se muestran las jugadas inválidas y después el resultado real.";
