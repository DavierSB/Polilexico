package core

import (
	"fmt"

	"github.com/domino14/macondo/move"
)

const (
	KindPlay     = "play"
	KindPass     = "pass"
	KindExchange = "exchange"
	KindInvalid  = "invalid"
	KindTimeout  = "timeout"
)

type Play struct {
	Kind      string
	Coords    string
	Tiles     string
	TileCount int
	Score     int
}

type Candidate struct {
	Coords      string  `json:"coords"`
	Description string  `json:"description"`
	Score       int     `json:"score"`
	Equity      float64 `json:"equity"`
}

func PlayOf(m *move.Move) Play {
	switch m.Action() {
	case move.MoveTypePlay:
		return Play{Kind: KindPlay, Coords: MoveCoords(m), Tiles: m.TilesString(),
			TileCount: m.TilesPlayed(), Score: m.Score()}
	case move.MoveTypeExchange:
		return Play{Kind: KindExchange, Tiles: m.TilesStringExchange(), TileCount: m.TilesPlayed()}
	}
	return Play{Kind: KindPass}
}

func Describe(p Play) string {
	switch p.Kind {
	case KindPlay:
		return fmt.Sprintf("%s %s (%d pts)", p.Coords, p.Tiles, p.Score)
	case KindExchange:
		return describeExchange(p)
	case KindInvalid:
		return fmt.Sprintf("(Inválida %s %s: pierde el turno)", p.Coords, p.Tiles)
	case KindTimeout:
		return "(Sin jugada -- se agotó el tiempo)"
	}
	return "(Pasar)"
}

func CandidateOf(p Play, equity float64) Candidate {
	return Candidate{Coords: p.Coords, Description: Describe(p), Score: p.Score, Equity: equity}
}

func Candidates(moves []*move.Move) []Candidate {
	out := make([]Candidate, len(moves))
	for i, m := range moves {
		out[i] = CandidateOf(PlayOf(m), float64(m.Equity()))
	}
	return out
}

func SameCandidate(a, b Candidate) bool {
	return a.Coords == b.Coords && a.Description == b.Description
}

func describeExchange(p Play) string {
	if p.Tiles == "" {
		return fmt.Sprintf("(Cambia %d ficha(s))", p.TileCount)
	}
	return fmt.Sprintf("(Cambiar %s)", p.Tiles)
}
