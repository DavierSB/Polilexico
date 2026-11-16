package main

import "time"

var hostGrace = time.Minute

const hostClosed = "El creador cerró la sala."

func (r *duplicateRoom) waitingDrop(s *seat) {
	if s != r.seats[0] {
		return
	}
	stopTimer(r.hostTimer)
	r.hostTimer = time.AfterFunc(hostGrace, r.hostNeverCame)
	r.broadcastRoom()
}

func (r *duplicateRoom) hostNeverCame() {
	r.mu.Lock()
	defer r.mu.Unlock()
	if r.hostAway() {
		r.closeAll(hostClosed)
	}
}

func (r *duplicateRoom) hostBack(s *seat) {
	if s == r.seats[0] {
		stopTimer(r.hostTimer)
	}
}

func (r *duplicateRoom) hostAway() bool {
	return r.table == nil && r.resumed == nil && !r.seats[0].online()
}

func (h *hub) waitingRoomOf(playerID string) string {
	r := h.roomOf(playerID)
	if r == nil {
		return ""
	}
	if l, ok := r.listing(); ok && l.seek.hostID == playerID {
		return l.seek.ID
	}
	return ""
}
