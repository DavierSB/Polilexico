package main

import (
	"slices"
	"strings"
)

const reservedName = "Máster"

type seat struct {
	player player
	name   string
	client *client
}

func (s *seat) send(m message) {
	if s.client != nil {
		s.client.send(m)
	}
}

func (s *seat) online() bool {
	return s.client != nil
}

func displayName(p player, taken []string) string {
	clash := func(n string) bool { return strings.EqualFold(n, p.Name) }
	if slices.ContainsFunc(taken, clash) || strings.EqualFold(p.Name, reservedName) {
		return p.Name + " (" + p.Country + ")"
	}
	return p.Name
}

func outcomeResult(outcome string) string {
	switch outcome {
	case "win", "loss":
		return outcome
	}
	return "draw"
}
