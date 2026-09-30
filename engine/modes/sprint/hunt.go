package sprint

import (
	"errors"
	"sync"

	"github.com/domino14/macondo/move"
	"github.com/domino14/macondo/movegen"

	"lexico/engine/internal/core"
)

var errStopped = errors.New("búsqueda detenida")

type hunter struct {
	pick          func() int
	found         chan found
	done          chan struct{}
	stopOnce      sync.Once
	play          *core.SelfPlay
	gen           *movegen.GordonGenerator
	count, target int
}

type found struct {
	puzzle *puzzle
	err    error
}

func startHunter(pick func() int) *hunter {
	h := &hunter{pick: pick, found: make(chan found), done: make(chan struct{}), target: pick()}
	go h.run()
	return h
}

func (h *hunter) stop() {
	h.stopOnce.Do(func() { close(h.done) })
}

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

func (h *hunter) next() (*puzzle, error) {
	for !h.stopped() {
		if p, err := h.step(); p != nil || err != nil {
			return p, err
		}
	}
	return nil, errStopped
}

func (h *hunter) step() (*puzzle, error) {
	if err := h.ensureGame(); err != nil {
		return nil, err
	}
	p := h.checkTurn()
	return p, h.playTurn()
}

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

func (h *hunter) playTurn() error {
	return h.play.Play(h.play.Best())
}

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
