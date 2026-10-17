package core

import (
	"github.com/domino14/macondo/board"
	"github.com/domino14/macondo/movegen"
	"github.com/domino14/word-golib/kwg"
)

func NewGenerator(b *board.GameBoard) (*movegen.GordonGenerator, error) {
	if err := Ready(); err != nil {
		return nil, err
	}
	gd, err := kwg.GetKWG(cfg.WGLConfig(), Lexicon, kwg.WithDistribution(distribution))
	if err != nil {
		return nil, err
	}
	return movegen.NewGordonGenerator(gd, b, rules.LetterDistribution()), nil
}
