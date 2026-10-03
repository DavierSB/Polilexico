package classic

import (
	"errors"
	"slices"

	"github.com/domino14/word-golib/tilemapping"
)

const (
	QAnywhere = "anywhere"
	QUnplayed = "unplayed"
	QPlayed   = "played"
	QYours    = "yours"
	QHidden   = "hidden"
)

var errQPlace = errors.New("posición de la Q desconocida")

func validQ(q string) bool {
	return slices.Contains([]string{QAnywhere, QUnplayed, QPlayed, QYours, QHidden}, q)
}

func (sim *simulation) qMatches(q string) bool {
	switch q {
	case QUnplayed:
		return !sim.qOnBoard()
	case QPlayed:
		return sim.qOnBoard()
	case QYours:
		return sim.qInRack(sim.play.Game.PlayerOnTurn())
	case QHidden:
		return !sim.qOnBoard() && !sim.qInRack(sim.play.Game.PlayerOnTurn())
	}
	return true
}

func (sim *simulation) qOnBoard() bool {
	b, q := sim.play.Game.Board(), sim.q()
	for row := 0; row < b.Dim(); row++ {
		for col := 0; col < b.Dim(); col++ {
			if b.GetLetter(row, col) == q {
				return true
			}
		}
	}
	return false
}

func (sim *simulation) qInRack(player int) bool {
	return slices.Contains(sim.play.Game.RackFor(player).TilesOn(), sim.q())
}

func (sim *simulation) q() tilemapping.MachineLetter {
	q, _ := sim.play.Game.Alphabet().Val("Q")
	return q
}
