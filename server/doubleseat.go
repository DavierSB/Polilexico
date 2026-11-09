package main

import "log"

func (h *hub) settleSeat(playerID string, prev, r room) {
	keep := h.keepOne(prev, r)
	h.mu.Lock()
	if cur := h.seats[playerID]; cur == prev || cur == r {
		h.seats[playerID] = keep
	}
	h.mu.Unlock()
	h.moveClient(playerID, keep)
}

func (h *hub) keepOne(prev, r room) room {
	switch {
	case !prev.active():
		return r
	case !r.active():
		return prev
	case prev.forceAdjourn():
		return r
	case r.forceAdjourn():
		return prev
	}
	log.Printf("jugador en dos partidas que no se pueden aplazar")
	return r
}

func (h *hub) moveClient(playerID string, keep room) {
	c := h.clientOf(playerID)
	if c == nil || c.room == keep {
		return
	}
	if c.room != nil && !c.room.active() {
		h.leaveCurrent(c)
	}
	if c.room == nil {
		keep.enter(c)
	}
}

func (r *classicRoom) forceAdjourn() bool {
	r.mu.Lock()
	defer r.mu.Unlock()
	if r.recorded || !r.inPlay() {
		return false
	}
	r.saveAndClose(nil)
	return r.recorded
}

func (r *duplicateRoom) forceAdjourn() bool {
	r.mu.Lock()
	defer r.mu.Unlock()
	if r.recorded || !r.pair() {
		return false
	}
	r.saveAndClose(nil)
	return r.recorded
}
