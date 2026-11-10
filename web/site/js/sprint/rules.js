export const SPRINT_RULES = [
  ["Encuentra tantos scrabbles como puedas, dentro de un límite de tiempo y de intentos fallidos (vidas)."],
  ["En cada ronda se muestra un tablero distinto, con un atril que tiene al menos un scrabble: encuéntralo, no importa si no es el de mayor puntuación."],
  ["Si no lo encuentras, puedes rendirte y pasar a otro tablero, pero pierdes una vida. En modo Single, realizar una jugada no válida también cuesta una vida."],
  ["Puedes ajustar la dificultad, el tiempo, el número de vidas y la comprobación de jugadas."],
];

export const DIFFICULTY_INFO = "Fácil: todos los tableros que salgan tendrán múltiples scrabbles para colocar.\n\nNormal: puede salirte cualquier tablero con al menos un scrabble.\n\nDifícil: todos los tableros que salgan tendrán una sola oportunidad de scrabble.";

export const CHALLENGE_INFO = "Cómo se tratan las jugadas con palabras que no existen.\n\nVoid: si intentas colocar una jugada inválida, no se te permite y continúas tu turno.\n\nSingle: si colocas una jugada inválida, vuelven las fichas a tu atril y pierdes una vida.";
