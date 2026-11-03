package main

import (
	"sync"
	"time"

	"lexico/engine/modes/classic"
)

const (
	waitingTTL  = 30 * time.Minute
	finishedTTL = 10 * time.Minute
)

type classicRoom struct {
	mu         sync.Mutex
	hub        *hub
	code       string
	timeMs     int64
	overtimeMs int64
	seats      []*seat
	match      *classic.Match
	started    time.Time
	recorded   bool
	timer      *time.Timer
	invitee    string
}

type listener func()

func (l listener) OnChange() { go l() }

func newClassicRoom(h *hub, code string, host player, m message, invitee string) *classicRoom {
	r := &classicRoom{hub: h, code: code, timeMs: clamp(m.TimeMs, 60_000, 90*60_000, 25*60_000),
		overtimeMs: clamp(m.OvertimeMs, 0, 30*60_000, 10*60_000), seats: []*seat{{player: host, name: host.Name}}, invitee: invitee}
	r.expireIn(waitingTTL)
	return r
}

func (r *classicRoom) enter(c *client) {
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

func (r *classicRoom) handle(c *client, m message) {
	r.mu.Lock()
	defer r.mu.Unlock()
	s := r.seatOf(c.player.ID)
	if s == nil || s.client != c {
		return
	}
	switch m.T {
	case "move":
		r.play(s, m.Input)
	case "leave":
		r.leave(s)
	}
}

func (r *classicRoom) drop(c *client) {
	r.mu.Lock()
	defer r.mu.Unlock()
	if s := r.seatOf(c.player.ID); s != nil && s.client == c {
		s.client = nil
		r.sendPresence()
	}
}

func (r *classicRoom) decline(c *client) {
	r.mu.Lock()
	defer r.mu.Unlock()
	if r.match == nil && c.player.ID == r.invitee {
		r.closeAll(c.player.Name + " rechazó el reto.")
	}
}

func (r *classicRoom) listing() (listing, bool) {
	r.mu.Lock()
	defer r.mu.Unlock()
	if r.match != nil {
		return listing{}, false
	}
	host := r.seats[0]
	return listing{seek: seek{ID: r.code, Mode: modeClassic, Host: host.name, Country: host.player.Country, TimeMs: r.timeMs,
		OvertimeMs: r.overtimeMs, Players: len(r.seats), hostID: host.player.ID}, invitee: r.invitee}, true
}

func (r *classicRoom) admit(c *client) *seat {
	switch {
	case len(r.seats) == 2:
		c.send(notice("error", "Esa partida ya empezó."))
		return nil
	case r.invitee != "" && c.player.ID != r.invitee:
		c.send(notice("error", "Ese reto es para otro jugador."))
		return nil
	}
	s := &seat{player: c.player, name: displayName(c.player, []string{r.seats[0].name})}
	r.seats = append(r.seats, s)
	r.hub.seat(c.player.ID, r)
	r.start()
	return s
}

func (r *classicRoom) attach(s *seat, c *client) {
	if s.client != nil && s.client != c {
		s.client.send(notice("closed", "Entraste desde otro dispositivo."))
		s.client.room = nil
	}
	s.client, c.room = c, r
	c.send(r.roomMessage(s))
	if r.match != nil {
		r.sendState(s)
	}
	r.sendPresence()
}

func (r *classicRoom) start() {
	m, err := classic.NewLocalMatch(r.seats[0].name, r.seats[1].name, classic.StarterAny, r.timeMs, r.overtimeMs, true, listener(r.changed))
	if err != nil {
		r.closeAll("No se pudo empezar la partida.")
		return
	}
	r.match, r.started = m, time.Now()
	r.timer.Stop()
}

func (r *classicRoom) changed() {
	r.mu.Lock()
	defer r.mu.Unlock()
	if r.match == nil {
		return
	}
	for _, s := range r.seats {
		r.sendState(s)
	}
	if r.match.Game().Status().Over {
		r.finish()
	}
}

func (r *classicRoom) play(s *seat, input string) {
	if r.match == nil {
		return
	}
	status := r.match.Game().Status()
	if status.Over {
		return
	}
	if status.HumanToMove != r.first(s) {
		s.send(notice("rejected", "No es tu turno."))
		return
	}
	if _, err := r.match.Play(input); err != nil {
		s.send(notice("rejected", err.Error()))
	}
}

func (r *classicRoom) leave(s *seat) {
	if r.match != nil {
		r.match.Forfeit(r.first(s))
	}
	if s.client != nil {
		s.client.room = nil
	}
	s.client = nil
	r.hub.unseat(s.player.ID, r)
	switch {
	case r.match == nil:
		r.closeAll("El anfitrión cerró la sala.")
	case r.nobodyLeft():
		r.closeAll("")
	default:
		r.sendPresence()
	}
}

func (r *classicRoom) finish() {
	if r.recorded {
		return
	}
	r.recorded = true
	record(r.hub.store, classicResult(r))
	r.expireIn(finishedTTL)
}

func (r *classicRoom) expireIn(d time.Duration) {
	if r.timer != nil {
		r.timer.Stop()
	}
	r.timer = time.AfterFunc(d, func() {
		r.mu.Lock()
		defer r.mu.Unlock()
		r.closeAll("La sala se cerró.")
	})
}

func (r *classicRoom) closeAll(reason string) {
	for _, s := range r.seats {
		if reason != "" {
			s.send(notice("closed", reason))
		}
		if s.client != nil {
			s.client.room = nil
		}
		s.client = nil
	}
	r.hub.release(r.code, r)
	if r.match != nil {
		r.match.Close()
	}
}

func (r *classicRoom) sendState(s *seat) {
	view, err := r.match.ViewFor(r.first(s))
	if err == nil {
		s.send(message{T: "state", Save: view, First: r.first(s)})
	}
}

func (r *classicRoom) sendPresence() {
	if len(r.seats) < 2 {
		return
	}
	r.seats[0].send(message{T: "rival", Online: r.seats[1].online()})
	r.seats[1].send(message{T: "rival", Online: r.seats[0].online()})
}

func (r *classicRoom) roomMessage(s *seat) message {
	names := make([]string, len(r.seats))
	for i, seated := range r.seats {
		names[i] = seated.name
	}
	return message{T: "room", Mode: modeClassic, Code: r.code, Name: s.name, Host: r.seats[0].name, Players: names,
		First: r.first(s), TimeMs: r.timeMs, OvertimeMs: r.overtimeMs}
}

func (r *classicRoom) seatOf(playerID string) *seat {
	for _, s := range r.seats {
		if s.player.ID == playerID {
			return s
		}
	}
	return nil
}

func (r *classicRoom) first(s *seat) bool {
	return s == r.seats[0]
}

func (r *classicRoom) nobodyLeft() bool {
	for _, s := range r.seats {
		if s.online() {
			return false
		}
	}
	return true
}

func clamp(v, low, high, fallback int64) int64 {
	if v == 0 {
		return fallback
	}
	return min(max(v, low), high)
}
