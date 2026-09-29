package core

import (
	"sort"
	"strings"

	"github.com/domino14/macondo/game"
	"github.com/domino14/word-golib/tilemapping"
)

// TilesText: fichas como "A CH E ?", sin los corchetes de los digrafos.
func TilesText(g *game.Game, tiles []tilemapping.MachineLetter) string {
	out := make([]string, len(tiles))
	for i, t := range tiles {
		out[i] = withoutBrackets(t.UserVisible(g.Alphabet(), false))
	}
	return strings.Join(out, " ")
}

// RackText: el atril de un jugador como en TilesText.
func RackText(g *game.Game, player int) string {
	return TilesText(g, g.RackFor(player).TilesOn())
}

// Unseen: las fichas que el jugador no ve (la bolsa mas las hidden), ordenadas y con el
// comodin al final, como el "tile tracking" de Woogles.
func Unseen(g *game.Game, hidden ...tilemapping.MachineLetter) string {
	tiles := append(append([]tilemapping.MachineLetter{}, g.Bag().Peek()...), hidden...)
	sortTiles(tiles)
	return TilesText(g, tiles)
}

// El comodin es la letra 0: va al final.
func sortTiles(tiles []tilemapping.MachineLetter) {
	sort.Slice(tiles, func(i, j int) bool {
		if (tiles[i] == 0) != (tiles[j] == 0) {
			return tiles[j] == 0
		}
		return tiles[i] < tiles[j]
	})
}
