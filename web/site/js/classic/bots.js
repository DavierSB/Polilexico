export const BOTS = [
  { name: "BeginnerBot", alias: "Polimita", photo: "polimita", theme: "Hoja", description: "Polimita va despacio, ideal si justo empiezas.", voidOnly: true },
  { name: "BasicBot", alias: "Sancho", photo: "sancho", theme: "Polimita", description: "De tanto cabalgar, Sancho ha aprendido a jugar." },
  { name: "BetterBot", alias: "Pelusa", photo: "pelusa", theme: "Celeste", description: "¡Cuidado con la mano de Dios!" },
  { name: "STEEBot", alias: "María M.", photo: "maria", theme: "Polimita", description: "María escribió un diccionario, ¡y las usará todas contra ti!" },
  { name: "HastyBot", alias: "Gitana", photo: "gitana", theme: "Noche", description: "La mejor. ¿Qué esconde tras su mirada?" },
];

export function bot(name) {
  return BOTS.find((b) => b.name === name || b.alias === name) ?? null;
}

export function botPhoto(b) {
  return `img/bots/${b.photo}.webp`;
}
