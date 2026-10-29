from collections import defaultdict

TARGET = 5
MOST = 8
VOWELS = "aeiou"
SOFT = "ei"
CONSONANTS = ["b", "c", "d", "f", "g", "h", "j", "l", "m", "n", "ñ", "p", "r", "s", "t", "v", "x", "y", "z", "C", "L", "R", "qu", "gu"]
RARE = {"f", "h", "j", "ñ", "v", "x", "y", "z", "C", "L", "R", "qu"}
ONSET_R = "bcdfgpt"
ONSET_L = "bcfgp"
CODA = "nsrl"
VOWEL_RUNS = ["a", "e", "i", "o", "u", "ie", "ue", "ai", "ei", "oi", "au", "eu", "ia", "io", "ua", "uo", "iu", "ui"]
SOUND, AR_EAR, FINAL_VOWEL, CLUSTER, VOWEL, COMMON, RARE_PAIR, CROSS = "1", "2", "3", "4", "5", "6", "7", "8"


def similar_lines(families):
    pool = defaultdict(set)
    for i, f in enumerate(families):
        for w in f["words"]:
            pool[w].add(i)
    return [" ".join(similar(i, f, pool)) for i, f in enumerate(families)]


def similar(index, family, pool):
    own = set(family["words"])
    found = {}
    for code, variants in rules(family["words"][0], family["verbal"]):
        if len(found) >= TARGET:
            break
        for word in ranked(variants, pool, own | set(found), index)[:MOST - len(found)]:
            found[word] = code
    return [c + w for w, c in found.items()]


def ranked(variants, pool, taken, index):
    fresh = {v for v in variants if v in pool and v not in taken and index not in pool[v]}
    return [w for _, w in sorted((min(pool[v]), v) for v in fresh)]


def rules(word, verbal):
    yield SOUND, list(sounds(word))
    yield AR_EAR, list(ar_ear(word)) if verbal else []
    yield FINAL_VOWEL, list(final_vowels(word))
    yield CLUSTER, list(clusters(word))
    yield VOWEL, list(vowel_changes(word))
    changes = list(consonant_changes(word))
    yield COMMON, [w for w, old, new in changes if old not in RARE and new not in RARE]
    yield RARE_PAIR, [w for w, old, new in changes if old in RARE and new in RARE]
    yield CROSS, [w for w, old, new in changes if (old in RARE) != (new in RARE)]


def final_vowels(word):
    for vowel in VOWELS:
        yield word + vowel
    if word[-1] in VOWELS:
        yield word[:-1]


def sounds(word):
    for i, c in enumerate(word):
        for sub in sound_alikes(word, i, c):
            yield word[:i] + sub + word[i + 1:]
    for i, c in enumerate(word):
        if c in VOWELS and (i == 0 or word[i - 1] != "c"):
            yield word[:i] + "h" + word[i:]


def sound_alikes(word, i, c):
    after = word[i + 1:i + 2]
    soft = after != "" and after in SOFT
    subs = {"b": ["v"], "v": ["b"], "s": ["z"], "z": ["s"], "h": [""]}.get(c, [])
    if c in "sz" and soft:
        subs = subs + ["c"]
    if c == "c" and soft:
        subs = ["s", "z"]
    if c in "gj" and soft:
        subs = ["j" if c == "g" else "g"]
    if c in "yL" and after != "" and after in VOWELS:
        subs = ["L" if c == "y" else "y"]
    if i == len(word) - 1 and c in "iy":
        subs = ["y" if c == "i" else "i"]
    return subs


def ar_ear(word):
    if word.endswith("ear"):
        yield hard_before_a(word[:-3]) + "ar"
    elif word.endswith("ar"):
        yield hard_before_e(word[:-2]) + "ear"


def hard_before_a(stem):
    for soft, hard in (("qu", "c"), ("gu", "g"), ("c", "z")):
        if stem.endswith(soft):
            return stem[:-len(soft)] + hard
    return stem


def hard_before_e(stem):
    for hard, soft in (("c", "qu"), ("g", "gu"), ("z", "c")):
        if stem.endswith(hard):
            return stem[:-1] + soft
    return stem


def clusters(word):
    for i in range(len(word) + 1):
        for letter in insertable(word, i):
            yield word[:i] + letter + word[i:]
    for i in range(len(word)):
        if removable(word, i):
            yield word[:i] + word[i + 1:]


def insertable(word, i):
    before, here = word[i - 1:i], word[i:i + 1]
    letters = []
    if before and here and here in VOWELS:
        letters += [l for l, onset in (("r", ONSET_R), ("l", ONSET_L)) if before in onset]
    if here in ("r", "l") and here and (not before or before in VOWELS) and word[i + 1:i + 2] in tuple(VOWELS):
        letters += list(ONSET_R if here == "r" else ONSET_L)
    if before and before in VOWELS and here and here not in VOWELS:
        letters += ["m"] if here in "bp" else [l for l in CODA if l != here]
    return letters


def removable(word, i):
    before, here, after = word[i - 1:i], word[i], word[i + 1:i + 2]
    vowel_after = after != "" and after in VOWELS
    liquid = here in "rl" and before != "" and before in (ONSET_R if here == "r" else ONSET_L) and vowel_after
    onset = after in ("r", "l") and here in (ONSET_R if after == "r" else ONSET_L) and (not before or before in VOWELS)
    coda = before != "" and before in VOWELS and after != "" and after not in VOWELS and (here in CODA or (here == "m" and after in "bp"))
    return liquid or onset or coda


def units(word):
    out, i = [], 0
    while i < len(word):
        silent_u = word[i] in "qg" and word[i + 1:i + 2] == "u" and (word[i] == "q" or word[i + 2:i + 3] in tuple(SOFT))
        out.append(word[i:i + 2] if silent_u else word[i])
        i += 2 if silent_u else 1
    return out


def vowel_changes(word):
    parts = units(word)
    for start, end in vowel_runs(parts):
        old = "".join(parts[start:end])
        for new in VOWEL_RUNS:
            if new != old:
                before = keep_sound(parts[start - 1], old[0], new[0]) if start > 0 else ""
                yield "".join(parts[:max(0, start - 1)]) + before + new + "".join(parts[end:])


def vowel_runs(parts):
    runs, start = [], None
    for i, unit in enumerate(parts + ["#"]):
        is_vowel = unit in VOWELS
        if is_vowel and start is None:
            start = i
        if not is_vowel and start is not None:
            runs.append((start, i))
            start = None
    return runs


def keep_sound(consonant, old, new):
    was_soft, is_soft = old in SOFT, new in SOFT
    if was_soft == is_soft:
        return consonant
    changes = {("c", False): "qu", ("c", True): "z", ("qu", True): "c", ("g", False): "gu", ("g", True): "j", ("gu", True): "g", ("z", False): "c"}
    return changes.get((consonant, was_soft), consonant)


def consonant_changes(word):
    parts = units(word)
    for i, unit in enumerate(parts):
        if unit in CONSONANTS:
            for other in CONSONANTS:
                if other != unit:
                    yield "".join(parts[:i]) + other + "".join(parts[i + 1:]), unit, other
