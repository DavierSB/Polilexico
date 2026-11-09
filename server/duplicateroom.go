package main

import (
	"strings"
	"sync"
	"time"

	"lexico/engine/modes/duplicate"
)

const maxDuplicatePlayers = 12

type duplicateRoom struct {
	mu       sync.Mutex
	hub      *hub
	code     string
	turnMs   int64
	rounds   int
	live     bool
	seats    []*seat
	table    *duplicate.Table
	started  time.Time
	recorded bool
	timer    *time.Timer
	watchers spectators
	asker    *seat
	askTimer *time.Timer
	resumed  *adjourned
	idle     *time.Timer
}

func newDuplicateRoom(h *hub, code string, host player, m message) *duplicateRoom {
	r := &duplicateRoom{hub: h, code: code, turnMs: clamp(m.TurnMs, 30_000, 10*60_000, 3*60_000),
		rounds: int(clamp(int64(m.Rounds), 0, 99, 0)), live: m.Live, seats: []*seat{{player: host, name: displayName(host, nil)}}}
	r.expireIn(waitingTTL)
	return r
}

func (r *duplicateRoom) enter(c *client) {
	r.mu.Lock()
	defer r.mu.Unlock()
	s := r.seatOf(c.player.ID)
	if s == nil {
		if s = r.admit(c); s == nil {
			return
		}
	}
	r.attach(s, c)
}

func (r *duplicateRoom) handle(c *client, m message) {
	r.mu.Lock()
	defer r.mu.Unlock()
	s := r.seatOf(c.player.ID)
	if s == nil || s.client != c {
		if m.T == "leave" && r.watchers.remove(c) {
			r.watchers.countFor(r.seats)
		}
		return
	}
	switch m.T {
	case "start":
		r.start(s)
	case "move":
		r.propose(s, m.Input)
	case "ready":
		r.ready(s)
	case "chat":
		relayChat(s, r.seats, m.Text)
	case "leave":
		r.leave(s)
	case "adjourn":
		r.adjourn(s)
	case "adjournReply":
		r.replyAdjourn(s, m.Accept)
	}
}

func (r *duplicateRoom) drop(c *client) {
	r.mu.Lock()
	defer r.mu.Unlock()
	if s := r.seatOf(c.player.ID); s != nil && s.client == c {
		s.client = nil
		s.awayAt = time.Now()
		r.setPresent(s, false)
		r.awayChanged(s)
		r.sendPresence()
		r.watchAbandon()
		return
	}
	if r.watchers.remove(c) {
		r.watchers.countFor(r.seats)
	}
}

func (r *duplicateRoom) ongoing() (liveGame, bool) {
	r.mu.Lock()
	defer r.mu.Unlock()
	if r.table == nil || r.table.Finished() {
		return liveGame{}, false
	}
	names, countries := seatNames(r.seats)
	return r.progress(liveGame{ID: r.code, Mode: modeDuplicate, Players: names, Countries: countries, Watchers: len(r.watchers.list)}), true
}

func (r *duplicateRoom) watch(c *client) bool {
	r.mu.Lock()
	defer r.mu.Unlock()
	if r.table == nil || r.table.Finished() {
		return false
	}
	r.watchers.add(c, r)
	r.watchers.countFor(r.seats)
	m := r.roomMessage(r.seats[0])
	m.Name, m.First, m.Watching = c.player.Name, false, true
	c.send(m)
	r.sendSpectatorView()
	return true
}

func (r *duplicateRoom) sendSpectatorView() {
	if view, err := r.table.SpectatorView(); err == nil {
		r.watchers.send(message{T: "table", View: view})
	}
}

func (r *duplicateRoom) decline(*client) {}

func (r *duplicateRoom) cancel(playerID, reason string) {
	r.mu.Lock()
	defer r.mu.Unlock()
	if r.table == nil && r.seats[0].player.ID == playerID {
		r.closeAll(reason)
	}
}

func (r *duplicateRoom) startBy(c *client) {
	r.mu.Lock()
	defer r.mu.Unlock()
	host := r.seats[0]
	switch {
	case host.player.ID != c.player.ID || r.table != nil:
		return
	case len(r.seats) < 2:
		c.send(notice("error", "Espera a que entre al menos otro jugador."))
		return
	}
	r.hub.seat(host.player.ID, r)
	r.hub.matched(host.player.ID, r.code)
	host.client, c.room = c, r
	c.send(r.roomMessage(host))
	r.start(host)
}

func (r *duplicateRoom) active() bool {
	r.mu.Lock()
	defer r.mu.Unlock()
	if r.table == nil {
		return false
	}
	_, over := r.finalView()
	return !over
}

func (r *duplicateRoom) finished() bool {
	r.mu.Lock()
	defer r.mu.Unlock()
	if r.table == nil {
		return false
	}
	_, over := r.finalView()
	return over
}

func (r *duplicateRoom) listing() (listing, bool) {
	r.mu.Lock()
	defer r.mu.Unlock()
	if r.table != nil || r.resumed != nil {
		return listing{}, false
	}
	host := r.seats[0]
	return listing{seek: seek{ID: r.code, Mode: modeDuplicate, Host: host.name, Country: host.player.Country, TurnMs: r.turnMs,
		Rounds: r.rounds, Live: r.live, Players: len(r.seats), hostID: host.player.ID}}, true
}

func (r *duplicateRoom) admit(c *client) *seat {
	switch {
	case r.table != nil:
		c.send(notice("error", "La partida de esa sala ya empezó."))
		return nil
	case len(r.seats) >= maxDuplicatePlayers:
		c.send(notice("error", "La sala ya está completa."))
		return nil
	}
	s := &seat{player: c.player, name: displayName(c.player, r.names())}
	r.seats = append(r.seats, s)
	r.hub.seat(c.player.ID, r)
	return s
}

func (r *duplicateRoom) attach(s *seat, c *client) {
	if s.client != nil && s.client != c {
		s.client.send(notice("closed", "Entraste desde otro dispositivo."))
		s.client.room = nil
	}
	s.client, c.room = c, r
	c.send(r.roomMessage(s))
	if r.table == nil {
		r.broadcastRoom()
		return
	}
	s.awayAt = time.Time{}
	r.stopAbandon()
	r.sendView(s)
	r.setPresent(s, true)
	r.awayChanged(s)
	r.sendPresence()
	s.send(message{T: "watchers", Watchers: len(r.watchers.list)})
}

func (r *duplicateRoom) start(s *seat) {
	if s != r.seats[0] || r.table != nil || len(r.seats) < 2 {
		return
	}
	t, err := duplicate.NewTable(strings.Join(r.names(), "\n"), r.turnMs, r.rounds, r.live, listener(r.changed))
	if err != nil {
		r.closeAll("No se pudo empezar la partida.")
		return
	}
	r.table, r.started = t, time.Now()
	r.timer.Stop()
	r.hub.changed()
	for _, seated := range r.seats {
		r.sendView(seated)
	}
}

func (r *duplicateRoom) changed() {
	r.mu.Lock()
	defer r.mu.Unlock()
	if r.table == nil {
		return
	}
	for _, s := range r.seats {
		r.sendView(s)
	}
	r.sendSpectatorView()
	if final, ok := r.finalView(); ok {
		r.finish(final)
	}
}

func (r *duplicateRoom) propose(s *seat, input string) {
	if r.table == nil {
		return
	}
	if _, err := r.table.Propose(s.name, input); err != nil {
		s.send(notice("rejected", err.Error()))
	}
}

func (r *duplicateRoom) ready(s *seat) {
	if r.table != nil {
		r.table.Ready(s.name)
	}
}

func (r *duplicateRoom) leave(s *seat) {
	if s.client != nil {
		s.client.room = nil
	}
	s.client = nil
	r.hub.unseat(s.player.ID, r)
	if r.table != nil {
		r.setPresent(s, false)
		r.watchAbandon()
		return
	}
	if s == r.seats[0] {
		r.closeAll("El anfitrión cerró la sala.")
		return
	}
	r.seats = without(r.seats, s)
	r.broadcastRoom()
	r.hub.changed()
}

func (r *duplicateRoom) setPresent(s *seat, present bool) {
	if r.table != nil {
		r.table.SetPresent(s.name, present)
	}
}

func (r *duplicateRoom) finish(final tableSummary) {
	if r.recorded {
		return
	}
	r.recorded = true
	record(r.hub.store, duplicateResult(r, final))
	r.expireIn(finishedTTL)
	r.hub.changed()
}

func (r *duplicateRoom) expireIn(d time.Duration) {
	if r.timer != nil {
		r.timer.Stop()
	}
	r.timer = time.AfterFunc(d, func() {
		r.mu.Lock()
		defer r.mu.Unlock()
		r.closeAll("La sala se cerró.")
	})
}

func (r *duplicateRoom) closeAll(reason string) {
	stopTimer(r.askTimer)
	for _, s := range r.seats {
		s.send(notice("closed", reason))
		if s.client != nil {
			s.client.room = nil
		}
		s.client = nil
	}
	r.watchers.dismiss(reason)
	r.hub.release(r.code, r)
	if r.table != nil {
		r.table.Close()
	}
}

func (r *duplicateRoom) sendView(s *seat) {
	if view, err := r.table.View(s.name); err == nil {
		s.send(message{T: "table", View: view})
	}
}

func (r *duplicateRoom) broadcastRoom() {
	for _, s := range r.seats {
		s.send(r.roomMessage(s))
	}
}

func (r *duplicateRoom) roomMessage(s *seat) message {
	return message{T: "room", Mode: modeDuplicate, Code: r.code, Name: s.name, Host: r.seats[0].name, Players: r.names(),
		First: s == r.seats[0], TurnMs: r.turnMs, Rounds: r.rounds, Live: r.live}
}

func (r *duplicateRoom) names() []string {
	names := make([]string, len(r.seats))
	for i, s := range r.seats {
		names[i] = s.name
	}
	return names
}

func (r *duplicateRoom) seatOf(playerID string) *seat {
	for _, s := range r.seats {
		if s.player.ID == playerID {
			return s
		}
	}
	return nil
}

func without(seats []*seat, gone *seat) []*seat {
	var out []*seat
	for _, s := range seats {
		if s != gone {
			out = append(out, s)
		}
	}
	return out
}
