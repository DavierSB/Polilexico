#!/usr/bin/env python3
import json
import lzma
import re
import sys
import unicodedata
from pathlib import Path

import pandas as pd

LEXICO = Path(__file__).resolve().parent.parent
PRIVATE = LEXICO.parent
SOURCE = PRIVATE / "WordFamilies" / "output_for_scrabble" / "diccionario"
THEMED_DEMONYMS = PRIVATE / "WordFamilies" / "output_for_scrabble" / "gentilicios.parquet"
STUDY = PRIVATE / "Scrabble Study" / "LexicSimulator2" / "studies" / "gentilicios"
ASSETS = LEXICO / "android" / "game" / "src" / "main" / "assets" / "dictionary"
ORDER = ["wikt_es", "drae1914", "wikt_en"]
SOURCES = {"wikt_es": "w", "drae1914": "d", "wikt_en": "e"}
CATEGORIES = {"sustantivo": "s", "adjetivo": "a", "verbo": "v", "otra": "o", "adverbio": "r"}
DIRECT = "0,0"
MAX_SENSES = 3
SAME_SENSE = 0.8
SAME_ENGLISH_SENSE = 0.6
SHORTEST_COMPARED = 3
FUNCTION_WORDS = set("de del la las el los lo un una unos unas y o u e a al en con por para que se su sus como es son le les mas muy "
                     "sin sobre the of or and to an in on for from with by as is be its it that which this".split())
LONG_PLACE = 80
ONLY_USED = re.compile(r"^only used in .* of (\S+?)[,.;]?$")
SUBSCRIPTS = str.maketrans("", "", "₀₁₂₃₄₅₆₇₈₉")
FILTERS = [{"id": lzma.FILTER_LZMA2, "preset": 9 | lzma.PRESET_EXTREME, "lc": 4, "lp": 0, "pb": 0, "dict_size": 1 << 25}]


def main():
    words = pd.read_csv(SOURCE / "palabras.tsv", sep="\t", keep_default_na=False)
    relations = {}
    write("words.xz", word_lines(words, relations))
    write("relations.xz", ["\t".join(r) for r in relations])
    write("senses.xz", sense_lines(chosen_senses(set(words.lemma_key))))
    write("demonyms.xz", demonym_lines())


def word_lines(words, relations):
    lines = [word_line(w, rows, relations) for w, rows in words.sort_values(["word", "rank"]).groupby("word", sort=True)]
    return [l for l in lines if not l.endswith("\t" + DIRECT)]


def word_line(word, rows, relations):
    spelled = plain(word)
    return word + "\t" + ";".join(entry(spelled, r, relations) for r in rows.itertuples())


def entry(spelled, row, relations):
    relation = relations.setdefault((row.via, row.relacion, row.categoria), len(relations))
    return ",".join([relative(spelled, row.lemma_key), str(relation)] + ([relative(spelled, row.medio)] if row.medio else []))


def plain(word):
    return word.replace("C", "ch").replace("L", "ll").replace("R", "rr")


def encoded(word):
    return word.replace("ch", "C").replace("ll", "L").replace("rr", "R")


def relative(base, target):
    shared = 0
    while shared < min(len(base), len(target)) and base[shared] == target[shared]:
        shared += 1
    return f"{len(base) - shared}{target[shared:]}"


def chosen_senses(lemmas):
    senses = pd.read_parquet(SOURCE / "completo" / "acepciones.parquet")
    senses = senses[senses.lemma_key.isin(lemmas)].assign(rank=lambda s: s.fuente.map(ORDER.index))
    senses = senses[senses["rank"] == senses.groupby("lemma_key")["rank"].transform("min")]
    senses = senses.assign(definicion=[cleaned(s, t) for s, t in zip(senses.fuente, senses.definicion)])
    return {key: picked(distinct(rows)) for key, rows in senses.groupby("lemma_key", sort=True)}


def cleaned(source, text):
    match = ONLY_USED.match(text) if source == "wikt_en" else None
    return f"Forma de {match.group(1)}" if match else text


def distinct(rows):
    kept = []
    for row in rows.itertuples():
        if all(overlap(row.definicion, k.definicion) < threshold(row, k) for k in kept):
            kept.append(row)
    return kept


def threshold(first, second):
    return SAME_ENGLISH_SENSE if first.fuente == second.fuente == "wikt_en" else SAME_SENSE


def overlap(a, b):
    first, second = vocabulary(a), vocabulary(b)
    smaller = min(len(first), len(second))
    return len(first & second) / smaller if smaller >= SHORTEST_COMPARED else 0


def vocabulary(text):
    bare = "".join(c for c in unicodedata.normalize("NFKD", text.lower()) if not unicodedata.combining(c))
    return set(re.findall(r"[a-zñ]+", bare)) - FUNCTION_WORDS


def picked(rows):
    leaders = [r for i, r in enumerate(rows) if r.categoria not in {p.categoria for p in rows[:i]}][:MAX_SENSES]
    chosen = leaders + [r for r in rows if r not in leaders][:MAX_SENSES - len(leaders)]
    return sorted(chosen, key=rows.index)


def sense_lines(chosen):
    return [key + "\t" + "\t".join(sense(key, r) for r in rows) for key, rows in chosen.items() if rows]


def sense(key, row):
    lemma = "" if row.lemma == key else row.lemma
    return "\t".join([lemma, SOURCES[row.fuente] + CATEGORIES[row.categoria], row.pos, row.definicion])


def demonym_lines():
    places = {}
    for form, place in study_places() + themed_places():
        places.setdefault(form, place)
    return [f"{form}\tNatural de {place}" for form, place in sorted(places.items())]


def study_places():
    places = json.loads((STUDY / "gentilicios_lugares.json").read_text(encoding="utf-8"))
    themed = dict(themed_places())
    forms = study_forms()
    return [(form, short(place)) for word, place in places.items()
            if len(place) <= LONG_PLACE or encoded(word) not in themed for form in forms.get(encoded(word), [encoded(word)])]


def short(place):
    place = place.translate(SUBSCRIPTS).strip()
    return place.split(", ")[0] if len(place) > LONG_PLACE else place


def study_forms():
    df = pd.read_parquet(STUDY / "gentilicios.parquet", columns=["masculino", "femenino", "masculino_plural", "femenino_plural"])
    return {row[0]: [w for w in row if isinstance(w, str)] for row in df.itertuples(index=False) if isinstance(row[0], str)}


def themed_places():
    df = pd.read_parquet(THEMED_DEMONYMS, columns=["word", "formas", "lugar"])
    return [(form, place) for forms, place in zip(df.formas, df.lugar) if place for form in forms]


def write(name, lines):
    ASSETS.mkdir(parents=True, exist_ok=True)
    path = ASSETS / name
    path.write_bytes(lzma.compress(("\n".join(lines) + "\n").encode("utf-8"), format=lzma.FORMAT_XZ, filters=FILTERS))
    print(f"{name}: {len(lines)} lineas, {path.stat().st_size // 1024} KB", file=sys.stderr)


if __name__ == "__main__":
    main()
