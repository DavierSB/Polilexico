package core

import (
	"sort"

	"github.com/domino14/word-golib/kwg"
	"github.com/domino14/word-golib/tilemapping"
)

func Anagrams(rack string) ([]string, error) {
	gd, err := WordGraph()
	if err != nil {
		return nil, err
	}
	var a kwg.KWGAnagrammer
	if err := a.InitForString(gd, rack); err != nil {
		return nil, err
	}
	var words []string
	err = a.Anagram(gd, func(w tilemapping.MachineWord) error {
		words = append(words, w.UserVisible(gd.GetAlphabet()))
		return nil
	})
	sort.Strings(words)
	return words, err
}

func Plain(words []string) []string {
	out := make([]string, len(words))
	for i, w := range words {
		out[i] = withoutBrackets(w)
	}
	sort.Strings(out)
	return out
}
