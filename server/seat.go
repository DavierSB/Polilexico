package main

import (
	"slices"
	"strings"
	"time"
)

const reservedName = "Máster"

type seat struct {
	player player
	name   string
	client *client
	chat   chatState
	awayAt time.Time
}

func (s *seat) send(m message) {
	if s.client != nil {
		s.client.send(m)
	}
}

func (s *seat) online() bool {
	return s.client != nil
}

func firstAway(seats []*seat) *seat {
	var first *seat
	for _, s := range seats {
		if first == nil || s.awayAt.Before(first.awayAt) {
			first = s
		}
	}
	return first
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
