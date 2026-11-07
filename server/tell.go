package main

import (
	"strings"
	"time"
)

func (h *hub) tell(c *client, m message) {
	text := strings.TrimSpace(m.Text)
	target := h.findOnline(m.Name, m.Country)
	switch {
	case target == nil:
		c.send(notice("error", "Ese jugador no está conectado."))
	case target == c:
		c.send(notice("error", "No puedes escribirte a ti mismo."))
	case tellRefusal(c, text) != "":
		c.send(notice("error", tellRefusal(c, text)))
	default:
		h.deliver(c, target, text)
	}
}

func tellRefusal(c *client, text string) string {
	switch {
	case text == "" || len([]rune(text)) > chatMaxRunes:
		return chatEmptyOrLong
	case time.Since(c.tells.last) < chatGap:
		return chatTooSoon
	}
	return ""
}

func (h *hub) deliver(from, to *client, text string) {
	switch {
	case to.version >= tellProtocol:
		to.send(message{T: "tell", Name: from.player.Name, Country: from.player.Country, Text: text})
	case to.room != nil && to.room == from.room:
		to.send(message{T: "chat", Name: from.player.Name, Text: text})
	default:
		from.send(notice("error", to.player.Name+" tiene una versión anterior y no puede recibir mensajes."))
		return
	}
	from.tells.last = time.Now()
}
