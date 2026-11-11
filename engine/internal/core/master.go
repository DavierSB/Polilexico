package core

import (
	"cmp"
	"slices"

	"github.com/domino14/macondo/ai/bot"
	"github.com/domino14/macondo/board"
	"github.com/domino14/macondo/game"
	"github.com/domino14/macondo/move"
	"github.com/domino14/word-golib/tilemapping"
)

type tiebreak func(a, b *move.Move) int

func MasterPlays(master *bot.BotTurnPlayer, g *game.Game, player int) []*move.Move {
	plays := copyMoves(generatorOf(master).GenAll(g.RackFor(player), false))
	master.AssignEquity(plays, g.Board(), g.Bag(), g.RackFor(1-player))
	slices.SortStableFunc(plays, MasterOrder(g.Board()))
	return plays
}

func MasterOrder(b *board.GameBoard) func(x, y *move.Move) int {
	rules := []tiebreak{byScore, byBlank, byEquity, byTilesPlayed, byMainWord(b), byPosition}
	return func(x, y *move.Move) int {
		for _, rule := range rules {
			if c := rule(x, y); c != 0 {
				return c
			}
		}
		if x.TiebreaksBetter(y) {
			return -1
		}
		return 1
	}
}

func byScore(a, b *move.Move) int {
	return cmp.Compare(b.Score(), a.Score())
}

func byBlank(a, b *move.Move) int {
	return cmp.Compare(rank(usesBlank(a)), rank(usesBlank(b)))
}

func byEquity(a, b *move.Move) int {
	return cmp.Compare(b.Equity(), a.Equity())
}

func byTilesPlayed(a, b *move.Move) int {
	return cmp.Compare(a.TilesPlayed(), b.TilesPlayed())
}

func byMainWord(b *board.GameBoard) tiebreak {
	return func(x, y *move.Move) int {
		return slices.Compare(MainWord(b, x), MainWord(b, y))
	}
}

func byPosition(a, b *move.Move) int {
	aRow, aCol, aVertical := a.CoordsAndVertical()
	bRow, bCol, bVertical := b.CoordsAndVertical()
	if aVertical != bVertical {
		return cmp.Compare(rank(aVertical), rank(bVertical))
	}
	if aVertical {
		return cmp.Or(cmp.Compare(aRow, bRow), cmp.Compare(aCol, bCol))
	}
	return cmp.Or(cmp.Compare(aCol, bCol), cmp.Compare(aRow, bRow))
}

func usesBlank(m *move.Move) bool {
	return slices.ContainsFunc(m.Tiles(), tilemapping.MachineLetter.IsBlanked)
}

func rank(b bool) int {
	if b {
		return 1
	}
	return 0
}

func copyMoves(moves []*move.Move) []*move.Move {
	out := make([]*move.Move, len(moves))
	for i, m := range moves {
		out[i] = new(move.Move)
		out[i].CopyFrom(m)
	}
	return out
}
