package main

import (
	"slices"
	"strings"
	"time"
)

func (r *duplicateRoom) handleWatcher(c *client, m message) {
	switch {
	case m.T == "leave" && r.watchers.remove(c):
		r.watchers.countFor(r.seats)
	case m.T == "chat" && slices.Contains(r.watchers.list, c):
		r.tableChat(c, &c.chat, c.player.Name, m.Text)
	}
}

func (r *duplicateRoom) tableChat(from *client, state *chatState, name, text string) {
	text = strings.TrimSpace(text)
	if reason := chatRefusal(state, text); reason != "" {
		from.send(notice("error", reason))
		return
	}
	state.last, state.unanswered = time.Now(), state.unanswered+1
	r.broadcastChat(from, message{T: "chat", Name: name, Text: text})
}

func (r *duplicateRoom) broadcastChat(from *client, m message) {
	for _, s := range r.seats {
		if s.client != from {
			s.chat.unanswered = 0
			s.send(m)
		}
	}
	for _, w := range r.watchers.list {
		if w != from {
			w.chat.unanswered = 0
			w.send(m)
		}
	}
}
