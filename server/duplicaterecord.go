package main

import "lexico/engine/modes/duplicate"

func (r *duplicateRoom) sendRecords() {
	for _, s := range r.seats {
		r.sendRecord(s.client, s)
	}
	for _, w := range r.watchers.list {
		r.sendRecord(w, nil)
	}
}

func (r *duplicateRoom) sendRecord(c *client, s *seat) {
	if c == nil || c.version < recordProtocol {
		return
	}
	if rec, err := r.recordOf(s); err == nil {
		c.send(message{T: "record", Record: rec.Log, GCG: rec.GCG})
	}
}

func (r *duplicateRoom) recordOf(s *seat) (duplicate.TableRecord, error) {
	if s == nil {
		return r.table.Record("")
	}
	return r.table.RecordFor(s.name, s.player.Country, s.player.ID)
}
