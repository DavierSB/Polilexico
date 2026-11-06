package main

import (
	"context"
	"log"
	"time"
)

func (h *hub) adjournAll() int {
	saved := 0
	for _, r := range h.allRooms() {
		if cr, ok := r.(*classicRoom); ok && h.saveNow(cr) {
			saved++
		}
	}
	return saved
}

func (h *hub) allRooms() []room {
	h.mu.Lock()
	defer h.mu.Unlock()
	rooms := make([]room, 0, len(h.rooms))
	for _, r := range h.rooms {
		rooms = append(rooms, r)
	}
	return rooms
}

func (h *hub) saveNow(r *classicRoom) bool {
	r.mu.Lock()
	defer r.mu.Unlock()
	if !r.inPlay() {
		return false
	}
	a, ok := r.snapshot(nil)
	if !ok {
		return false
	}
	a.CreatedAt = time.Now().UTC().Format(time.RFC3339)
	ctx, cancel := context.WithTimeout(context.Background(), 20*time.Second)
	defer cancel()
	if err := h.store.SaveAdjourned(ctx, a); err != nil {
		log.Printf("aplazar al apagar %s: %v", a.ID, err)
		return false
	}
	r.recorded = true
	r.closeAll(adjournedReason)
	return true
}
