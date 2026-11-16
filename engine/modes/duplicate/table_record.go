package duplicate

import (
	"errors"

	"lexico/engine/internal/core"
)

var errNotFinished = errors.New("la partida no ha terminado")

type TableRecord struct {
	Log string
	GCG string
}

func (t *Table) Record(player string) (TableRecord, error) {
	t.mu.Lock()
	defer t.mu.Unlock()
	if t.phase != TableFinished {
		return TableRecord{}, errNotFinished
	}
	return t.game.recordWith(t.playsOf(player))
}

func (t *Table) playsOf(player string) map[int]core.Play {
	if player == "" || player == masterName {
		return nil
	}
	out := map[int]core.Play{}
	for _, r := range t.rounds {
		for _, p := range r.Plays {
			if p.Name == player {
				out[r.Number] = p.play()
			}
		}
	}
	return out
}

func (p *TablePlay) play() core.Play {
	return core.Play{Kind: p.Kind, Coords: p.Coords, Tiles: p.Tiles, TileCount: p.TileCount, Score: p.Points}
}

func (d *Game) recordWith(plays map[int]core.Play) (TableRecord, error) {
	d.mu.Lock()
	defer d.mu.Unlock()
	log, err := core.JSON(d.logWith(plays))
	if err != nil {
		return TableRecord{}, err
	}
	gcg, err := core.GCG(d.g)
	return TableRecord{Log: log, GCG: gcg}, err
}

func (d *Game) logWith(plays map[int]core.Play) gameLog {
	out := gameLog{StartedAt: d.log.StartedAt, FinalMaster: d.masterTotal}
	for _, r := range d.log.Turns {
		out.Turns = append(out.Turns, out.withPlay(r, playOrMaster(plays, r)))
	}
	return out
}

func playOrMaster(plays map[int]core.Play, r turnRecord) core.Play {
	if plays == nil {
		return core.Play{Kind: core.KindPlay, Coords: r.MasterPlay.Coords, Score: r.MasterPlay.Score}
	}
	if p, ok := plays[r.TurnNum]; ok {
		return p
	}
	return core.Play{Kind: core.KindTimeout}
}

func (l *gameLog) withPlay(r turnRecord, play core.Play) turnRecord {
	r.HumanPlay, r.HumanScore, r.HumanStatus = core.CandidateOf(play, 0), play.Score, play.Kind
	if matchesMaster(play, r.MasterPlay) {
		r.HumanPlay = r.MasterPlay
	}
	r.Hit = play.Kind == core.KindPlay && play.Score == r.MasterPlay.Score
	l.FinalHuman += play.Score
	if r.Hit {
		l.FinalHits++
	}
	r.HumanTotal, r.Hits = l.FinalHuman, l.FinalHits
	return r
}

func matchesMaster(play core.Play, master core.Candidate) bool {
	if play.Kind != core.KindPlay || play.Coords != master.Coords || play.Score != master.Score {
		return false
	}
	return play.Tiles == "" || core.SameCandidate(core.CandidateOf(play, 0), master)
}
