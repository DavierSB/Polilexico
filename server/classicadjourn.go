package main

import (
	"log"
	"strconv"
	"time"

	"lexico/engine/modes/classic"
)

var (
	askAwayLimit = time.Minute
	vacantLimit  = 3 * time.Minute
)

func (r *classicRoom) adjourn(s *seat) {
	if !r.inPlay() {
		return
	}
	other := r.rival(s)
	switch {
	case r.asker == other:
		r.saveAndClose(other)
	case !other.online() && r.awayMs(other) >= askAwayLimit.Milliseconds():
		r.saveAndClose(other)
	default:
		r.ask(s, other)
	}
}

func (r *classicRoom) ask(s, other *seat) {
	r.asker = s
	other.send(message{T: "adjournAsk", Name: s.name})
	if other.online() {
		s.send(notice("rejected", "Propuesta enviada a "+other.name+"."))
		return
	}
	s.send(notice("rejected", "Se aplaza si "+other.name+" no vuelve en un minuto."))
	r.acceptWhenAway(other)
}

func (r *classicRoom) acceptWhenAway(other *seat) {
	stopTimer(r.askTimer)
	wait := askAwayLimit - time.Duration(r.awayMs(other))*time.Millisecond
	r.askTimer = time.AfterFunc(wait, func() {
		r.mu.Lock()
		defer r.mu.Unlock()
		if !r.recorded && r.asker == r.rival(other) && !other.online() {
			r.saveAndClose(other)
		}
	})
}

func (r *classicRoom) replyAdjourn(s *seat, accept bool) {
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

func (r *classicRoom) awayChanged(s *seat) {
	if s.online() {
		r.cameBack(s)
		return
	}
	if r.asker == r.rival(s) {
		r.acceptWhenAway(s)
	}
	if r.inPlay() && r.nobodyLeft() {
		stopTimer(r.vacantTimer)
		r.vacantTimer = time.AfterFunc(vacantLimit, r.adjournVacant)
	}
}

func (r *classicRoom) cameBack(s *seat) {
	stopTimer(r.vacantTimer)
	if r.asker == r.rival(s) {
		stopTimer(r.askTimer)
		s.send(message{T: "adjournAsk", Name: r.asker.name})
	}
}

func (r *classicRoom) adjournVacant() {
	r.mu.Lock()
	defer r.mu.Unlock()
	if !r.recorded && r.inPlay() && r.nobodyLeft() {
		r.saveAndClose(firstAway(r.seats))
	}
}

func (r *classicRoom) saveAndClose(responsible *seat) {
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

const adjournedReason = "Partida aplazada."

func (r *classicRoom) snapshot(responsible *seat) (adjourned, bool) {
	text, err := r.match.Save()
	if err != nil {
		return adjourned{}, false
	}
	a := r.adjournedAs(text)
	if responsible != nil {
		a.Responsible, a.AfterPlay = responsible.player.ID, r.playedLast(responsible)
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

func (r *classicRoom) playedLast(s *seat) bool {
	return r.match.Game().Status().HumanToMove != r.first(s)
}

func (r *classicRoom) awayMs(s *seat) int64 {
	return awayMsOf(s)
}

func stopTimer(t *time.Timer) {
	if t != nil {
		t.Stop()
	}
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
	if a.Mode == modeDuplicate {
		return adjudicateDuplicate(st, a)
	}
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
