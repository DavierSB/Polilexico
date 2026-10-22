package endgame

import (
	"github.com/domino14/macondo/board"
	"github.com/domino14/word-golib/tilemapping"
)

type snapshot struct {
	board     *board.GameBoard
	racks     [2]*tilemapping.Rack
	points    [2]int
	onTurn    int
	scoreless int
	over      bool
}

func (p *Position) push() {
	if p.depth == len(p.stack) {
		p.stack = append(p.stack, p.newSnapshot())
	}
	p.save(&p.stack[p.depth])
	p.depth++
}

func (p *Position) pop() {
	p.depth--
	s := &p.stack[p.depth]
	p.board.CopyFrom(s.board)
	p.racks[0].CopyFrom(s.racks[0])
	p.racks[1].CopyFrom(s.racks[1])
	p.points, p.onTurn, p.scoreless, p.over = s.points, s.onTurn, s.scoreless, s.over
}

func (p *Position) newSnapshot() snapshot {
	return snapshot{board: p.board.Copy(), racks: [2]*tilemapping.Rack{p.racks[0].Copy(), p.racks[1].Copy()}}
}

func (p *Position) save(s *snapshot) {
	s.board.CopyFrom(p.board)
	s.racks[0].CopyFrom(p.racks[0])
	s.racks[1].CopyFrom(p.racks[1])
	s.points, s.onTurn, s.scoreless, s.over = p.points, p.onTurn, p.scoreless, p.over
}
