package main

import (
	"encoding/json"
	"strconv"
	"time"

	"lexico/engine/modes/duplicate"
)

func (r *duplicateRoom) pair() bool {
	return len(r.seats) == 2 && r.table != nil && !r.table.Finished()
}

func (r *duplicateRoom) rival(s *seat) *seat {
	if s == r.seats[0] {
		return r.seats[1]
	}
	return r.seats[0]
}

func (r *duplicateRoom) adjourn(s *seat) {
	if !r.pair() {
		return
	}
	other := r.rival(s)
	switch {
	case !other.online():
		r.saveAndClose(other)
	case r.asker == other:
		r.saveAndClose(nil)
	default:
		r.asker = s
		other.send(message{T: "adjournAsk", Name: s.name})
		s.send(notice("rejected", "Propuesta enviada a "+other.name+"."))
	}
}

func (r *duplicateRoom) replyAdjourn(s *seat, accept bool) {
	asker := r.asker
	if asker == nil || asker == s {
		return
	}
	r.asker = nil
	if accept {
		r.saveAndClose(nil)
		return
	}
	asker.send(notice("rejected", s.name+" no quiere aplazar."))
}

func (r *duplicateRoom) saveAndClose(responsible *seat) {
	a, ok := r.snapshot(responsible)
	if !ok {
		r.seats[0].send(notice("rejected", "No se pudo aplazar la partida."))
		r.seats[1].send(notice("rejected", "No se pudo aplazar la partida."))
		return
	}
	r.hub.keepAdjourned(a)
	r.recorded = true
	r.closeAll(adjournedReason)
}

func (r *duplicateRoom) snapshot(responsible *seat) (adjourned, bool) {
	text, err := r.table.Save()
	if err != nil {
		return adjourned{}, false
	}
	a, b := r.seats[0], r.seats[1]
	saved := adjourned{
		ID: r.code + "-" + strconv.FormatInt(time.Now().UnixNano(), 36), Mode: modeDuplicate, PlayerA: a.player.ID, PlayerB: b.player.ID,
		NameA: a.name, NameB: b.name, CountryA: a.player.Country, CountryB: b.player.Country,
		TimeMs: r.turnMs, OvertimeMs: int64(r.rounds), Void: r.live, Save: text,
	}
	if responsible != nil {
		saved.Responsible = responsible.player.ID
	}
	return saved, true
}

func (r *duplicateRoom) sendPresence() {
	if len(r.seats) != 2 || r.table == nil {
		return
	}
	for _, s := range r.seats {
		other := r.rival(s)
		s.send(message{T: "rival", Online: other.online(), AwayMs: awayMsOf(other)})
	}
}

func awayMsOf(s *seat) int64 {
	if s.online() || s.awayAt.IsZero() {
		return 0
	}
	return time.Since(s.awayAt).Milliseconds()
}

func adjudicateDuplicate(st store, a adjourned) string {
	var saved struct{ Totals map[string]int }
	if a.Responsible == "" || json.Unmarshal([]byte(a.Save), &saved) != nil {
		return outcomeNone
	}
	blamedFirst := a.Responsible == a.PlayerA
	blamed, other := saved.Totals[a.NameA], saved.Totals[a.NameB]
	if !blamedFirst {
		blamed, other = other, blamed
	}
	if other <= blamed {
		return outcomeNone
	}
	record(st, adjudicatedDuplicate(a, saved.Totals, !blamedFirst))
	if blamedFirst {
		return outcomeB
	}
	return outcomeA
}

func adjudicatedDuplicate(a adjourned, totals map[string]int, firstWins bool) result {
	win, loss := placing{Result: "win", Place: 1}, placing{Result: "loss", Place: 2}
	first, second := loss, win
	if firstWins {
		first, second = win, loss
	}
	first.UserID, first.Name, first.Score = a.PlayerA, a.NameA, totals[a.NameA]
	second.UserID, second.Name, second.Score = a.PlayerB, a.NameB, totals[a.NameB]
	first.Efficiency, second.Efficiency = efficiency(first.Score, totals[reservedName]), efficiency(second.Score, totals[reservedName])
	return result{Mode: modeDuplicate, StartedAt: a.createdAt(), Players: []placing{first, second},
		Settings: map[string]any{"turnMs": a.TimeMs, "rounds": a.OvertimeMs, "live": a.Void}}
}

func newResumedDuplicate(h *hub, code string, host player, a adjourned, rival player) *duplicateRoom {
	m := message{TurnMs: a.TimeMs, Rounds: int(a.OvertimeMs), Live: a.Void}
	r := newDuplicateRoom(h, code, host, m)
	r.resumed, r.invitee, r.inviteeTo = &a, rival.ID, rival.Name
	_, r.seats[0].name, _ = a.opponentOf(rival.ID)
	r.timer.Stop()
	r.timer = time.AfterFunc(resumeTimeout, func() {
		r.mu.Lock()
		defer r.mu.Unlock()
		if r.table == nil {
			r.refuseResume(r.inviteeTo + " no respondió.")
		}
	})
	return r
}

func (r *duplicateRoom) refuseResume(reason string) {
	r.hub.blame(r.resumed.ID, r.invitee)
	r.hub.notify(r.seats[0].player.ID, notice("closed", reason))
	r.closeAll("")
}

func (r *duplicateRoom) startResumed() {
	host, guest := r.seats[0], r.seats[1]
	_, guest.name, _ = r.resumed.opponentOf(host.player.ID)
	t, err := duplicate.LoadTable(r.resumed.Save, listener(r.changed))
	if err != nil {
		r.closeAll("No se pudo retomar la partida.")
		return
	}
	r.table, r.started = t, time.Now()
	r.timer.Stop()
	r.hub.forgetAdjourned(r.resumed.ID)
	r.hub.seat(host.player.ID, r)
	r.hub.matched(host.player.ID, r.code)
	if c := r.hub.clientOf(host.player.ID); c != nil {
		r.attach(host, c)
	}
	r.hub.changed()
}
