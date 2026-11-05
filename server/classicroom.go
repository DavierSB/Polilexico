package main

import (
	"sync"
	"time"

	"lexico/engine/modes/classic"
)

const (
	waitingTTL  = 30 * time.Minute
	finishedTTL = 10 * time.Minute
	idleTTL     = 24 * time.Hour
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
	inviteeTo  string
	voidMode   bool
	untimed    bool
	watchers   spectators
}

type listener func()

func (l listener) OnChange() { go l() }

func newClassicRoom(h *hub, code string, host player, m message, invitee *player) *classicRoom {
	r := &classicRoom{hub: h, code: code, timeMs: clamp(m.TimeMs, 60_000, 90*60_000, 25*60_000),
		overtimeMs: clamp(m.OvertimeMs, 0, 30*60_000, 10*60_000), seats: []*seat{{player: host, name: host.Name}},
		voidMode: m.Void, untimed: m.Untimed}
	if invitee != nil {
		r.invitee, r.inviteeTo = invitee.ID, invitee.Name
	}
	if r.untimed {
		r.timeMs, r.overtimeMs = 0, 0
	}
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
		if m.T == "leave" && r.watchers.remove(c) {
			r.watchers.countFor(r.seats)
		}
		return
	}
	switch m.T {
	case "move":
		r.play(s, m.Input)
	case "chat":
		relayChat(s, r.seats, m.Text)
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
		return
	}
	if r.watchers.remove(c) {
		r.watchers.countFor(r.seats)
	}
}

func (r *classicRoom) ongoing() (liveGame, bool) {
	r.mu.Lock()
	defer r.mu.Unlock()
	if r.match == nil || r.match.Game().Status().Over {
		return liveGame{}, false
	}
	names, countries := seatNames(r.seats)
	return liveGame{ID: r.code, Mode: modeClassic, Players: names, Countries: countries}, true
}

func (r *classicRoom) watch(c *client) bool {
	r.mu.Lock()
	defer r.mu.Unlock()
	if r.match == nil || r.match.Game().Status().Over {
		return false
	}
	r.watchers.add(c, r)
	r.watchers.countFor(r.seats)
	c.send(r.watcherMessage(c))
	r.sendSpectatorState()
	return true
}

func (r *classicRoom) watcherMessage(c *client) message {
	m := r.roomMessage(r.seats[0])
	m.Name, m.First, m.Watching = c.player.Name, true, true
	return m
}

func (r *classicRoom) sendSpectatorState() {
	if view, err := r.match.SpectatorView(); err == nil {
		r.watchers.send(message{T: "state", Save: view, First: true})
	}
}

func (r *classicRoom) decline(c *client) {
	r.mu.Lock()
	defer r.mu.Unlock()
	if r.match == nil && c.player.ID == r.invitee {
		r.hub.notify(r.seats[0].player.ID, notice("closed", c.player.Name+" rechazó el reto."))
		r.closeAll("")
	}
}

func (r *classicRoom) cancel(playerID, reason string) {
	r.mu.Lock()
	defer r.mu.Unlock()
	if r.match == nil && r.seats[0].player.ID == playerID {
		r.closeAll(reason)
	}
}

func (r *classicRoom) startBy(*client) {}

func (r *classicRoom) active() bool {
	r.mu.Lock()
	defer r.mu.Unlock()
	return r.match != nil && !r.match.Game().Status().Over
}

func (r *classicRoom) finished() bool {
	r.mu.Lock()
	defer r.mu.Unlock()
	return r.match != nil && r.match.Game().Status().Over
}

func (r *classicRoom) listing() (listing, bool) {
	r.mu.Lock()
	defer r.mu.Unlock()
	if r.match != nil {
		return listing{}, false
	}
	host := r.seats[0]
	return listing{seek: seek{ID: r.code, Mode: modeClassic, Host: host.name, Country: host.player.Country, TimeMs: r.timeMs,
		OvertimeMs: r.overtimeMs, Void: r.voidMode, Untimed: r.untimed, Players: len(r.seats), To: r.inviteeTo, hostID: host.player.ID},
		invitee: r.invitee}, true
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
	if r.match != nil {
		r.seatHost()
	}
	return s
}

func (r *classicRoom) seatHost() {
	host := r.seats[0]
	r.hub.seat(host.player.ID, r)
	r.hub.matched(host.player.ID, r.code)
	c := r.hub.clientOf(host.player.ID)
	if c == nil {
		return
	}
	if c.room != nil && c.room != r {
		r.hub.leaveCurrent(c)
	}
	r.attach(host, c)
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
	s.send(message{T: "watchers", Watchers: len(r.watchers.list)})
}

func (r *classicRoom) start() {
	m, err := classic.NewLocalMatch(r.seats[0].name, r.seats[1].name, classic.StarterAny, r.timeMs, r.overtimeMs, !r.voidMode, listener(r.changed))
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
	r.sendSpectatorState()
	if r.match.Game().Status().Over {
		r.finish()
	} else if r.untimed {
		r.forfeitIdleIn(idleTTL)
	}
}

func (r *classicRoom) forfeitIdleIn(d time.Duration) {
	if r.timer != nil {
		r.timer.Stop()
	}
	r.timer = time.AfterFunc(d, func() {
		r.mu.Lock()
		defer r.mu.Unlock()
		if status := r.match.Game().Status(); !status.Over {
			r.match.Forfeit(status.HumanToMove)
		}
	})
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
	r.hub.changed()
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
	r.watchers.dismiss(reason)
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
		First: r.first(s), TimeMs: r.timeMs, OvertimeMs: r.overtimeMs, Void: r.voidMode, Untimed: r.untimed}
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
