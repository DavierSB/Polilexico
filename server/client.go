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
	silenceLimit = 25 * time.Second
	kickGrace    = time.Second
)

type client struct {
	conn    *websocket.Conn
	out     chan message
	done    chan struct{}
	once    sync.Once
	player  player
	room    room
	version int
	tells   chatState
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
		m, err := c.read(ctx)
		if err != nil {
			return
		}
		handle(m)
	}
}

func (c *client) read(ctx context.Context) (message, error) {
	ctx, cancel := context.WithTimeout(ctx, silenceLimit)
	defer cancel()
	var m message
	err := wsjson.Read(ctx, c.conn, &m)
	return m, err
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
