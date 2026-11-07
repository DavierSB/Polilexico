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
	case r.asker == other:
		r.saveAndClose(other)
	case !other.online() && awayMsOf(other) >= askAwayLimit.Milliseconds():
		r.saveAndClose(other)
	default:
		r.ask(s, other)
	}
}

func (r *duplicateRoom) ask(s, other *seat) {
	r.asker = s
	other.send(message{T: "adjournAsk", Name: s.name})
	if other.online() {
		s.send(notice("rejected", "Propuesta enviada a "+other.name+"."))
		return
	}
	s.send(notice("rejected", "Se aplaza si "+other.name+" no vuelve en un minuto."))
	r.acceptWhenAway(other)
}

func (r *duplicateRoom) acceptWhenAway(other *seat) {
	stopTimer(r.askTimer)
	wait := askAwayLimit - time.Duration(awayMsOf(other))*time.Millisecond
	r.askTimer = time.AfterFunc(wait, func() {
		r.mu.Lock()
		defer r.mu.Unlock()
		if !r.recorded && r.pair() && r.asker == r.rival(other) && !other.online() {
			r.saveAndClose(other)
		}
	})
}

func (r *duplicateRoom) replyAdjourn(s *seat, accept bool) {
	asker := r.asker
	if asker == nil || asker == s {
		return
	}
	r.asker = nil
	stopTimer(r.askTimer)
	if accept {
		r.saveAndClose(asker)
		return
	}
	asker.send(notice("rejected", s.name+" no quiere aplazar."))
}

func (r *duplicateRoom) awayChanged(s *seat) {
	if !r.pair() || r.asker != r.rival(s) {
		return
	}
	if !s.online() {
		r.acceptWhenAway(s)
		return
	}
	stopTimer(r.askTimer)
	s.send(message{T: "adjournAsk", Name: r.asker.name})
}

func (r *duplicateRoom) saveAndClose(responsible *seat) {
	a, ok := r.snapshot(responsible)
	if !ok {
		r.seats[0].send(notice("rejected", "No se pudo aplazar la partida."))
		r.seats[1].send(notice("rejected", "No se pudo aplazar la partida."))
		return
	}
	r.hub.keepAdjourned(a)
	r.hub.missAdjourned(r.seats)
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

func newResumedDuplicate(h *hub, code string, a adjourned) *duplicateRoom {
	m := message{TurnMs: a.TimeMs, Rounds: int(a.OvertimeMs), Live: a.Void}
	r := newDuplicateRoom(h, code, player{ID: a.PlayerA, Name: a.NameA, Country: a.CountryA}, m)
	r.resumed = &a
	r.seats[0].name = a.NameA
	r.seats = append(r.seats, &seat{player: player{ID: a.PlayerB, Name: a.NameB, Country: a.CountryB}, name: a.NameB})
	return r
}

func (r *duplicateRoom) resumeWith(clients map[string]*client) {
	r.mu.Lock()
	defer r.mu.Unlock()
	t, err := duplicate.LoadTable(r.resumed.Save, listener(r.changed))
	if err != nil {
		r.closeAll("No se pudo retomar la partida.")
		return
	}
	r.table, r.started = t, time.Now()
	r.timer.Stop()
	r.hub.forgetAdjourned(r.resumed.ID)
	for _, s := range r.seats {
		r.seatResumed(s, clients[s.player.ID])
	}
}

func (r *duplicateRoom) seatResumed(s *seat, c *client) {
	r.hub.seat(s.player.ID, r)
	r.hub.matched(s.player.ID, r.code)
	if c != nil {
		r.attach(s, c)
		return
	}
	s.awayAt = r.started
	r.setPresent(s, false)
	r.watchAbandon()
}
