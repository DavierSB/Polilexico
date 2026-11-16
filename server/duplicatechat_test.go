package main

import "testing"

func TestWatcherChatReachesTable(t *testing.T) {
	player, watcher, other := newClient(nil), newClient(nil), newClient(nil)
	watcher.player.Name = "Vera"
	r := &duplicateRoom{seats: []*seat{{name: "Ana", client: player}}}
	r.watchers.list = []*client{watcher, other}
	r.handle(watcher, message{T: "chat", Text: "¡Qué jugada!"})
	for _, c := range []*client{player, other} {
		if got := <-c.out; got.T != "chat" || got.Name != "Vera" {
			t.Fatalf("no llega el mensaje del espectador: %+v", got)
		}
	}
	if len(watcher.out) != 0 {
		t.Fatal("el espectador recibe su propio mensaje")
	}
}

func TestPlayerChatReachesWatchers(t *testing.T) {
	player, watcher := newClient(nil), newClient(nil)
	r := &duplicateRoom{seats: []*seat{{name: "Ana", client: player}}}
	r.watchers.list = []*client{watcher}
	player.player.ID = "a"
	r.seats[0].player.ID = "a"
	r.handle(player, message{T: "chat", Text: "hola"})
	if got := <-watcher.out; got.T != "chat" || got.Name != "Ana" {
		t.Fatalf("el espectador no recibe el mensaje de la mesa: %+v", got)
	}
}
