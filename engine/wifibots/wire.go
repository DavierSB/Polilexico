package main

import (
	"bufio"
	"encoding/json"
	"fmt"
	"net"
	"strings"
	"sync"
	"time"
)

type msg map[string]any

type conn struct {
	c    net.Conn
	mu   sync.Mutex
	who  string
	done chan struct{}
}

func logf(who, f string, a ...any) {
	fmt.Printf("%s [%s] %s\n", time.Now().Format("15:04:05.0"), who, fmt.Sprintf(f, a...))
}

func dial(addr, who string) (*conn, error) {
	c, err := net.DialTimeout("tcp", addr, 5*time.Second)
	if err != nil {
		return nil, err
	}
	return wrap(c, who), nil
}

func wrap(c net.Conn, who string) *conn {
	k := &conn{c: c, who: who, done: make(chan struct{})}
	go k.pinger()
	return k
}

func (k *conn) send(m msg) {
	b, _ := json.Marshal(m)
	k.mu.Lock()
	defer k.mu.Unlock()
	k.c.Write(append(b, '\n'))
	if m["t"] != "ping" {
		logf(k.who, "-> %s", short(string(b)))
	}
}

func (k *conn) pinger() {
	for {
		select {
		case <-k.done:
			return
		case <-time.After(3 * time.Second):
			k.send(msg{"t": "ping"})
		}
	}
}

func (k *conn) read(handle func(msg)) {
	r := bufio.NewReaderSize(k.c, 1<<20)
	for {
		line, err := r.ReadString('\n')
		if err != nil {
			logf(k.who, "conexión cerrada: %v", err)
			close(k.done)
			return
		}
		var m msg
		if json.Unmarshal([]byte(line), &m) != nil || m["t"] == "ping" {
			continue
		}
		if m["t"] != "table" && m["t"] != "state" {
			logf(k.who, "<- %s", short(strings.TrimSpace(line)))
		}
		handle(m)
	}
}

func (k *conn) close() {
	k.c.Close()
}

func short(s string) string {
	if len(s) > 160 {
		return s[:160] + "…"
	}
	return s
}
