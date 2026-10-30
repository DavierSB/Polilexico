package duplicate

import (
	"lexico/engine/internal/core"
)

type tableView struct {
	Phase       string
	Board       string
	Rack        string
	Unseen      string
	Round       int
	RemainingMs int64
	Proposal    *Attempt
	Ready       int
	Waiting     int
	IAmReady    bool
	Live        bool
	Players     []string
	Rounds      []*tableRound
}

func (t *Table) View(player string) (string, error) {
	t.mu.Lock()
	defer t.mu.Unlock()
	return core.JSON(t.viewFor(player))
}

func (t *Table) viewFor(player string) tableView {
	v := tableView{Phase: t.phase, Board: t.game.Board(), Unseen: t.game.Unseen(), Round: len(t.rounds),
		RemainingMs: -1, Proposal: t.attempts[player], IAmReady: t.ready[player], Live: t.live,
		Players: t.players, Rounds: t.roundsFor(player)}
	v.Ready, v.Waiting = t.readiness()
	if t.phase == TablePlaying {
		v.Rack, v.Round, v.RemainingMs = t.game.currentRack(), len(t.rounds)+1, max(t.turn.Left(), 0).Milliseconds()
	}
	return v
}

func (t *Table) roundsFor(player string) []*tableRound {
	if t.live || t.phase == TableFinished {
		return t.rounds
	}
	out := make([]*tableRound, len(t.rounds))
	for i, r := range t.rounds {
		out[i] = &tableRound{Number: r.Number, Rack: r.Rack, Plays: onlyOf(r.Plays, player)}
	}
	return out
}

func onlyOf(plays []*TablePlay, player string) []*TablePlay {
	var out []*TablePlay
	for _, p := range plays {
		if p.Name == masterName || p.Name == player {
			out = append(out, p)
		}
	}
	return out
}

func (t *Table) readiness() (ready, waiting int) {
	for _, p := range t.players {
		if t.absent[p] {
			continue
		}
		waiting++
		if t.ready[p] {
			ready++
		}
	}
	return ready, waiting
}
