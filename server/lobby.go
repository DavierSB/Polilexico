package main

import (
	"sort"
	"strings"
)

type seek struct {
	ID         string `json:"id"`
	Mode       string `json:"mode"`
	Host       string `json:"host"`
	Country    string `json:"country"`
	TimeMs     int64  `json:"timeMs,omitempty"`
	OvertimeMs int64  `json:"overtimeMs,omitempty"`
	TurnMs     int64  `json:"turnMs,omitempty"`
	Rounds     int    `json:"rounds,omitempty"`
	Live       bool   `json:"live,omitempty"`
	Players    int    `json:"players"`
	Mine       bool   `json:"mine,omitempty"`
	hostID     string
}

type listing struct {
	seek    seek
	invitee string
}

type person struct {
	Name    string `json:"name"`
	Country string `json:"country"`
	Playing bool   `json:"playing,omitempty"`
}

func (h *hub) goOnline(c *client) {
	h.mu.Lock()
	defer h.mu.Unlock()
	h.online[c.player.ID] = c
}

func (h *hub) goOffline(c *client) {
	h.mu.Lock()
	defer h.mu.Unlock()
	if h.online[c.player.ID] == c {
		delete(h.online, c.player.ID)
	}
}

func (h *hub) findOnline(name, country string) *client {
	h.mu.Lock()
	defer h.mu.Unlock()
	for _, c := range h.online {
		if nameKey(c.player.Name) == nameKey(strings.TrimSpace(name)) && c.player.Country == country {
			return c
		}
	}
	return nil
}

func (h *hub) changed() {
	go h.broadcastLobby()
}

func (h *hub) broadcastLobby() {
	listings, people := h.listings(), h.people()
	for _, c := range h.onlineClients() {
		c.send(lobbyMessage(listings, people, c.player))
	}
}

func (h *hub) lobbyFor(p player) message {
	return lobbyMessage(h.listings(), h.people(), p)
}

func (h *hub) people() []person {
	h.mu.Lock()
	defer h.mu.Unlock()
	out := make([]person, 0, len(h.online))
	for id, c := range h.online {
		out = append(out, person{Name: c.player.Name, Country: c.player.Country, Playing: h.seats[id] != nil})
	}
	sort.Slice(out, func(i, j int) bool { return nameKey(out[i].Name) < nameKey(out[j].Name) })
	return out
}

func (h *hub) listings() []listing {
	h.mu.Lock()
	rooms := make([]room, 0, len(h.rooms))
	for _, r := range h.rooms {
		rooms = append(rooms, r)
	}
	h.mu.Unlock()
	var out []listing
	for _, r := range rooms {
		if l, ok := r.listing(); ok {
			out = append(out, l)
		}
	}
	return out
}

func (h *hub) onlineClients() []*client {
	h.mu.Lock()
	defer h.mu.Unlock()
	out := make([]*client, 0, len(h.online))
	for _, c := range h.online {
		out = append(out, c)
	}
	return out
}

func lobbyMessage(listings []listing, people []person, p player) message {
	m := message{T: "lobby", Seeks: []seek{}, Invites: []seek{}, People: others(people, p)}
	for _, l := range listings {
		s := l.seek
		s.Mine = s.hostID == p.ID
		switch {
		case l.invitee == "":
			m.Seeks = append(m.Seeks, s)
		case l.invitee == p.ID:
			m.Invites = append(m.Invites, s)
		}
	}
	return m
}

func others(people []person, p player) []person {
	out := []person{}
	for _, someone := range people {
		if someone.Name != p.Name || someone.Country != p.Country {
			out = append(out, someone)
		}
	}
	return out
}
