package main

import (
	"strings"
	"time"
	"unicode/utf8"
)

const (
	chatMaxRunes    = 140
	chatGap         = 5 * time.Second
	chatUnanswered  = 3
	chatTooSoon     = "Espera unos segundos para escribir otra vez."
	chatWaitAnswer  = "Espera la respuesta antes de escribir más."
	chatEmptyOrLong = "El mensaje debe tener entre 1 y 140 caracteres."
)

type chatState struct {
	last       time.Time
	unanswered int
}

func relayChat(from *seat, seats []*seat, text string) {
	text = strings.TrimSpace(text)
	if reason := chatRefusal(from, text); reason != "" {
		from.send(notice("error", reason))
		return
	}
	from.chat.last, from.chat.unanswered = time.Now(), from.chat.unanswered+1
	for _, s := range seats {
		if s != from {
			s.chat.unanswered = 0
			s.send(message{T: "chat", Name: from.name, Text: text})
		}
	}
}

func chatRefusal(from *seat, text string) string {
	switch {
	case text == "" || utf8.RuneCountInString(text) > chatMaxRunes:
		return chatEmptyOrLong
	case time.Since(from.chat.last) < chatGap:
		return chatTooSoon
	case from.chat.unanswered >= chatUnanswered:
		return chatWaitAnswer
	}
	return ""
}
