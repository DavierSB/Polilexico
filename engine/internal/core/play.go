package core

import (
	"fmt"

	"github.com/domino14/macondo/move"
)

// Tipos de jugada, tal como llegan a Android en el campo Kind.
const (
	KindPlay     = "play"     // colocacion
	KindPass     = "pass"     // pase
	KindExchange = "exchange" // cambio de fichas
	KindInvalid  = "invalid"  // palabra no valida con "pierde el turno" activado
	KindTimeout  = "timeout"  // se agoto el tiempo sin jugar (duplicada)
)

// Play es una jugada ya reducida a datos, sin tipos de macondo.
type Play struct {
	Kind      string
	Coords    string // "H8" o "8H" en colocaciones (e invalidas)
	Tiles     string // "CA.A" ('.' = letra ya en el tablero); en cambios, las fichas cambiadas
	TileCount int    // fichas colocadas o cambiadas
	Score     int
}

// Candidate es una jugada en los registros de partida (-log.json), con el formato de las
// herramientas de revision de la PC.
type Candidate struct {
	Coords      string  `json:"coords"`
	Description string  `json:"description"`
	Score       int     `json:"score"`
	Equity      float64 `json:"equity"`
}

// PlayOf reduce una jugada de macondo a Play.
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

// Describe es el texto de la jugada en los registros: "H8 CASA (12 pts)", "(Pasar)"...
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

// CandidateOf: la jugada p como entrada de registro, con su equity.
func CandidateOf(p Play, equity float64) Candidate {
	return Candidate{Coords: p.Coords, Description: Describe(p), Score: p.Score, Equity: equity}
}

// Candidates: las jugadas de macondo como entradas de registro, en el mismo orden.
func Candidates(moves []*move.Move) []Candidate {
	out := make([]Candidate, len(moves))
	for i, m := range moves {
		out[i] = CandidateOf(PlayOf(m), float64(m.Equity()))
	}
	return out
}

// SameCandidate: a y b son la misma jugada.
func SameCandidate(a, b Candidate) bool {
	return a.Coords == b.Coords && a.Description == b.Description
}

// Sin Tiles (un cambio del rival en una partida a ciegas) solo se sabe cuantas fichas cambio.
func describeExchange(p Play) string {
	if p.Tiles == "" {
		return fmt.Sprintf("(Cambia %d ficha(s))", p.TileCount)
	}
	return fmt.Sprintf("(Cambiar %s)", p.Tiles)
}
