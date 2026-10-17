package endgame

import (
	"slices"

	"github.com/domino14/macondo/move"
)

func (s *searcher) trackLines() {
	s.lines = make([][]*move.Move, s.config.MaxDepth+2)
}

func (s *searcher) endLine(ply int) {
	if s.lines != nil {
		s.lines[ply] = nil
	}
}

func (s *searcher) extendLine(ply int, m *move.Move) {
	if s.lines != nil {
		s.lines[ply] = slices.Concat([]*move.Move{m}, s.lines[ply+1])
	}
}
