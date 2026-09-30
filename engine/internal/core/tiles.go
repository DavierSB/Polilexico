package core

import (
	"sort"
	"strings"

	"github.com/domino14/macondo/game"
	"github.com/domino14/word-golib/tilemapping"
)

func TilesText(g *game.Game, tiles []tilemapping.MachineLetter) string {
	out := make([]string, len(tiles))
	for i, t := range tiles {
		out[i] = withoutBrackets(t.UserVisible(g.Alphabet(), false))
	}
	return strings.Join(out, " ")
}

func RackText(g *game.Game, player int) string {
	return TilesText(g, g.RackFor(player).TilesOn())
}

func Unseen(g *game.Game, hidden ...tilemapping.MachineLetter) string {
	tiles := append(append([]tilemapping.MachineLetter{}, g.Bag().Peek()...), hidden...)
	sortTiles(tiles)
	return TilesText(g, tiles)
}

func sortTiles(tiles []tilemapping.MachineLetter) {
	sort.Slice(tiles, func(i, j int) bool {
		if (tiles[i] == 0) != (tiles[j] == 0) {
			return tiles[j] == 0
		}
		return tiles[i] < tiles[j]
	})
}

func RackValue(g *game.Game, player int) int {
	return g.RackFor(player).ScoreOn(g.Bag().LetterDistribution())
}
