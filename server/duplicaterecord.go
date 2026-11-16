package main

func (r *duplicateRoom) sendRecords() {
	for _, s := range r.seats {
		r.sendRecord(s.client, s.name)
	}
	for _, w := range r.watchers.list {
		r.sendRecord(w, "")
	}
}

func (r *duplicateRoom) sendRecord(c *client, player string) {
	if c == nil || c.version < recordProtocol {
		return
	}
	if rec, err := r.table.Record(player); err == nil {
		c.send(message{T: "record", Record: rec.Log, GCG: rec.GCG})
	}
}
