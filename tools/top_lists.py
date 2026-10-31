from default_lists import key_of, measured, parts_of_size, spec

ROOT = "Tops"
SIZE = 100
BINGOS = ("Palabras de Scrabble", 2000, "Familias cuyas palabras más se juegan como scrabble")
VALUABLE = ("Palabras valiosas", 1000, "Familias de más puntos que casi nunca se juegan como scrabble")
BINGO_SHARE = 0.2
MIN_VALUABLE = 4
FIELDS = ("scrabble_count", "total_points", "scrabble_points")


def top_lines(families, stats, forms):
    totals = [(f, family_stats(f, stats, forms)) for f in families]
    played = [f for f, t in sorted(totals, key=lambda p: -p[1]["scrabble_count"]) if t["scrabble_count"] > 0]
    valuable = [f for f, t in sorted(totals, key=lambda p: -p[1]["total_points"]) if is_valuable(f, t)]
    return (ranked_lines(BINGOS, played, spec()) +
            ranked_lines(VALUABLE, valuable, spec(min_length=MIN_VALUABLE)))


def family_stats(family, stats, forms):
    words = family_words(family, forms)
    return {field: sum(stats.get(w, {}).get(field, 0) for w in words) for field in FIELDS}


def family_words(family, forms):
    if not family["verbal"]:
        return set(family["words"])
    return {form for w in family["words"] for form in forms.get(w, ())} | set(family["words"])


def is_valuable(family, totals):
    return (measured(family) >= MIN_VALUABLE and totals["total_points"] > 0 and
            totals["scrabble_points"] < BINGO_SHARE * totals["total_points"])


def ranked_lines(section, families, filters):
    name, most, note = section
    chunks = parts_of_size(families[:most], SIZE)
    return [ranked_line(name, note, filters, i, chunk) for i, chunk in enumerate(chunks)]


def ranked_line(name, note, filters, index, chunk):
    first = index * SIZE + 1
    title = f"Top {first}–{first + len(chunk) - 1}"
    return "\t".join([f"{ROOT}/{name}", title, filters, note, " ".join(key_of(f) for f in chunk)])
