package main

import (
	"strconv"
	"context"
	"crypto/rand"
	"math/big"
	"net/http"
	"strings"
	"sync"

	"github.com/coder/websocket"
)

const codeAlphabet = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"

type room interface {
	enter(c *client)
	handle(c *client, m message)
	drop(c *client)
	decline(c *client)
	listing() (listing, bool)
	cancel(playerID, reason string)
	startBy(c *client)
	active() bool
}

type hub struct {
	mu     sync.Mutex
	store  store
	rooms  map[string]room
	seats  map[string]room
	online map[string]*client
}

func newHub(st store) *hub {
	return &hub{store: st, rooms: map[string]room{}, seats: map[string]room{}, online: map[string]*client{}}
}

func (h *hub) serve(w http.ResponseWriter, r *http.Request) {
	conn, err := websocket.Accept(w, r, &websocket.AcceptOptions{InsecureSkipVerify: true})
	if err != nil {
		return
	}
	c := newClient(conn)
	ctx := r.Context()
	go c.writeLoop(ctx)
	go c.pingLoop(ctx)
	c.readLoop(ctx, func(m message) { h.handle(ctx, c, m) })
	if c.room != nil {
		c.room.drop(c)
	}
	h.goOffline(c)
	h.changed()
}

func (h *hub) handle(ctx context.Context, c *client, m message) {
	switch {
	case m.T == "hello":
		h.hello(ctx, c, m)
	case c.player.ID == "":
		c.send(notice("refused", errSession.Error()))
	case m.T == "create":
		h.create(c, m, nil)
	case m.T == "challenge":
		h.challenge(c, m)
	case m.T == "join":
		h.join(c, m.Code)
	case m.T == "decline":
		h.decline(c, m.Code)
	case m.T == "cancel":
		h.cancel(c, m.Code)
	case m.T == "start" && m.Code != "":
		h.startSeek(c, m.Code)
	case c.room != nil:
		c.room.handle(c, m)
	}
}

func (h *hub) hello(ctx context.Context, c *client, m message) {
	if m.Version != protocolVersion {
		c.send(notice("refused", "Actualiza Polilexico para jugar en línea."))
		return
	}
	p, err := h.store.Identify(ctx, m.Token)
	if err != nil {
		c.send(notice("refused", asUserError(err, errSession).Error()))
		return
	}
	c.player = p
	h.goOnline(c)
	defer h.changed()
	c.send(message{T: "welcome", Name: p.Name, Country: p.Country})
	c.send(h.lobbyFor(p))
	if r := h.roomOf(p.ID); r != nil {
		r.enter(c)
	}
}

func (h *hub) create(c *client, m message, invitee *player) {
	if err := h.canPublish(c); err != "" {
		c.send(notice("error", err))
		return
	}
	h.leaveCurrent(c)
	h.mu.Lock()
	code := h.freeCode()
	h.rooms[code] = newRoom(h, code, c.player, m, invitee)
	h.mu.Unlock()
	h.changed()
}

func (h *hub) canPublish(c *client) string {
	switch {
	case c.room != nil && c.room.active():
		return "Termina tu partida antes de publicar otra."
	case h.seekCount(c.player.ID) >= maxSeeks:
		return "Ya tienes " + strconv.Itoa(maxSeeks) + " búsquedas publicadas."
	}
	return ""
}

func (h *hub) seekCount(playerID string) int {
	n := 0
	for _, l := range h.listings() {
		if l.seek.hostID == playerID {
			n++
		}
	}
	return n
}

func (h *hub) challenge(c *client, m message) {
	target := h.findOnline(m.Name, m.Country)
	switch {
	case target == nil:
		c.send(notice("error", "Ese jugador no está conectado."))
	case target.player.ID == c.player.ID:
		c.send(notice("error", "No puedes retarte a ti mismo."))
	default:
		m.Mode = modeClassic
		h.create(c, m, &target.player)
	}
}

func (h *hub) join(c *client, code string) {
	r := h.roomByCode(code)
	if r == nil || !h.joinable(r, c) {
		c.send(notice("error", "Esa partida ya no está disponible."))
		return
	}
	if r != c.room {
		h.leaveCurrent(c)
	}
	h.withdrawSeeksOf(c.player.ID, code)
	r.enter(c)
	h.changed()
}

func (h *hub) joinable(r room, c *client) bool {
	l, waiting := r.listing()
	return !waiting || (l.seek.hostID != c.player.ID && h.isOnline(l.seek.hostID))
}

func (h *hub) cancel(c *client, code string) {
	if r := h.roomByCode(code); r != nil {
		r.cancel(c.player.ID, "El anfitrión cerró la sala.")
	}
}

func (h *hub) startSeek(c *client, code string) {
	if c.room != nil && c.room.active() {
		c.send(notice("error", "Termina tu partida antes de empezar otra."))
		return
	}
	if r := h.roomByCode(code); r != nil {
		h.leaveCurrent(c)
		r.startBy(c)
	}
}

func (h *hub) matched(playerID, code string) {
	go h.withdrawSeeksOf(playerID, code)
}

func (h *hub) withdrawSeeksOf(playerID, except string) {
	for _, l := range h.listings() {
		if l.seek.hostID == playerID && l.seek.ID != except {
			if r := h.roomByCode(l.seek.ID); r != nil {
				r.cancel(playerID, "El anfitrión empezó otra partida.")
			}
		}
	}
}

func (h *hub) notify(playerID string, m message) {
	if c := h.clientOf(playerID); c != nil {
		c.send(m)
	}
}

func (h *hub) clientOf(playerID string) *client {
	h.mu.Lock()
	defer h.mu.Unlock()
	return h.online[playerID]
}

func (h *hub) isOnline(playerID string) bool {
	return h.clientOf(playerID) != nil
}

func (h *hub) decline(c *client, code string) {
	if r := h.roomByCode(code); r != nil {
		r.decline(c)
	}
}

func newRoom(h *hub, code string, host player, m message, invitee *player) room {
	if m.Mode == modeDuplicate {
		return newDuplicateRoom(h, code, host, m)
	}
	return newClassicRoom(h, code, host, m, invitee)
}

func (h *hub) leaveCurrent(c *client) {
	if c.room != nil {
		c.room.handle(c, message{T: "leave"})
	}
}

func (h *hub) roomByCode(code string) room {
	h.mu.Lock()
	defer h.mu.Unlock()
	return h.rooms[strings.ToUpper(strings.TrimSpace(code))]
}

func (h *hub) roomOf(playerID string) room {
	h.mu.Lock()
	defer h.mu.Unlock()
	return h.seats[playerID]
}

func (h *hub) seat(playerID string, r room) {
	h.mu.Lock()
	defer h.mu.Unlock()
	h.seats[playerID] = r
}

func (h *hub) unseat(playerID string, r room) {
	h.mu.Lock()
	defer h.mu.Unlock()
	if h.seats[playerID] == r {
		delete(h.seats, playerID)
	}
}

func (h *hub) release(code string, r room) {
	h.mu.Lock()
	defer h.mu.Unlock()
	if h.rooms[code] == r {
		delete(h.rooms, code)
	}
	for id, seated := range h.seats {
		if seated == r {
			delete(h.seats, id)
		}
	}
	go h.broadcastLobby()
}

func (h *hub) freeCode() string {
	for {
		if code := randomCode(); h.rooms[code] == nil {
			return code
		}
	}
}

func randomCode() string {
	var b strings.Builder
	for range 4 {
		n, _ := rand.Int(rand.Reader, big.NewInt(int64(len(codeAlphabet))))
		b.WriteByte(codeAlphabet[n.Int64()])
	}
	return b.String()
}
