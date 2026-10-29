from collections import defaultdict

SHORT = "Palabras Cortas"
VERBS = "Verbos"
NOUNS = "NoVerbos"
COMPLEX = "bCfghjLmñpqRvxyz"
SPLITTING = COMPLEX + "lu"
DOUBLE = "bcdfghijlmnñpqtuvxyzCLR"
SPELLED = {"C": "ch", "L": "ll", "R": "rr"}
SIMPLE = ""
SIMPLE_FOLDER = "Solo letras simples"
DOUBLE_FOLDER = "Letras duplicadas"
ENDINGS_FOLDER = "Terminaciones raras"
RARE_ENDS = "iuytmxcgpjfbhCLv"
MAX_LEAF = 50
MIN_LEAF = 20
SHORT_LISTS = [("Dos letras", 2), ("Tres letras", 3)]
LENGTHS = [("4, 5 y 6 letras", range(4, 7)), ("7 y 8 letras", range(7, 9)), ("9 y 10 letras", range(9, 11))]


def default_lines(families, tops):
    return (short_lines(families) + tops + kind_lines(families, verbal=True) + kind_lines(families, verbal=False) +
            ending_lines(families))


def short_lines(families):
    return [short_line(families, name, size) for name, size in SHORT_LISTS]


def short_line(families, name, size):
    members = [f for f in families if any(len(w) == size for w in f["words"])]
    return line([SHORT], name, spec(min_length=size, max_length=size), [], members)


def ending_lines(families):
    by_end = defaultdict(list)
    for f in families:
        if not f["verbal"] and f["words"][0][-1] in RARE_ENDS:
            by_end[f["words"][0][-1]].append(f)
    path = [NOUNS, ENDINGS_FOLDER]
    return [l for group in letter_groups(sorted(by_end, key=alphabetic), lambda k: by_end[k])
            for l in ending_group(path, group, unique(f for k in group for f in by_end[k]))]


def ending_group(path, letters, members):
    single = len(letters) == 1
    filters = spec(kind="NON_VERBAL", fixed_end=letters[0] if single else "")
    name = f"Terminadas en -{upper(letters[0])}" if single else "Otras terminaciones raras"
    note = name if single else "Terminadas en " + ", ".join(f"-{upper(l)}" for l in letters)
    return leaf_lines(path, name, filters, [note], members)


def kind_lines(families, verbal):
    root = [VERBS if verbal else NOUNS]
    short = {id(f) for f in families if any(len(w) <= 3 for w in f["words"])}
    own = [f for f in families if f["verbal"] == verbal and (measured(f) >= 4 or id(f) not in short)]
    return [l for i, (name, sizes) in enumerate(LENGTHS)
            for l in length_lines(root + [name], verbal, name, [f for f in own if measured(f) in sizes or (i == 0 and measured(f) < 4)])]


def measured(family):
    return len(family["words"][0]) - (1 if family["verbal"] else 0)


def length_lines(path, verbal, length_name, families):
    context = Context(path, verbal, f"Jefe de familia de {length_name}")
    return simple_lines(context, families) + double_lines(context, families) + letter_lines(context, families)


class Context:
    def __init__(self, path, verbal, note):
        self.path, self.verbal, self.note = path, verbal, note

    def filters(self, contains="", simple=False):
        return spec(contains=contains, kind="VERBAL" if self.verbal else "NON_VERBAL", simple=simple)


def simple_lines(context, families):
    members = [f for f in families if not any(c in SPLITTING for c in heard(f["words"][0])) and not doubled(f["words"][0])]
    return leaf_lines(context.path, SIMPLE_FOLDER, context.filters(simple=True), [context.note], members)


def double_lines(context, families):
    by_letter = defaultdict(list)
    for f in families:
        for letter in doubled(f["words"][0]):
            by_letter[letter].append(f)
    everything = unique(f for fs in by_letter.values() for f in fs)
    if len(everything) <= MAX_LEAF:
        return leaf_lines(context.path, DOUBLE_FOLDER, context.filters(), ["Con alguna letra duplicada", context.note], everything)
    path = context.path + [DOUBLE_FOLDER]
    return [l for group in letter_groups(sorted(by_letter, key=alphabetic), lambda k: by_letter[k])
            for l in double_group(path, context, group, unique(f for k in group for f in by_letter[k]))]


def double_group(path, context, letters, members):
    if letters == ["n"] and context.verbal:
        return double_n(path, context, members)
    single = len(letters) == 1
    filters = context.filters(contains=spelled(letters[0]) * 2 if single else "")
    notes = ([] if single else ["Con alguna letra duplicada"]) + [context.note]
    return leaf_lines(path, double_title(letters), filters, notes, members)


def double_n(path, context, members):
    prefixed = [f for f in members if f["words"][0].startswith("en")]
    rest = [f for f in members if not f["words"][0].startswith("en")]
    with_prefix = spec(contains="nn", kind="VERBAL" if context.verbal else "NON_VERBAL", fixed="en")
    return (leaf_lines(path, "Doble N, prefijo EN", with_prefix, [context.note], prefixed) +
            leaf_lines(path, "Doble N, sin prefijo EN", context.filters(contains="nn"), [context.note], rest))


def letter_lines(context, families):
    by_main = defaultdict(lambda: defaultdict(list))
    for f in families:
        for main, second in pairs(f["words"][0]):
            by_main[main][second].append(f)
    size = lambda main: len(unique(f for fs in by_main[main].values() for f in fs))
    mains = sorted(by_main, key=alphabetic)
    small = unique(f for main in mains if size(main) < MIN_LEAF for fs in by_main[main].values() for f in fs)
    lines = [l for main in mains if size(main) >= MIN_LEAF for l in main_lines(context, main, by_main[main])]
    return lines + leaf_lines(context.path, "Letras raras", context.filters(), [context.note], small)


def main_lines(context, main, by_second):
    everything = unique(f for fs in by_second.values() for f in fs)
    filters = context.filters(contains=spelled(main))
    if len(everything) <= MAX_LEAF:
        return leaf_lines(context.path, title(main, []), filters, [context.note], everything)
    path = context.path + [title(main, [])]
    seconds = sorted(by_second, key=lambda s: "" if s == SIMPLE else alphabetic(s))
    return [l for group in letter_groups(seconds, lambda s: by_second[s])
            for l in leaf_lines(path, title(main, group), filters, seconds_notes(group) + [context.note],
                                unique(f for s in group for f in by_second[s]))]


def leaf_lines(path, name, filters, notes, members):
    if not members:
        return []
    chunks = parts_of(sorted(members, key=lambda f: -f["points"]))
    if len(chunks) == 1:
        return [line(path, name, filters, notes, chunks[0])]
    return [line(path + [name], f"{name} · parte {i + 1}", filters, notes, chunk) for i, chunk in enumerate(chunks)]


def parts_of(members):
    return parts_of_size(members, MAX_LEAF)


def parts_of_size(members, size):
    chunks = [members[i:i + size] for i in range(0, len(members), size)]
    if len(chunks) > 1 and len(chunks[-1]) < MIN_LEAF:
        chunks[-2:] = [chunks[-2] + chunks[-1]]
    return chunks


def letter_groups(keys, members_of):
    alone = [[k] for k in keys if len(members_of(k)) >= MIN_LEAF]
    small = [k for k in keys if len(members_of(k)) < MIN_LEAF]
    if small and alone and len(unique(f for k in small for f in members_of(k))) < MIN_LEAF:
        min(alone, key=lambda g: len(members_of(g[0]))).extend(small)
        return alone
    return alone + ([small] if small else [])


def heard(head):
    return [c for i, c in enumerate(head) if not silent_u(head, i)]


def silent_u(head, i):
    return head[i] == "u" and i > 0 and (head[i - 1] == "q" or (head[i - 1] == "g" and head[i + 1:i + 2] in ("e", "i")))


def doubled(head):
    letters = heard(head)
    return {c for c in DOUBLE if letters.count(c) >= 2}


def pairs(head):
    letters = {c for c in heard(head) if c in SPLITTING}
    for main in letters:
        for second in (letters - {main}) or {SIMPLE}:
            yield main, second


def alphabetic(letter):
    return SPELLED.get(letter, letter).replace("ñ", "n~")


def unique(families):
    return list({id(f): f for f in families}.values())


def title(main, seconds):
    return f"Letra {upper(main)}" + (f", {seconds_text(seconds)}" if seconds else "")


def double_title(letters):
    names = [upper(l) for l in letters]
    if len(names) > 3:
        return "Dobles de letras raras"
    return "Doble " + (names[0] if len(names) == 1 else ", ".join(names[:-1]) + " y " + names[-1])


def seconds_text(seconds):
    if seconds == [SIMPLE]:
        return "letras simples"
    names = [("simples" if s == SIMPLE else upper(s)) for s in seconds]
    if len(names) == 1:
        return f"Letra {names[0]}"
    if len(names) <= 3:
        return "Letras " + ", ".join(names[:-1]) + " y " + names[-1]
    return "letras raras"


def seconds_notes(seconds):
    if seconds == [SIMPLE]:
        return ["Las demás letras, simples"]
    if len(seconds) == 1:
        return [f"Con la letra {upper(seconds[0])}"]
    return []


def spelled(letter):
    return SPELLED.get(letter, letter)


def upper(letter):
    return spelled(letter).upper()


def key_of(family):
    return ("v:" if family["verbal"] else "n:") + family["words"][0]


def spec(min_length=2, max_length=10, contains="", kind="ALL", simple=False, fixed="", fixed_end="", words=False):
    return "|".join([str(min_length), str(max_length), contains, fixed, fixed_end, kind, "POINTS", "", "", "s" if simple else "",
                     "p" if words else "f"])


def line(path, name, filters, notes, members):
    return "\t".join(["/".join(path), name, filters, "|".join(notes), " ".join(key_of(f) for f in members)])
