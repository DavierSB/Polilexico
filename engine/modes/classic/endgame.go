package classic

import (
	"errors"
	"sync/atomic"

	"lexico/engine/events"
	"lexico/engine/internal/core"
)

// Finales: HastyBot juega contra si mismo y, la primera vez que la bolsa baja a maxBag fichas o
// menos, mira la ventaja del jugador en turno (sus puntos menos los del rival). Si esta entre
// minLead y maxLead, esa partida es la tuya: sigues con su atril contra HastyBot hasta el final,
// como en una clasica, con las jugadas anteriores en la planilla. Si no, juega otra.

// ModeEndgame marca las partidas de Finales: en su registro (gameLog.Mode) y en el nombre del
// archivo (endgame-<fecha>-log.json).
const ModeEndgame = "endgame"

// Valores por defecto de Finales y los limites de la ventaja.
const (
	EndgameMaxBag  = 8
	EndgameMinLead = -40
	EndgameMaxLead = 0
	LeadLimit      = 200 // la ventaja se elige entre -LeadLimit y LeadLimit
)

var (
	errSearchStopped = errors.New("búsqueda detenida")
	errLeadRange     = errors.New("la ventaja mínima no puede superar a la máxima")
	errBagRange      = errors.New("el número de fichas en la bolsa no puede ser negativo")
)

// EndgameSearch busca la partida de Finales. Find (o Match) bloquea hasta encontrarla; Stop la
// detiene desde otro hilo.
type EndgameSearch struct {
	maxBag, minLead, maxLead int
	stopped                  atomic.Bool
}

// NewEndgameSearch prepara la busqueda: el tope de fichas en la bolsa y la ventaja buscada.
func NewEndgameSearch(maxBag, minLead, maxLead int) *EndgameSearch {
	return &EndgameSearch{maxBag: maxBag, minLead: minLead, maxLead: maxLead}
}

// Stop detiene la busqueda: Find devuelve un error en cuanto acaba el turno que estaba jugando.
func (s *EndgameSearch) Stop() {
	s.stopped.Store(true)
}

// Match busca la partida y la pone en marcha contra HastyBot, como classic.NewMatch.
func (s *EndgameSearch) Match(timeMs, overtimeMs int64, invalidLosesTurn bool, l events.Listener) (*Match, error) {
	g, err := s.Find()
	if err != nil {
		return nil, err
	}
	return newMatch(g, timeMs, overtimeMs, invalidLosesTurn, l), nil
}

// Find juega partidas hasta dar con una que cumpla lo buscado y te la entrega en tu turno.
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

// simulate juega una partida hasta que la bolsa baja a maxBag; nil si no sirve (la ventaja no
// es la buscada, termino antes o se detuvo la busqueda).
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

// accept: sim si al llegar a la bolsa buscada sigue en juego con la ventaja buscada.
func (s *EndgameSearch) accept(sim *simulation) *simulation {
	lead := sim.lead()
	if sim.over() || lead < s.minLead || lead > s.maxLead {
		return nil
	}
	return sim
}
