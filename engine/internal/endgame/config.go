package endgame

import "time"

type Config struct {
	Beam          []int
	Prefilter     int
	Wide          int
	WideBeam      int
	WidePrefilter int
	Budget        time.Duration
	MaxDepth      int
}

var Default = Config{
	Beam:          []int{0, 0, 8, 5, 3},
	Prefilter:     30,
	Wide:          250,
	WideBeam:      20,
	WidePrefilter: 80,
	Budget:        2 * time.Second,
	MaxDepth:      20,
}

type limits struct {
	width     int
	prefilter int
	wide      bool
}

func (c Config) limits(ply, moves int) limits {
	if w := c.width(ply); w > 0 {
		return limits{width: w, prefilter: c.Prefilter}
	}
	if c.Wide > 0 && moves > c.Wide {
		return limits{width: c.WideBeam, prefilter: c.WidePrefilter, wide: true}
	}
	return limits{}
}

func (c Config) width(ply int) int {
	if len(c.Beam) == 0 {
		return 0
	}
	return c.Beam[min(ply, len(c.Beam)-1)]
}
