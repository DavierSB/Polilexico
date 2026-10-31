#!/usr/bin/env python3
import re
from bisect import bisect_left
import sys
from collections import defaultdict
from pathlib import Path

from default_lists import default_lines
from similar_words import similar_lines
from themed_lists import themed_lines
from top_lists import top_lines

import pandas as pd

LEXICO = Path(__file__).resolve().parent.parent
WORD_FAMILIES = LEXICO.parent / "WordFamilies"
FAMILIES = WORD_FAMILIES / "output_for_scrabble" / "familias"
ASSETS = LEXICO / "android" / "game" / "src" / "main" / "assets" / "lexicon"

MAX_TILES = 9
ROLES = ["masculino", "femenino", "masculino_plural", "femenino_plural"]
TRAITS = {"intransitive_verbs": "i", "impersonal_verbs": "m", "unipersonal_verbs": "u", "defective_verbs": "d",
          "different_participle_verbs": "p"}
ROLE_CODES = {"masculino": "m", "femenino": "f", "masculino_plural": "mp", "femenino_plural": "fp"}
HALF_DIGRAPH = re.compile(r"(C)h|(L)l|(R)r")
COMMON = 90


def main():
    points = word_points()
    families = [f for f in verb_families() + noun_families(points) if f["words"]]
    families = sorted(families + adopt_orphans(families, points), key=lambda f: -f["points"])
    ranks = permilles([f["points"] for f in families])
    write("families.tsv", [family_line(f, r) for f, r in zip(families, ranks)])
    write("words.tsv", word_lines(families, points))
    write("similar.tsv", similar_lines(families, verb_forms()))
    common = common_words()
    study = [f for f in families if common.get(f["words"][0], 0) < COMMON]
    write("default_lists.tsv", default_lines(study, top_lines(study, word_stats(), verb_forms())) + themed_lines(study))


def key(word):
    return HALF_DIGRAPH.sub(lambda m: m.group(m.lastindex), word)


def fits(word):
    return len(word) <= MAX_TILES


def verb_fits(infinitive):
    return len(infinitive) - 1 <= MAX_TILES + 1


def word_points():
    df = pd.read_parquet(WORD_FAMILIES / "Importancia" / "palabras.parquet", columns=["word", "total_points"])
    return {key(w): int(p) for w, p in zip(df.word, df.total_points)}


def common_words():
    df = pd.read_parquet(WORD_FAMILIES / "Comunes" / "palabras_comunes.parquet", columns=["word", "pct"])
    return {key(w): p for w, p in zip(df.word, df.pct) if pd.notna(p)}


def word_stats():
    columns = ["word", "scrabble_count", "total_points", "scrabble_points"]
    df = pd.read_parquet(WORD_FAMILIES / "Importancia" / "palabras.parquet", columns=columns)
    return {key(w): {"scrabble_count": c, "total_points": t, "scrabble_points": s} for w, c, t, s in df.itertuples(index=False)}


def family_points(kind):
    df = pd.read_parquet(WORD_FAMILIES / "Importancia" / "familias.parquet", columns=["familia", "tipo", "total_points"])
    df = df[df.tipo == kind]
    return {key(f): int(p) for f, p in zip(df.familia, df.total_points)}


def verb_traits():
    traits = defaultdict(str)
    for name, code in TRAITS.items():
        for verb in pd.read_parquet(WORD_FAMILIES / "Conjugator" / f"{name}.parquet").word:
            traits[key(verb)] += code
    return traits


def verb_families():
    paradigm = family_points("verbal")
    traits = verb_traits()
    df = pd.read_parquet(FAMILIES / "familias_verbales.parquet", columns=["word", "jefe"])
    members = defaultdict(list)
    for word, head in zip(df.word, df.jefe):
        members[key(head)].append(key(word))
    grouped = {w for ws in members.values() for w in ws}
    for verb in paradigm.keys() - grouped:
        members[verb].append(verb)
    return [verb_family(head, words, paradigm, traits) for head, words in members.items() if verb_fits(head)]


def verb_family(head, words, paradigm, traits):
    words = [head] + sorted({w for w in words if w != head and verb_fits(w)}, key=lambda w: (len(w), w))
    total = sum(paradigm.get(w, 0) for w in words)
    code = " ".join(f"{w}:{traits[w]}" for w in words if traits.get(w))
    return {"verbal": True, "points": total, "code": code, "words": words}


def noun_families(points):
    df = pd.read_parquet(FAMILIES / "familias_no_verbales.parquet", columns=ROLES)
    return [noun_family(row, points) for row in df.itertuples(index=False) if fits(key(head_of(row)).split("/")[0])]


def head_of(row):
    return row[0] if pd.notna(row[0]) else row[1]


def noun_family(row, points):
    roles = [ROLE_CODES[col] for col, value in zip(ROLES, row) if pd.notna(value)]
    words = [key(w) for value in row if pd.notna(value) for w in value.split("/")]
    code = "todos" if len(roles) == 4 else ", ".join(roles)
    words = [w for w in dict.fromkeys(words) if fits(w)]
    return {"verbal": False, "points": sum(points.get(w, 0) for w in words), "code": code, "words": words}


def adopt_orphans(families, points):
    owned = {w for f in families for w in f["words"]} | {w for ws in verb_forms().values() for w in ws}
    nouns = {w: f for f in reversed(families) if not f["verbal"] for w in f["words"]}
    lexicon = {key(w) for w in pd.read_parquet(WORD_FAMILIES / "Conjugator" / "all_words.parquet").word}
    orphans = sorted(w for w in lexicon if fits(w) and w not in owned)
    loose = [w for w in orphans if not adopted(w, nouns)]
    return orphan_families(loose, points)


def adopted(word, nouns):
    family = next((nouns[s] for s in stems(word) if s in nouns), None)
    if family is not None:
        family["words"].append(word)
    return family is not None


def stems(word):
    out = []
    if word.endswith("es"):
        out += [word[:-2]]
    if word.endswith("s"):
        out += [word[:-1]]
    if word.endswith("as"):
        out += [word[:-2] + "o", word[:-2] + "e", word[:-2]] + ([word[:-4]] if word.endswith("esas") else [])
    if word.endswith("a"):
        out += [word[:-1] + "o", word[:-1] + "e", word[:-1]] + ([word[:-3]] if word.endswith("esa") else [])
    return [s for s in out if len(s) >= 2]


def orphan_families(loose, points):
    rest, out = set(loose), []
    for word in loose:
        if word in rest:
            plurals = [p for p in (word + "s", word + "es") if p in rest]
            rest -= {word, *plurals}
            out.append({"verbal": False, "points": sum(points.get(w, 0) for w in [word] + plurals), "code": "", "words": [word] + plurals})
    return out


def family_line(f, rank):
    return f"{'v' if f['verbal'] else 'n'}\t{f['points']}\t{rank}\t{f['code']}\t{' '.join(f['words'])}"


def permilles(points):
    played = sorted(p for p in points if p > 0)
    return [min(999, bisect_left(played, p) * 1000 // len(played)) if p > 0 else -1 for p in points]


def word_lines(families, points):
    lexicon = list(dict.fromkeys(key(w) for w in pd.read_parquet(WORD_FAMILIES / "Conjugator" / "all_words.parquet").word))
    lexicon = [w for w in lexicon if fits(w)]
    forms = verb_forms()
    owners, nominal = defaultdict(set), set()
    verbal = set(forms) | {w for ws in forms.values() for w in ws}
    for i, f in enumerate(families):
        for w in f["words"]:
            owners[w].add(i)
            (verbal if f["verbal"] else nominal).add(w)
            for form in forms.get(w, ()) if f["verbal"] else ():
                owners[form].add(i)
    lexicon.sort(key=lambda w: -points.get(w, 0))
    ranks = permilles([points.get(w, 0) for w in lexicon])
    return [f"{kind(w, verbal, nominal)}\t{r}\t{','.join(map(str, sorted(owners[w])))}\t{w}"
            for w, r in zip(lexicon, ranks)]


def verb_forms():
    df = pd.read_parquet(WORD_FAMILIES / "Conjugator" / "word_origin.parquet", columns=["word", "verbo"])
    forms = defaultdict(set)
    for word, verb in zip(df.word, df.verbo):
        forms[key(verb)].add(key(word))
    return forms


def kind(word, verbal, nominal):
    is_verb = word in verbal
    is_noun = word in nominal or not is_verb
    return "b" if is_verb and is_noun else ("v" if is_verb else "n")


def write(name, lines):
    ASSETS.mkdir(parents=True, exist_ok=True)
    path = ASSETS / name
    path.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"{name}: {len(lines)} lineas, {path.stat().st_size // 1024} KB", file=sys.stderr)


if __name__ == "__main__":
    main()
