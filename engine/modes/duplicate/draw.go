package duplicate

import (
	"errors"
	"sort"

	"github.com/domino14/macondo/move"
	"github.com/domino14/word-golib/tilemapping"

	"lexico/engine/internal/core"
)

// Motivos de fin de partida en Draw.EndReason.
const (
	EndNoValidRack    = "no_valid_rack"    // no quedan vocales o consonantes para un atril valido (FISF 3.2)
	EndNoPlayableRack = "no_playable_rack" // ningun atril posible admite una colocacion
)

// Intentos de sacar un atril antes de dar la partida por terminada.
const maxDrawAttempts = 40

var errGameOver = errors.New("la partida ya terminó")

// Draw es el atril de un turno, ya pasado por las reglas FISF.
type Draw struct {
	Rack        string // el atril del turno ("A CH E ?")
	Redrawn     bool   // el primer atril era una mano invalida y se volvio a sacar
	InitialRack string // ese primer atril, si Redrawn (se muestra InvalidRackSeconds)
	GameOver    bool   // no se pudo formar un atril: la partida termina aqui
	EndReason   string // si GameOver, EndNoValidRack o EndNoPlayableRack
}

// DrawRack saca el atril del turno (el reliquat del anterior completado a 7) y le aplica las
// reglas FISF 3.2 y 3.6. Si el turno ya estaba en curso devuelve el mismo atril.
func (d *Game) DrawRack() (*Draw, error) {
	d.mu.Lock()
	defer d.mu.Unlock()
	if d.over() {
		return nil, errGameOver
	}
	if d.inTurn() {
		return &Draw{Rack: d.rack}, nil
	}
	d.turn++
	return d.draw(), nil
}

// draw vuelve a sacar mientras el atril sea invalido o no admita ninguna colocacion.
func (d *Game) draw() *Draw {
	draw := &Draw{}
	for i := 0; i < maxDrawAttempts; i++ {
		if done := d.tryRack(draw); done != nil {
			return done
		}
		if !d.redraw() {
			break
		}
	}
	return d.endGame(draw, EndNoPlayableRack)
}

// tryRack mira el atril actual: devuelve el Draw final (turno empezado o partida terminada), o
// nil si hay que volver a sacar.
func (d *Game) tryRack(draw *Draw) *Draw {
	switch {
	case !d.rackIsValid() && !d.poolIsValid():
		return d.endGame(draw, EndNoValidRack)
	case !d.rackIsValid():
		d.noteRedraw(draw)
	case d.startTurn():
		draw.Rack = d.rack
		return draw
	}
	return nil
}

// startTurn calcula las jugadas del máster con el atril actual; false si no hay ninguna
// colocacion.
func (d *Game) startTurn() bool {
	plays := d.masterPlays()
	if len(plays) == 0 || plays[0].Action() == move.MoveTypePass {
		return false
	}
	d.plays = plays
	d.rack = core.RackText(d.g, masterIdx)
	d.boardBefore = core.RenderBoard(d.g)
	return true
}

// masterPlays: todas las jugadas del atril, la mejor primero, copiadas porque GenAll
// reutiliza sus buffers.
func (d *Game) masterPlays() []*move.Move {
	plays := d.master.GenAll(d.g.RackFor(masterIdx), false)
	sort.Slice(plays, func(i, j int) bool { return plays[i].TiebreaksBetter(plays[j]) })
	return copyMoves(plays)
}

func (d *Game) noteRedraw(draw *Draw) {
	if !draw.Redrawn {
		draw.Redrawn = true
		draw.InitialRack = core.RackText(d.g, masterIdx)
	}
}

func (d *Game) redraw() bool {
	_, err := d.g.SetRandomRack(masterIdx, nil)
	return err == nil
}

func (d *Game) endGame(draw *Draw, reason string) *Draw {
	d.ended = true
	d.finish()
	draw.GameOver, draw.EndReason = true, reason
	return draw
}

func (d *Game) rackIsValid() bool {
	return d.validTiles(d.g.RackFor(masterIdx).TilesOn())
}

// poolIsValid: si la bolsa junto con el atril todavia permite algun atril valido.
func (d *Game) poolIsValid() bool {
	pool := append(append([]tilemapping.MachineLetter{}, d.g.Bag().Peek()...), d.g.RackFor(masterIdx).TilesOn()...)
	return d.validTiles(pool)
}

func (d *Game) validTiles(tiles []tilemapping.MachineLetter) bool {
	minVowels, minConsonants := minimums(d.turn)
	vowels, consonants, blanks := countTiles(tiles, d.g.Bag().LetterDistribution())
	return vowels+blanks >= minVowels && consonants+blanks >= minConsonants
}

// FISF 3.2: al menos 2 vocales y 2 consonantes hasta el turno 15; despues, 1 y 1.
func minimums(turn int) (vowels, consonants int) {
	if turn > 15 {
		return 1, 1
	}
	return 2, 2
}

// El comodin (letra 0) cuenta aparte: sirve como vocal o como consonante.
func countTiles(tiles []tilemapping.MachineLetter, ld *tilemapping.LetterDistribution) (vowels, consonants, blanks int) {
	for _, t := range tiles {
		switch {
		case t == 0:
			blanks++
		case t.IsVowel(ld):
			vowels++
		default:
			consonants++
		}
	}
	return vowels, consonants, blanks
}

func copyMoves(moves []*move.Move) []*move.Move {
	out := make([]*move.Move, len(moves))
	for i, m := range moves {
		out[i] = new(move.Move)
		out[i].CopyFrom(m)
	}
	return out
}
