package main

import "slices"

type spectators struct {
	list []*client
}

func (s *spectators) add(c *client, r room) {
	if !slices.Contains(s.list, c) {
		s.list = append(s.list, c)
	}
	c.room = r
}

func (s *spectators) remove(c *client) bool {
	i := slices.Index(s.list, c)
	if i < 0 {
		return false
	}
	s.list = slices.Delete(s.list, i, i+1)
	c.room = nil
	return true
}

func (s *spectators) countFor(seats []*seat) {
	m := message{T: "watchers", Watchers: len(s.list), People: s.people()}
	for _, seated := range seats {
		seated.send(m)
	}
	s.send(m)
}

func (s *spectators) people() []person {
	out := []person{}
	for _, c := range s.list {
		p := person{Name: c.player.Name, Country: c.player.Country}
		if !slices.Contains(out, p) {
			out = append(out, p)
		}
	}
	return out
}

func (s *spectators) send(m message) {
	for _, c := range s.list {
		c.send(m)
	}
}

func (s *spectators) dismiss(reason string) {
	for _, c := range s.list {
		if reason != "" {
			c.send(notice("closed", reason))
		}
		c.room = nil
	}
	s.list = nil
}

func seatNames(seats []*seat) ([]string, []string) {
	names, countries := make([]string, len(seats)), make([]string, len(seats))
	for i, s := range seats {
		names[i], countries[i] = s.name, s.player.Country
	}
	return names, countries
}
