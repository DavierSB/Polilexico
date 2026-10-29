from pathlib import Path

import pandas as pd

from default_lists import MAX_LEAF, MIN_LEAF, leaf_lines, spec

THEMES_DIR = Path(__file__).resolve().parent.parent.parent / "WordFamilies" / "output_for_scrabble"
ROOT = "Temáticas"
THEMES = [
    ("gentilicios", "Gentilicios", ["ense", "ano", "eño", "ino", "es", "ita", "ero", "co", "io", "eo"]),
    ("quimica", "Química", ["ina", "ico", "ato", "ito", "ido", "uro", "eno", "ano", "io", "ol", "osa"]),
    ("plantas", "Plantas", []),
    ("animales", "Animales", []),
    ("extranjerismos", "Extranjerismos", []),
]
LENGTHS = [("2 a 5 letras", 2, 5), ("6 y 7 letras", 6, 7), ("8 y 9 letras", 8, 9)]
OTHER_ENDINGS = "Otras terminaciones"


def themed_lines(families):
    owner = owners(families)
    return [l for file, name, endings in THEMES for l in theme_lines(name, endings, members_of(file, owner))]


def owners(families):
    out = {}
    for f in sorted(families, key=lambda f: f["verbal"]):
        for w in f["words"]:
            out.setdefault(w, f)
    return out


def members_of(file, owner):
    words = pd.read_parquet(THEMES_DIR / f"{file}.parquet", columns=["word"]).word
    found = [(w, owner[w]) for w in words if w in owner]
    return list({id(f): (w, f) for w, f in found}.values())


def theme_lines(name, endings, members):
    return [l for title, low, high in LENGTHS
            for l in length_lines([ROOT, name], name, title, endings, spec(min_length=low, max_length=high),
                                  [(w, f) for w, f in members if low <= len(w) <= high])]


def length_lines(path, theme, title, endings, filters, members):
    notes = [theme, f"Palabra principal de {title}"]
    groups = ending_groups(endings, members) if len(members) > MAX_LEAF else []
    if len(groups) < 2:
        return leaf_lines(path, title, filters, notes, [f for _, f in members])
    return [l for ending, group in groups
            for l in leaf_lines(path + [title], ending_title(ending), filters, notes + ending_notes(ending), group)]


def ending_groups(endings, members):
    groups = {e: [f for w, f in members if ending_of(w, endings) == e] for e in endings}
    alone = [(e, fs) for e, fs in groups.items() if len(fs) >= MIN_LEAF]
    rest = [f for w, f in members if ending_of(w, endings) not in dict(alone)]
    return alone + ([(None, rest)] if rest else [])


def ending_of(word, endings):
    return next((e for e in endings if word.endswith(e)), None)


def ending_title(ending):
    return OTHER_ENDINGS if ending is None else f"Terminados en -{ending.upper()}"


def ending_notes(ending):
    return [] if ending is None else [ending_title(ending)]
