package preendgame

import (
	"slices"

	"github.com/domino14/macondo/game"
	"github.com/domino14/word-golib/tilemapping"

	"lexico/engine/internal/endgame"
)

func (b *preBot) winsByPassing(s situation) bool {
	g := b.game
	me := g.PlayerOnTurn()
	pos, err := extended(g, me, s.unseen)
	if err != nil || !b.scanClear(pos, me) {
		return false
	}
	return b.endgameWon(pos)
}

func ahead(g *game.Game, me int, unseenTiles *tilemapping.Rack) bool {
	ld := g.Rules().LetterDistribution()
	mine := g.PointsFor(me) - g.RackFor(me).ScoreOn(ld)
	theirs := g.PointsFor(1-me) - lowest(unseenTiles, ld, game.RackTileLimit)
	return mine > theirs
}

func lowest(r *tilemapping.Rack, ld *tilemapping.LetterDistribution, n int) int {
	values := make([]int, 0, r.NumTiles())
	for _, t := range r.TilesOn() {
		values = append(values, ld.Score(t))
	}
	slices.Sort(values)
	total := 0
	for _, v := range values[:min(n, len(values))] {
		total += v
	}
	return total
}

func extended(g *game.Game, me int, unseenTiles *tilemapping.Rack) (*endgame.Position, error) {
	var s endgame.State
	s.Board, s.OnTurn, s.Scoreless = g.Board(), 1-me, 1
	s.Racks[me], s.Racks[1-me] = g.RackFor(me), unseenTiles
	s.Points = [2]int{g.PointsFor(0), g.PointsFor(1)}
	return endgame.FromState(s)
}

func (b *preBot) scanClear(pos *endgame.Position, me int) bool {
	reachable, err := newScanner(pos, me, b.config.ScanBudget).reachable()
	return err == nil && !reachable
}

func (b *preBot) endgameWon(pos *endgame.Position) bool {
	r, err := endgame.SearchFrom(pos, b.config.Endgame)
	return err == nil && r.Move != nil && r.Spread < 0
}
