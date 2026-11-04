package main

import (
	"context"
	"sync"
	"time"

	"github.com/coder/websocket"
	"github.com/coder/websocket/wsjson"
)

const (
	outboxSize   = 64
	writeTimeout = 10 * time.Second
	pingEvery    = 20 * time.Second
	pongTimeout  = 15 * time.Second
	kickGrace    = time.Second
)

type client struct {
	conn   *websocket.Conn
	out    chan message
	done   chan struct{}
	once   sync.Once
	player player
	room   room
}

func newClient(conn *websocket.Conn) *client {
	return &client{conn: conn, out: make(chan message, outboxSize), done: make(chan struct{})}
}

func (c *client) send(m message) {
	select {
	case c.out <- m:
	case <-c.done:
	default:
		c.close()
	}
}

func (c *client) kick(reason string) {
	c.send(notice("refused", reason))
	time.AfterFunc(kickGrace, c.close)
}

func (c *client) close() {
	c.once.Do(func() {
		close(c.done)
		c.conn.Close(websocket.StatusNormalClosure, "")
	})
}

func (c *client) readLoop(ctx context.Context, handle func(message)) {
	defer c.close()
	for {
		var m message
		if err := wsjson.Read(ctx, c.conn, &m); err != nil {
			return
		}
		handle(m)
	}
}

func (c *client) writeLoop(ctx context.Context) {
	for {
		select {
		case m := <-c.out:
			if !c.write(ctx, m) {
				c.close()
				return
			}
		case <-c.done:
			return
		}
	}
}

func (c *client) write(ctx context.Context, m message) bool {
	ctx, cancel := context.WithTimeout(ctx, writeTimeout)
	defer cancel()
	return wsjson.Write(ctx, c.conn, m) == nil
}

func (c *client) pingLoop(ctx context.Context) {
	tick := time.NewTicker(pingEvery)
	defer tick.Stop()
	for {
		select {
		case <-tick.C:
			if !c.ping(ctx) {
				c.close()
				return
			}
		case <-c.done:
			return
		}
	}
}

func (c *client) ping(ctx context.Context) bool {
	ctx, cancel := context.WithTimeout(ctx, pongTimeout)
	defer cancel()
	return c.conn.Ping(ctx) == nil
}
