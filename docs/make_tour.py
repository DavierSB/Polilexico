#!/usr/bin/env python3
"""Arma docs/assets/tour.gif: las capturas de docs/screenshots una tras otra, con su nombre debajo.

Las que aun no existen se saltan: al añadir una (por ejemplo, las de duplicada), basta con
volver a ejecutarlo. Necesita Pillow:  python3 docs/make_tour.py
"""
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

DOCS = Path(__file__).resolve().parent
SHOTS = DOCS / "screenshots"
FONT = DOCS.parent / "android/ui/common/src/main/res/font/mulish_extrabold.ttf"
OUT = DOCS / "assets/tour.gif"

# (captura, nombre), en el orden en que se ven: el juego de cada modalidad en varias etapas, con
# los temas alternados.
TOUR = [
    ("theme-hoja.png", "Inicio"),
    ("classic-setup.png", "Clásica: elección de rival"),
    ("classic-placing.png", "Clásica: los puntos al colocar"),
    ("classic-late.png", "Clásica: el final de la partida"),
    ("classic-moves.png", "Clásica: la planilla"),
    ("duplicate-game.png", "Duplicada"),
    ("duplicate-round.png", "Duplicada: fin de la ronda"),
    ("duplicate-moves.png", "Duplicada: la planilla"),
    ("recall-watch.png", "¿Cuántas recuerdas?: memorizar"),
    ("recall-build.png", "¿Cuántas recuerdas?: anagramar"),
    ("sprint.png", "Scrabble Sprint"),
    ("sprint-reveal.png", "Scrabble Sprint: los scrabbles"),
    ("analyzer.png", "Analizador"),
    ("review.png", "Revisión turno a turno"),
    ("stats.png", "Mis estadísticas"),
]

WIDTH = 360
CAPTION = 56
BACKGROUND = (27, 21, 18)
INK = (255, 182, 49)
SECONDS = 2.2


def frame(path: Path, caption: str, font: ImageFont.FreeTypeFont) -> Image.Image:
    shot = Image.open(path).convert("RGB")
    shot = shot.resize((WIDTH, round(shot.height * WIDTH / shot.width)), Image.LANCZOS)
    out = Image.new("RGB", (WIDTH, shot.height + CAPTION), BACKGROUND)
    out.paste(shot, (0, 0))
    ImageDraw.Draw(out).text((WIDTH / 2, shot.height + CAPTION / 2), caption, font=font, fill=INK, anchor="mm")
    return out


def main() -> None:
    font = ImageFont.truetype(str(FONT), 20)
    frames = [frame(SHOTS / name, caption, font) for name, caption in TOUR if (SHOTS / name).exists()]
    height = max(f.height for f in frames)
    frames = [f if f.height == height else f.resize((WIDTH, height)) for f in frames]
    frames[0].save(OUT, save_all=True, append_images=frames[1:], duration=int(SECONDS * 1000), loop=0, optimize=True)
    skipped = [name for name, _ in TOUR if not (SHOTS / name).exists()]
    print(f"{OUT.relative_to(DOCS.parent)}: {len(frames)} capturas" + (f"; faltan {', '.join(skipped)}" if skipped else ""))


if __name__ == "__main__":
    main()
