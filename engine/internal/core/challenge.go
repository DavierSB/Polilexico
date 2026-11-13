package core

import (
	"strings"

	"github.com/domino14/macondo/game"
	"github.com/domino14/word-golib/tilemapping"
)

var claimWords = map[string]bool{"reclamar": true, "challenge": true}

func IsClaim(input string) bool {
	return claimWords[firstWord(input)]
}

func IsPass(input string) bool {
	return passWords[firstWord(input)]
}

func LastWords(g *game.Game) (words, invalid []string) {
	events := g.History().Events
	if len(events) == 0 {
		return nil, nil
	}
	for _, w := range events[len(events)-1].WordsFormed {
		words = append(words, withoutBrackets(w))
		if !isVisibleWord(g, w) {
			invalid = append(invalid, withoutBrackets(w))
		}
	}
	return words, invalid
}

func firstWord(input string) string {
	fields := strings.Fields(input)
	if len(fields) == 0 {
		return ""
	}
	return strings.ToLower(fields[0])
}

func isVisibleWord(g *game.Game, w string) bool {
	mw, err := tilemapping.ToMachineWord(w, g.Alphabet())
	return err == nil && IsWord(g, mw)
}
