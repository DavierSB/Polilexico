package core

import (
	"github.com/domino14/macondo/board"
	"github.com/domino14/macondo/cross_set"
	"github.com/domino14/macondo/movegen"
	"github.com/domino14/word-golib/kwg"
	"github.com/domino14/word-golib/tilemapping"
)

func NewGenerator(b *board.GameBoard) (*movegen.GordonGenerator, error) {
	gd, err := WordGraph()
	if err != nil {
		return nil, err
	}
	return movegen.NewGordonGenerator(gd, b, rules.LetterDistribution()), nil
}

func WordGraph() (*kwg.KWG, error) {
	if err := Ready(); err != nil {
		return nil, err
	}
	return kwg.GetKWG(cfg.WGLConfig(), Lexicon, kwg.WithDistribution(distribution))
}

func CrossSets() (cross_set.Generator, error) {
	if err := Ready(); err != nil {
		return nil, err
	}
	return rules.CrossSetGen(), nil
}

func Distribution() (*tilemapping.LetterDistribution, error) {
	if err := Ready(); err != nil {
		return nil, err
	}
	return rules.LetterDistribution(), nil
}
