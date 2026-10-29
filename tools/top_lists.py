from default_lists import parts_of_size, spec

ROOT = "Tops"
SIZE = 100
BINGOS = ("Scrabbles más jugados", 2000, "Scrabbles que más se juegan en las simulaciones")
VALUABLE = ("Palabras valiosas más jugadas", 1000, "Palabras de más puntos que casi nunca se juegan como scrabble")
BINGO_SHARE = 0.2
MIN_VALUABLE = 4


def top_lines(words):
    played = sorted((w for w in words if w["scrabble_count"] > 0), key=lambda w: -w["scrabble_count"])
    valuable = sorted((w for w in words if len(w["word"]) >= MIN_VALUABLE and w["total_points"] > 0 and
                       w["scrabble_points"] < BINGO_SHARE * w["total_points"]),
                      key=lambda w: -w["total_points"])
    return ranked_lines(BINGOS, played, spec(words=True)) + ranked_lines(VALUABLE, valuable, spec(min_length=MIN_VALUABLE, words=True))


def ranked_lines(section, words, filters):
    name, most, note = section
    chunks = parts_of_size([w["word"] for w in words[:most]], SIZE)
    return [ranked_line(name, note, filters, i, chunk) for i, chunk in enumerate(chunks)]


def ranked_line(name, note, filters, index, chunk):
    first = index * SIZE + 1
    title = f"Top {first}–{first + len(chunk) - 1}"
    return "\t".join([f"{ROOT}/{name}", title, filters, note, " ".join("w:" + w for w in chunk)])
