package main

import "time"

var abandonAfter = 5 * time.Minute

func (r *duplicateRoom) watchAbandon() {
	if r.table == nil || r.table.Finished() || r.anyoneHere() {
		return
	}
	r.stopAbandon()
	r.idle = time.AfterFunc(abandonAfter, r.abandoned)
}

func (r *duplicateRoom) stopAbandon() {
	if r.idle != nil {
		r.idle.Stop()
	}
}

func (r *duplicateRoom) abandoned() {
	r.mu.Lock()
	defer r.mu.Unlock()
	if r.table == nil || r.table.Finished() || r.anyoneHere() {
		return
	}
	if r.pair() {
		r.saveAndClose(firstAway(r.seats))
		return
	}
	r.recorded = true
	r.closeAll("La partida se cerró: no quedaba nadie.")
}

func (r *duplicateRoom) anyoneHere() bool {
	for _, s := range r.seats {
		if s.online() {
			return true
		}
	}
	return false
}
