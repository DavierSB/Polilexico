package classic

import (
	"errors"
	"sync/atomic"

	"lexico/engine/events"
	"lexico/engine/internal/core"
)

const ModeEndgame = "endgame"

const (
	EndgameMaxBag  = 8
	EndgameMinLead = -40
	EndgameMaxLead = 0
	LeadLimit      = 200
)

var (
	errSearchStopped = errors.New("búsqueda detenida")
	errLeadRange     = errors.New("la ventaja mínima no puede superar a la máxima")
	errBagRange      = errors.New("el número de fichas en la bolsa no puede ser negativo")
)

type EndgameSearch struct {
	maxBag, minLead, maxLead int
	stopped                  atomic.Bool
}

func NewEndgameSearch(maxBag, minLead, maxLead int) *EndgameSearch {
	return &EndgameSearch{maxBag: maxBag, minLead: minLead, maxLead: maxLead}
}

func (s *EndgameSearch) Stop() {
	s.stopped.Store(true)
}

func (s *EndgameSearch) Match(timeMs, overtimeMs int64, invalidLosesTurn bool, l events.Listener) (*Match, error) {
	g, err := s.Find()
	if err != nil {
		return nil, err
	}
	return newMatch(g, timeMs, overtimeMs, invalidLosesTurn, l), nil
}

func (s *EndgameSearch) Find() (*Game, error) {
	if err := s.check(); err != nil {
		return nil, err
	}
	for !s.stopped.Load() {
		sim, err := s.simulate()
		if err != nil {
			return nil, err
		}
		if sim != nil {
			return sim.game()
		}
	}
	return nil, errSearchStopped
}

func (s *EndgameSearch) check() error {
	switch {
	case s.minLead > s.maxLead:
		return errLeadRange
	case s.maxBag < 0:
		return errBagRange
	}
	return core.Ready()
}

func (s *EndgameSearch) simulate() (*simulation, error) {
	sim, err := newSimulation()
	if err != nil {
		return nil, err
	}
	for !sim.reached(s.maxBag) {
		if sim.over() || s.stopped.Load() {
			return nil, nil
		}
		if err := sim.step(); err != nil {
			return nil, err
		}
	}
	return s.accept(sim), nil
}

func (s *EndgameSearch) accept(sim *simulation) *simulation {
	lead := sim.lead()
	if sim.over() || lead < s.minLead || lead > s.maxLead {
		return nil
	}
	return sim
}
