package timing

import "time"

type Mark struct {
	Left time.Duration
	Cue  string
}

type Marks struct {
	alarm *Alarm
	marks []Mark
}

func NewMarks(clock Clock, marks []Mark) *Marks {
	return &Marks{alarm: NewAlarm(clock), marks: marks}
}

func (m *Marks) Sync(run bool, left time.Duration, onMark func(Mark)) {
	m.alarm.Cancel()
	if !run {
		return
	}
	if next, ok := m.next(left); ok {
		m.alarm.Set(left-next.Left, func() { onMark(next) })
	}
}

func (m *Marks) next(left time.Duration) (Mark, bool) {
	for _, mark := range m.marks {
		if mark.Left < left {
			return mark, true
		}
	}
	return Mark{}, false
}
