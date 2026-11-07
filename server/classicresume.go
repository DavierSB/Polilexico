package main

import (
	"lexico/engine/modes/classic"
)

type resumable interface {
	room
	resumeWith(clients map[string]*client)
}

func newResumedRoom(h *hub, code string, a adjourned) *classicRoom {
	m := message{TimeMs: a.TimeMs, OvertimeMs: a.OvertimeMs, Void: a.Void, Untimed: a.Untimed}
	r := newClassicRoom(h, code, player{ID: a.PlayerA, Name: a.NameA, Country: a.CountryA}, m, nil)
	r.resumed = &a
	r.seats = append(r.seats, &seat{player: player{ID: a.PlayerB, Name: a.NameB, Country: a.CountryB}, name: a.NameB})
	return r
}

func (r *classicRoom) resumeWith(clients map[string]*client) {
	r.mu.Lock()
	defer r.mu.Unlock()
	r.start()
	if r.match == nil {
		return
	}
	for _, s := range r.seats {
		r.hub.seat(s.player.ID, r)
		r.hub.matched(s.player.ID, r.code)
		s.awayAt = r.started
	}
	for _, s := range r.seats {
		if c := clients[s.player.ID]; c != nil {
			r.attach(s, c)
		}
	}
}

func (r *classicRoom) newMatch() (*classic.Match, error) {
	if r.resumed == nil {
		return classic.NewLocalMatch(r.seats[0].name, r.seats[1].name, classic.StarterAny, r.timeMs, r.overtimeMs, !r.voidMode, listener(r.changed))
	}
	m, err := classic.LoadMatch(r.resumed.Save, listener(r.changed))
	if err != nil {
		return nil, err
	}
	m.Resume()
	r.hub.forgetAdjourned(r.resumed.ID)
	return m, nil
}

func (h *hub) resume(c *client, id string) {
	a, ok := h.findAdjourned(id, c.player.ID)
	if !ok {
		c.send(notice("error", "Esa partida aplazada ya no está."))
		return
	}
	rivalID, rivalName, _ := a.opponentOf(c.player.ID)
	target := h.clientOf(rivalID)
	switch {
	case target == nil && !a.Untimed:
		c.send(notice("error", "Tu rival no está conectado."))
	case h.busy(rivalID):
		c.send(notice("error", rivalName+" está jugando otra partida."))
	case h.playing(c):
		c.send(notice("error", "Termina tu partida antes de retomar otra."))
	default:
		h.openResumed(a, c, target)
	}
}

func (h *hub) openResumed(a adjourned, clients ...*client) {
	present := map[string]*client{}
	for _, c := range clients {
		if c != nil {
			h.leaveCurrent(c)
			present[c.player.ID] = c
		}
	}
	h.mu.Lock()
	code := h.freeCode()
	r := newResumed(h, code, a)
	h.rooms[code] = r
	h.mu.Unlock()
	r.resumeWith(present)
	h.changed()
}

func newResumed(h *hub, code string, a adjourned) resumable {
	if a.Mode == modeDuplicate {
		return newResumedDuplicate(h, code, a)
	}
	return newResumedRoom(h, code, a)
}
