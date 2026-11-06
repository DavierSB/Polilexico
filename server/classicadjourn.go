package main

import (
	"log"
	"strconv"
	"time"

	"lexico/engine/modes/classic"
)

func (r *classicRoom) adjourn(s *seat) {
	if !r.inPlay() {
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

func (r *classicRoom) replyAdjourn(s *seat, accept bool) {
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

func (r *classicRoom) saveAndClose(responsible *seat) {
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

const adjournedReason = "Partida aplazada."

func (r *classicRoom) snapshot(responsible *seat) (adjourned, bool) {
	text, err := r.match.Save()
	if err != nil {
		return adjourned{}, false
	}
	a := r.adjournedAs(text)
	if responsible != nil {
		a.Responsible, a.AfterPlay = responsible.player.ID, responsible.leftAfterPlay
	}
	return a, true
}

func (r *classicRoom) adjournedAs(save string) adjourned {
	a, b := r.seats[0], r.seats[1]
	return adjourned{
		ID: r.code + "-" + strconv.FormatInt(time.Now().UnixNano(), 36), Mode: modeClassic, PlayerA: a.player.ID, PlayerB: b.player.ID,
		NameA: a.name, NameB: b.name, CountryA: a.player.Country, CountryB: b.player.Country,
		TimeMs: r.timeMs, OvertimeMs: r.overtimeMs, Void: r.voidMode, Untimed: r.untimed, Save: save,
	}
}

func (r *classicRoom) markAway(s *seat) {
	s.awayAt = time.Now()
	s.leftAfterPlay = r.inPlay() && r.match.Game().Status().HumanToMove != r.first(s)
}

func (r *classicRoom) awayMs(s *seat) int64 {
	if s.online() || s.awayAt.IsZero() {
		return 0
	}
	return time.Since(s.awayAt).Milliseconds()
}

func (r *classicRoom) inPlay() bool {
	return r.match != nil && len(r.seats) == 2 && !r.match.Game().Status().Over
}

func (r *classicRoom) rival(s *seat) *seat {
	if s == r.seats[0] {
		return r.seats[1]
	}
	return r.seats[0]
}

func adjudicateSaved(st store, a adjourned) string {
	if a.Responsible == "" {
		return outcomeNone
	}
	m, err := classic.LoadMatch(a.Save, listener(func() {}))
	if err != nil {
		log.Printf("adjudicar %s: %v", a.ID, err)
		return outcomeNone
	}
	defer m.Close()
	blamedFirst := a.Responsible == a.PlayerA
	if !otherAhead(m.Game(), blamedFirst, a.AfterPlay) {
		return outcomeNone
	}
	m.Forfeit(blamedFirst)
	record(st, classicResult(settledRoom(a, m)))
	if blamedFirst {
		return outcomeB
	}
	return outcomeA
}

func otherAhead(g *classic.Game, blamedFirst, afterPlay bool) bool {
	st := g.Status()
	blamed, other := st.HumanScore, st.BotScore
	if !blamedFirst {
		blamed, other = other, blamed
	}
	if afterPlay {
		blamed -= lastPoints(g, blamedFirst)
	}
	return other > blamed
}

func lastPoints(g *classic.Game, first bool) int {
	for i := g.MoveCount() - 1; i >= 0; i-- {
		if m := g.MoveAt(i); m.ByHuman == first {
			return m.Score
		}
	}
	return 0
}

func settledRoom(a adjourned, m *classic.Match) *classicRoom {
	seatA := &seat{player: player{ID: a.PlayerA, Name: a.NameA, Country: a.CountryA}, name: a.NameA}
	seatB := &seat{player: player{ID: a.PlayerB, Name: a.NameB, Country: a.CountryB}, name: a.NameB}
	return &classicRoom{match: m, seats: []*seat{seatA, seatB}, started: a.createdAt(), timeMs: a.TimeMs, overtimeMs: a.OvertimeMs}
}
