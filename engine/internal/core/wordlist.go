package core

import (
	"errors"
	"math/rand/v2"
	"sync"

	"github.com/domino14/word-golib/kwg"
	"github.com/domino14/word-golib/tilemapping"
)

var errNoWords = errors.New("no hay palabras de ese largo")

type wordList struct {
	letters []tilemapping.MachineLetter
	starts  []uint32
}

type lengths struct {
	min, max int
}

var (
	listsMu sync.Mutex
	lists   = map[lengths]*wordList{}
)

func RandomWord(minTiles, maxTiles int) (string, error) {
	gd, err := WordGraph()
	if err != nil {
		return "", err
	}
	l := listOf(gd, lengths{minTiles, maxTiles})
	if len(l.starts) == 0 {
		return "", errNoWords
	}
	return l.word(rand.IntN(len(l.starts))).UserVisible(gd.GetAlphabet()), nil
}

func listOf(gd *kwg.KWG, key lengths) *wordList {
	listsMu.Lock()
	defer listsMu.Unlock()
	if l, ok := lists[key]; ok {
		return l
	}
	l := &wordList{}
	l.collect(gd, gd.ArcIndex(0), nil, key)
	lists[key] = l
	return l
}

func (l *wordList) collect(gd *kwg.KWG, node uint32, prefix []tilemapping.MachineLetter, key lengths) {
	if node == 0 || len(prefix) >= key.max {
		return
	}
	for ; ; node++ {
		word := append(prefix, tilemapping.MachineLetter(gd.Tile(node)))
		if gd.Accepts(node) && len(word) >= key.min {
			l.starts = append(l.starts, uint32(len(l.letters)))
			l.letters = append(l.letters, word...)
		}
		l.collect(gd, gd.ArcIndex(node), word, key)
		if gd.IsEnd(node) {
			return
		}
	}
}

func (l *wordList) word(i int) tilemapping.MachineWord {
	end := uint32(len(l.letters))
	if i+1 < len(l.starts) {
		end = l.starts[i+1]
	}
	return tilemapping.MachineWord(l.letters[l.starts[i]:end])
}
