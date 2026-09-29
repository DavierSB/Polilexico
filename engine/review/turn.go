package review

// Turn es un turno de la partida: de quien era (en duplicada, ""), su atril ("A CH E ?"), el
// tablero antes de jugar (en el formato de engine.BestMoves), las mejores jugadas segun el motor
// y lo que se jugo de verdad.
type Turn struct {
	Number     int
	Player     string
	Rack       string
	Board      string
	candidates []*Move
	marks      []*Mark
}

// Move es una jugada del registro: Description es "H8 CASA (12 pts)", "(Pasar)"... Equity solo
// cuenta si HasEquity (la duplicada ordena por puntos, sin valoracion).
type Move struct {
	Description string
	Score       int
	Equity      float64
	HasEquity   bool
}

// Mark es lo que alguien jugo en el turno ("Tú", el bot, "Máster") y en que puesto de las
// mejores jugadas quedo (0 = la mejor; -1 = fuera de la lista).
type Mark struct {
	Who         string
	Description string
	Score       int
	Rank        int
}

func (t *Turn) CandidateCount() int {
	return len(t.candidates)
}

// CandidateAt devuelve la jugada i de las mejores (0 = la mejor), o nil.
func (t *Turn) CandidateAt(i int) *Move {
	if i < 0 || i >= len(t.candidates) {
		return nil
	}
	return t.candidates[i]
}

func (t *Turn) MarkCount() int {
	return len(t.marks)
}

// MarkAt devuelve lo que jugo alguien en el turno (en duplicada: 0 = el master, 1 = tu), o nil.
func (t *Turn) MarkAt(i int) *Mark {
	if i < 0 || i >= len(t.marks) {
		return nil
	}
	return t.marks[i]
}

// mark: lo que jugo `who`, con su puesto entre las candidatas.
func (t *Turn) mark(who string, played *Move) *Mark {
	rank := -1
	for i, c := range t.candidates {
		if c.Description == played.Description {
			rank = i
			break
		}
	}
	return &Mark{Who: who, Description: played.Description, Score: played.Score, Rank: rank}
}
