package main

import (
	"context"
	"log"
	"time"
)

func (h *hub) adjournAll() int {
	saved := 0
	for _, r := range h.allRooms() {
		if h.saveRoomNow(r) {
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

func (h *hub) saveRoomNow(r room) bool {
	switch room := r.(type) {
	case *classicRoom:
		room.mu.Lock()
		defer room.mu.Unlock()
		if !room.inPlay() || !h.storeNow(room.snapshot(nil)) {
			return false
		}
		room.recorded = true
		room.closeAll(adjournedReason)
	case *duplicateRoom:
		room.mu.Lock()
		defer room.mu.Unlock()
		if !room.pair() || !h.storeNow(room.snapshot(nil)) {
			return false
		}
		room.recorded = true
		room.closeAll(adjournedReason)
	default:
		return false
	}
	return true
}

func (h *hub) storeNow(a adjourned, ok bool) bool {
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
	return true
}
