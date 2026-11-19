package main

import "time"

var (
	finalPassDelay  = 3 * time.Second
	finalPassWindow = 30 * time.Second
)

type finalPass struct {
	timer *time.Timer
	at    time.Time
	seq   int
}

func (r *classicRoom) watchFinalPass() {
	if !r.match.Game().FinalPass() {
		r.stopFinalPass()
		return
	}
	if r.final.at.IsZero() && r.speaksFinalPass(r.seatToMove()) {
		r.armFinalPass()
	}
}

func (r *classicRoom) speaksFinalPass(s *seat) bool {
	c := r.hub.clientOf(s.player.ID)
	return c != nil && c.version >= finalProtocol
}

func (r *classicRoom) armFinalPass() {
	r.final.seq++
	seq, wait := r.final.seq, finalPassDelay+finalPassWindow
	r.final.at = time.Now().Add(wait)
	r.final.timer = time.AfterFunc(wait, func() { r.acceptFinalPass(seq) })
}

func (r *classicRoom) acceptFinalPass(seq int) {
	r.mu.Lock()
	defer r.mu.Unlock()
	if r.match != nil && seq == r.final.seq && r.match.Game().FinalPass() {
		r.match.Play("pasar")
	}
}

func (r *classicRoom) stopFinalPass() {
	stopTimer(r.final.timer)
	r.final.seq++
	r.final.at = time.Time{}
}

func (r *classicRoom) finalMsFor(s *seat) int64 {
	if r.final.at.IsZero() || s != r.seatToMove() {
		return 0
	}
	return max(time.Until(r.final.at).Milliseconds(), 1)
}

func (r *classicRoom) seatToMove() *seat {
	if r.match.Game().Status().HumanToMove {
		return r.seats[0]
	}
	return r.seats[1]
}
