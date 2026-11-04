package main

import (
	"context"
	"strings"

	"github.com/coder/websocket"
	"github.com/coder/websocket/wsjson"
)

type seek struct {
	ID      string `json:"id"`
	Mode    string `json:"mode"`
	Host    string `json:"host"`
	Mine    bool   `json:"mine"`
	Players int    `json:"players"`
}

type message struct {
	T       string   `json:"t"`
	Version int      `json:"version,omitempty"`
	Token   string   `json:"token,omitempty"`
	Mode    string   `json:"mode,omitempty"`
	Code    string   `json:"code,omitempty"`
	TimeMs  int64    `json:"timeMs,omitempty"`
	TurnMs  int64    `json:"turnMs,omitempty"`
	Rounds  int      `json:"rounds,omitempty"`
	Live    bool     `json:"live,omitempty"`
	Input   string   `json:"input,omitempty"`
	Text    string   `json:"text,omitempty"`
	Name    string   `json:"name,omitempty"`
	Reason  string   `json:"reason,omitempty"`
	Save    string   `json:"save,omitempty"`
	View    string   `json:"view,omitempty"`
	First   bool     `json:"first,omitempty"`
	Host    string   `json:"host,omitempty"`
	Players []string `json:"players,omitempty"`
	Seeks   []seek   `json:"seeks,omitempty"`
	Invites []seek   `json:"invites,omitempty"`
}

type link struct {
	conn *websocket.Conn
}

func session(o options, p *progress) error {
	t, err := token(o)
	if err != nil {
		return err
	}
	l, err := dial(o.server)
	if err != nil {
		return err
	}
	defer l.conn.CloseNow()
	l.send(message{T: "hello", Version: 2, Token: t})
	return newBot(l, o, p).run()
}

func dial(server string) (*link, error) {
	url := "ws" + strings.TrimPrefix(server, "http") + "/ws"
	conn, _, err := websocket.Dial(context.Background(), url, nil)
	if err != nil {
		return nil, err
	}
	conn.SetReadLimit(1 << 22)
	return &link{conn}, nil
}

func (l *link) send(m message) {
	wsjson.Write(context.Background(), l.conn, m)
}

func (l *link) read() (message, error) {
	var m message
	err := wsjson.Read(context.Background(), l.conn, &m)
	return m, err
}
