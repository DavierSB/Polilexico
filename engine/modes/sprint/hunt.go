package sprint

import (
	"errors"
	"sync"

	"github.com/domino14/macondo/move"
	"github.com/domino14/macondo/movegen"

	"lexico/engine/internal/core"
)

var errStopped = errors.New("búsqueda detenida")

// hunter juega partidas de HastyBot contra si mismo y cuenta los turnos en que el jugador en
// turno podia colocar un scrabble; al llegar al objetivo, ese turno es el problema siguiente.
// Busca en su propio hilo, un problema por delante del que se esta resolviendo.
type hunter struct {
	pick     func() int
	found    chan found
	done     chan struct{}
	stopOnce sync.Once
	// La partida en curso y su generador de jugadas (HastyBot, por puntos).
	play *core.SelfPlay
	gen  *movegen.GordonGenerator
	// Turnos con scrabble posible contados, y a cuantos hay que llegar.
	count, target int
}

// found es lo que entrega la busqueda: el problema, o por que no se pudo buscar.
type found struct {
	puzzle *puzzle
	err    error
}

// startHunter empieza a buscar problemas; pick elige cada objetivo.
func startHunter(pick func() int) *hunter {
	h := &hunter{pick: pick, found: make(chan found), done: make(chan struct{}), target: pick()}
	go h.run()
	return h
}

// stop detiene la busqueda (se puede llamar mas de una vez).
func (h *hunter) stop() {
	h.stopOnce.Do(func() { close(h.done) })
}

// run busca problemas y los entrega de uno en uno, hasta stop o un error.
func (h *hunter) run() {
	for {
		p, err := h.next()
		select {
		case h.found <- found{p, err}:
		case <-h.done:
			return
		}
		if err != nil {
			return
		}
	}
}

// next juega turnos hasta dar con el problema siguiente.
func (h *hunter) next() (*puzzle, error) {
	for !h.stopped() {
		if p, err := h.step(); p != nil || err != nil {
			return p, err
		}
	}
	return nil, errStopped
}

// step mira el turno en curso (¿es el problema?) y lo juega.
func (h *hunter) step() (*puzzle, error) {
	if err := h.ensureGame(); err != nil {
		return nil, err
	}
	p := h.checkTurn()
	return p, h.playTurn()
}

// checkTurn cuenta el turno si admite algun scrabble; si con el llega al objetivo, lo devuelve
// como problema y elige un objetivo nuevo.
func (h *hunter) checkTurn() *puzzle {
	bingos := h.bingos()
	if len(bingos) == 0 {
		return nil
	}
	h.count++
	if h.count < h.target {
		return nil
	}
	h.count, h.target = 0, h.pick()
	return newPuzzle(h.play.Game, bingos)
}

// bingos: los scrabbles del jugador en turno (las jugadas con todas las fichas del atril).
func (h *hunter) bingos() []*move.Move {
	g := h.play.Game
	rack := g.RackFor(g.PlayerOnTurn())
	if rack.NumTiles() < bingoTiles {
		return nil
	}
	var out []*move.Move
	for _, m := range h.gen.GenAll(rack, false) {
		if isBingo(m) {
			out = append(out, core.CopyMove(m))
		}
	}
	return out
}

// playTurn juega la mejor jugada de HastyBot para el jugador en turno.
func (h *hunter) playTurn() error {
	return h.play.Play(h.play.Best())
}

// ensureGame empieza una partida nueva si no hay ninguna o la actual termino.
func (h *hunter) ensureGame() error {
	if h.play != nil && !h.play.Over() {
		return nil
	}
	return h.newGame()
}

func (h *hunter) newGame() error {
	play, err := core.NewSelfPlay()
	if err != nil {
		return err
	}
	gen, err := core.NewMaster(play.Game)
	if err != nil {
		return err
	}
	h.play, h.gen = play, gen
	return nil
}

func (h *hunter) stopped() bool {
	select {
	case <-h.done:
		return true
	default:
		return false
	}
}
