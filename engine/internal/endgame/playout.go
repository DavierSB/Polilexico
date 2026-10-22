package endgame

import "github.com/domino14/macondo/move"

const maxPlayoutMoves = 40

func (p *Position) Playout(player int) int {
	return p.playout(player, nil)
}

func (p *Position) PlayoutLine(player int) ([]*move.Move, int) {
	var line []*move.Move
	spread := p.playout(player, &line)
	return line, spread
}

func (p *Position) playout(player int, line *[]*move.Move) int {
	played := 0
	for !p.Over() && played < maxPlayoutMoves {
		m := p.greedy()
		if p.Play(m) != nil {
			break
		}
		played++
		if line != nil {
			*line = append(*line, m)
		}
	}
	spread := p.Spread(player)
	for range played {
		p.Undo()
	}
	return spread
}

func (p *Position) greedy() *move.Move {
	if p.Forced() {
		return p.pass()
	}
	p.gen.GenAll(p.rack(p.OnTurn()), false)
	return p.highest(p.allowed(p.gen.Plays()))
}

func (p *Position) highest(moves []*move.Move) *move.Move {
	best, bestValue := moves[0], p.estimate(moves[0])
	for _, m := range moves[1:] {
		if v := p.estimate(m); v > bestValue || (v == bestValue && m.TilesPlayed() > best.TilesPlayed()) {
			best, bestValue = m, v
		}
	}
	return best
}

func (p *Position) estimate(m *move.Move) int {
	return p.valueFor(m, p.OnTurn())
}

func (p *Position) valueFor(m *move.Move, player int) int {
	if m.TilesPlayed() == p.RackSize(player) {
		return m.Score() + 2*p.RackValue(1-player)
	}
	return m.Score() - m.Leave().Score(p.ld)
}
