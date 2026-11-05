package main

import (
	"strconv"
	"time"
)

var adjudicateAfter = 3 * time.Minute

func (r *classicRoom) adjourn(s *seat) {
	if !r.inPlay() {
		return
	}
	other := r.rival(s)
	if !other.online() || r.asker == other {
		r.saveAndClose()
		return
	}
	r.asker = s
	other.send(message{T: "adjournAsk", Name: s.name})
	s.send(notice("rejected", "Propuesta enviada a "+other.name+"."))
}

func (r *classicRoom) replyAdjourn(s *seat, accept bool) {
	asker := r.asker
	if asker == nil || asker == s {
		return
	}
	r.asker = nil
	if accept {
		r.saveAndClose()
		return
	}
	asker.send(notice("rejected", s.name+" no quiere aplazar."))
}

func (r *classicRoom) saveAndClose() {
	text, err := r.match.Save()
	if err != nil {
		r.seats[0].send(notice("rejected", "No se pudo aplazar la partida."))
		r.seats[1].send(notice("rejected", "No se pudo aplazar la partida."))
		return
	}
	r.hub.keepAdjourned(r.adjournedAs(text))
	r.recorded = true
	r.closeAll("Partida aplazada.")
}

func (r *classicRoom) adjournedAs(save string) adjourned {
	a, b := r.seats[0], r.seats[1]
	return adjourned{
		ID: r.code + "-" + strconv.FormatInt(time.Now().Unix(), 36), PlayerA: a.player.ID, PlayerB: b.player.ID,
		NameA: a.name, NameB: b.name, CountryA: a.player.Country, CountryB: b.player.Country,
		TimeMs: r.timeMs, OvertimeMs: r.overtimeMs, Void: r.voidMode, Untimed: r.untimed, Save: save,
	}
}

func (r *classicRoom) adjudicate(s *seat) {
	if !r.inPlay() {
		return
	}
	other := r.rival(s)
	if other.online() || time.Since(other.awayAt) < adjudicateAfter {
		s.send(notice("rejected", "Aún no se puede adjudicar."))
		return
	}
	mine, theirs := r.scores(s)
	if other.leftAfterPlay {
		theirs -= r.lastPoints(other)
	}
	if mine > theirs {
		r.match.Forfeit(r.first(other))
		return
	}
	r.recorded = true
	r.closeAll("Partida anulada: no ibas por delante.")
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

func (r *classicRoom) scores(s *seat) (int, int) {
	st := r.match.Game().Status()
	if r.first(s) {
		return st.HumanScore, st.BotScore
	}
	return st.BotScore, st.HumanScore
}

func (r *classicRoom) lastPoints(s *seat) int {
	g := r.match.Game()
	for i := g.MoveCount() - 1; i >= 0; i-- {
		if m := g.MoveAt(i); m.ByHuman == r.first(s) {
			return m.Score
		}
	}
	return 0
}
