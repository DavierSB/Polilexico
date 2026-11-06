package main

import (
	"time"

	"lexico/engine/modes/classic"
)

func newResumedRoom(h *hub, code string, host player, a adjourned, rival player) *classicRoom {
	m := message{TimeMs: a.TimeMs, OvertimeMs: a.OvertimeMs, Void: a.Void, Untimed: a.Untimed}
	r := newClassicRoom(h, code, host, m, &rival)
	r.resumed = &a
	_, r.seats[0].name, _ = a.opponentOf(rival.ID)
	r.awaitResume()
	return r
}

func (r *classicRoom) awaitResume() {
	r.timer.Stop()
	r.timer = time.AfterFunc(resumeTimeout, func() {
		r.mu.Lock()
		defer r.mu.Unlock()
		if r.match == nil {
			r.refuseResume(r.inviteeTo + " no respondió.")
		}
	})
}

func (r *classicRoom) refuseResume(reason string) {
	r.hub.blame(r.resumed.ID, r.invitee)
	r.hub.notify(r.seats[0].player.ID, notice("closed", reason))
	r.closeAll("")
}

func (r *classicRoom) newMatch() (*classic.Match, error) {
	if r.resumed == nil {
		return classic.NewLocalMatch(r.seats[0].name, r.seats[1].name, classic.StarterAny, r.timeMs, r.overtimeMs, !r.voidMode, listener(r.changed))
	}
	m, err := classic.LoadMatch(r.resumed.Save, listener(r.changed))
	if err != nil {
		return nil, err
	}
	m.Resume()
	r.hub.forgetAdjourned(r.resumed.ID)
	return m, nil
}

func (r *classicRoom) restoreSeats() {
	a := r.resumed
	if a == nil || len(r.seats) != 2 {
		return
	}
	host, guest := r.seats[0], r.seats[1]
	_, guest.name, _ = a.opponentOf(host.player.ID)
	if host.player.ID == a.PlayerB {
		r.seats = []*seat{guest, host}
	}
}

func (h *hub) resume(c *client, id string) {
	a, ok := h.findAdjourned(id, c.player.ID)
	if !ok {
		c.send(notice("error", "Esa partida aplazada ya no está."))
		return
	}
	rivalID, _, _ := a.opponentOf(c.player.ID)
	target := h.clientOf(rivalID)
	switch {
	case target == nil:
		c.send(notice("error", "Tu rival no está conectado."))
	case h.playing(target):
		c.send(notice("error", target.player.Name+" está jugando otra partida."))
	case h.canPublish(c) != "":
		c.send(notice("error", h.canPublish(c)))
	default:
		h.openResumed(c, a, target.player)
	}
}

func (h *hub) openResumed(c *client, a adjourned, rival player) {
	h.leaveCurrent(c)
	h.mu.Lock()
	code := h.freeCode()
	h.rooms[code] = newResumedRoom(h, code, c.player, a, rival)
	h.mu.Unlock()
	h.changed()
}
