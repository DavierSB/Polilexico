package main

const protocolVersion = 1

const (
	modeClassic   = "classic"
	modeDuplicate = "duplicate"
)

type message struct {
	T          string   `json:"t"`
	Version    int      `json:"version,omitempty"`
	Token      string   `json:"token,omitempty"`
	Name       string   `json:"name,omitempty"`
	Mode       string   `json:"mode,omitempty"`
	Code       string   `json:"code,omitempty"`
	TimeMs     int64    `json:"timeMs,omitempty"`
	OvertimeMs int64    `json:"overtimeMs,omitempty"`
	TurnMs     int64    `json:"turnMs,omitempty"`
	Rounds     int      `json:"rounds,omitempty"`
	Live       bool     `json:"live,omitempty"`
	Input      string   `json:"input,omitempty"`
	Reason     string   `json:"reason,omitempty"`
	Save       string   `json:"save,omitempty"`
	View       string   `json:"view,omitempty"`
	First      bool     `json:"first,omitempty"`
	Host       string   `json:"host,omitempty"`
	Players    []string `json:"players,omitempty"`
	Online     bool     `json:"online,omitempty"`
	Country    string   `json:"country,omitempty"`
	Seeks      []seek   `json:"seeks,omitempty"`
	Invites    []seek   `json:"invites,omitempty"`
	People     []person `json:"people,omitempty"`
}

func notice(kind, reason string) message {
	return message{T: kind, Reason: reason}
}
