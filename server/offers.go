package main

func (h *hub) announceDuplicate(host *client, code string) {
	m := message{T: "duplicateOffer", Name: host.player.Name, Country: host.player.Country, Code: code}
	for _, c := range h.onlineClients() {
		if c != host && h.idle(c) {
			c.send(m)
		}
	}
}

func (h *hub) idle(c *client) bool {
	return c.room == nil && !h.busy(c.player.ID)
}
