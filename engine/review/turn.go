package review

type Turn struct {
	Number     int
	Player     string
	Rack       string
	Board      string
	candidates []*Move
	marks      []*Mark
}

type Move struct {
	Description string
	Score       int
	Equity      float64
	HasEquity   bool
}

type Mark struct {
	Who         string
	Description string
	Score       int
	Rank        int
}

func (t *Turn) CandidateCount() int {
	return len(t.candidates)
}

func (t *Turn) CandidateAt(i int) *Move {
	if i < 0 || i >= len(t.candidates) {
		return nil
	}
	return t.candidates[i]
}

func (t *Turn) MarkCount() int {
	return len(t.marks)
}

func (t *Turn) MarkAt(i int) *Mark {
	if i < 0 || i >= len(t.marks) {
		return nil
	}
	return t.marks[i]
}

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
