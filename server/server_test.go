package main

import (
	"bytes"
	"context"
	"encoding/base64"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"strings"
	"sync"
	"testing"
	"time"

	"github.com/coder/websocket"
	"github.com/coder/websocket/wsjson"
)

var engineOnce sync.Once

type testServer struct {
	*httptest.Server
	store *memoryStore
	hub   *hub
}

type testClient struct {
	t       *testing.T
	conn    *websocket.Conn
	in      chan message
	name    string
	country string
}

func TestSignUpAndLogIn(t *testing.T) {
	srv := newTestServer(t)
	srv.signUp(t, "Ana", "CU")
	var s session
	if code := srv.post(t, "/login", account{Name: "ana", Country: "CU", Password: "secreta"}, &s); code != 200 || s.Name != "Ana" {
		t.Fatalf("%d %+v", code, s)
	}
	if code := srv.post(t, "/signup", account{Name: "ANA", Country: "CU", Password: "secreta"}, nil); code != 400 {
		t.Fatalf("nombre repetido: %d", code)
	}
	srv.signUp(t, "Ana", "ES")
}

func TestLogInRejectsWrongPassword(t *testing.T) {
	srv := newTestServer(t)
	srv.signUp(t, "Ana", "CU")
	if code := srv.post(t, "/login", account{Name: "Ana", Country: "CU", Password: "otra123"}, nil); code != 400 {
		t.Fatalf("%d", code)
	}
}

func TestClassicGameBetweenTwoPlayers(t *testing.T) {
	srv := newTestServer(t)
	ana, beto := srv.player(t, "Ana", "CU"), srv.player(t, "Beto", "ES")
	ana.send(message{T: "create", Mode: modeClassic, TimeMs: 600_000})
	code := ana.mySeeks(1)[0].ID
	beto.send(message{T: "join", Code: code})
	first := ana.waitFor("state")
	beto.waitFor("state")
	onTurn, waiting := beto, ana
	if turnIsFirst(t, first.Save) {
		onTurn, waiting = ana, beto
	}
	waiting.send(message{T: "move", Input: "pasar"})
	waiting.waitFor("rejected")
	onTurn.send(message{T: "move", Input: "pasar"})
	waiting.waitFor("state")
	ana.send(message{T: "leave"})
	beto.waitUntil(func(m message) bool { return m.T == "state" && strings.Contains(m.Save, `LostOnTime\":true`) })
	waitStats(t, srv, "Beto", "ES", "win")
}

func TestClassicRoomRejectsThirdPlayer(t *testing.T) {
	srv := newTestServer(t)
	ana, beto, carla := srv.player(t, "Ana", "CU"), srv.player(t, "Beto", "ES"), srv.player(t, "Carla", "MX")
	ana.send(message{T: "create", Mode: modeClassic})
	code := ana.mySeeks(1)[0].ID
	beto.send(message{T: "join", Code: code})
	beto.waitFor("state")
	carla.send(message{T: "join", Code: code})
	carla.waitFor("error")
}

func TestReconnectReturnsToTheGame(t *testing.T) {
	srv := newTestServer(t)
	ana, beto := srv.player(t, "Ana", "CU"), srv.player(t, "Beto", "ES")
	ana.send(message{T: "create", Mode: modeClassic})
	beto.send(message{T: "join", Code: ana.mySeeks(1)[0].ID})
	beto.waitFor("state")
	beto.conn.Close(websocket.StatusNormalClosure, "")
	ana.waitUntil(func(m message) bool { return m.T == "rival" && !m.Online })
	again := srv.connect(t, srv.logIn(t, "Beto", "ES"))
	again.waitFor("state")
	ana.waitUntil(func(m message) bool { return m.T == "rival" && m.Online })
}

func TestDuplicateRoomStartsForEveryone(t *testing.T) {
	srv := newTestServer(t)
	ana, beto := srv.player(t, "Ana", "CU"), srv.player(t, "Ana", "ES")
	ana.send(message{T: "create", Mode: modeDuplicate, TurnMs: 60_000})
	code := ana.mySeeks(1)[0].ID
	beto.send(message{T: "join", Code: code})
	lobby := beto.waitUntil(func(m message) bool { return m.T == "room" && len(m.Players) == 2 })
	if lobby.Players[1] != "Ana (ES)" {
		t.Fatalf("%v", lobby.Players)
	}
	ana.waitUntil(func(m message) bool { return m.T == "lobby" && len(m.Seeks) == 1 && m.Seeks[0].Players == 2 })
	ana.send(message{T: "start", Code: code})
	ana.waitFor("room")
	ana.waitFor("table")
	view := beto.waitFor("table").View
	if !strings.Contains(view, `"Phase":"playing"`) {
		t.Fatal(view)
	}
	beto.send(message{T: "move", Input: "zzz"})
	beto.waitFor("rejected")
}

func TestDuplicateResultRanksPlayers(t *testing.T) {
	r := &duplicateRoom{seats: []*seat{{name: "Ana"}, {name: "Beto"}}}
	var final tableSummary
	json.Unmarshal([]byte(`{"Phase":"finished","Rounds":[{"Plays":[
		{"Name":"Máster","Kind":"play","Points":80,"Total":80},
		{"Name":"Ana","Kind":"play","Points":80,"TileCount":7,"Total":80},
		{"Name":"Beto","Kind":"play","Points":20,"TileCount":2,"Total":20}]}]}`), &final)
	res := duplicateResult(r, final)
	ana, beto := res.Players[0], res.Players[1]
	if ana.Result != "win" || ana.BestBingo != 80 || *ana.Efficiency != 1 || beto.Result != "loss" || beto.Place != 2 {
		t.Fatalf("%+v %+v", ana, beto)
	}
}

func newTestServer(t *testing.T) *testServer {
	t.Helper()
	engineOnce.Do(func() {
		if err := startEngine(config{thirdParty: "../third_party"}); err != nil {
			t.Fatal(err)
		}
	})
	st := newMemoryStore()
	h := newHub(st)
	srv := httptest.NewServer(routes(h, st))
	t.Cleanup(srv.Close)
	return &testServer{srv, st, h}
}

func (s *testServer) player(t *testing.T, name, country string) *testClient {
	return s.connect(t, s.signUp(t, name, country))
}

func (s *testServer) signUp(t *testing.T, name, country string) session {
	t.Helper()
	var sess session
	if code := s.post(t, "/signup", account{Name: name, Country: country, Password: "secreta"}, &sess); code != 200 {
		t.Fatalf("alta de %s: %d", name, code)
	}
	return sess
}

func (s *testServer) logIn(t *testing.T, name, country string) session {
	t.Helper()
	var sess session
	if code := s.post(t, "/login", account{Name: name, Country: country, Password: "secreta"}, &sess); code != 200 {
		t.Fatalf("entrada de %s: %d", name, code)
	}
	return sess
}

func (s *testServer) post(t *testing.T, path string, body, out any) int {
	t.Helper()
	data, _ := json.Marshal(body)
	resp, err := http.Post(s.URL+path, "application/json", bytes.NewReader(data))
	if err != nil {
		t.Fatal(err)
	}
	defer resp.Body.Close()
	if out != nil {
		json.NewDecoder(resp.Body).Decode(out)
	}
	return resp.StatusCode
}

func (s *testServer) connect(t *testing.T, sess session) *testClient {
	t.Helper()
	return s.connectAs(t, sess, protocolVersion)
}

func (s *testServer) connectAs(t *testing.T, sess session, version int) *testClient {
	t.Helper()
	conn, _, err := websocket.Dial(context.Background(), "ws"+strings.TrimPrefix(s.URL, "http")+"/ws", nil)
	if err != nil {
		t.Fatal(err)
	}
	c := &testClient{t: t, conn: conn, in: make(chan message, 256)}
	go c.read()
	t.Cleanup(func() { conn.Close(websocket.StatusNormalClosure, "") })
	c.send(message{T: "hello", Version: version, Token: sess.AccessToken})
	c.waitFor("welcome")
	return c
}

func (c *testClient) read() {
	for {
		var m message
		if wsjson.Read(context.Background(), c.conn, &m) != nil {
			close(c.in)
			return
		}
		c.in <- m
	}
}

func (c *testClient) send(m message) {
	if err := wsjson.Write(context.Background(), c.conn, m); err != nil {
		c.t.Fatal(err)
	}
}

func (c *testClient) waitFor(kind string) message {
	c.t.Helper()
	return c.waitUntil(func(m message) bool { return m.T == kind })
}

func (c *testClient) waitUntil(ok func(message) bool) message {
	c.t.Helper()
	timeout := time.After(10 * time.Second)
	for {
		select {
		case m, open := <-c.in:
			if !open {
				c.t.Fatal("conexión cerrada")
			}
			if ok(m) {
				return m
			}
		case <-timeout:
			c.t.Fatal("no llegó el mensaje esperado")
		}
	}
}

func turnIsFirst(t *testing.T, save string) bool {
	t.Helper()
	var sm struct{ Game string }
	var sg struct {
		HumanIdx int
		History  struct {
			Events []struct{} `json:"events"`
		}
	}
	json.Unmarshal([]byte(save), &sm)
	json.Unmarshal([]byte(sm.Game), &sg)
	return sg.HumanIdx == len(sg.History.Events)%2
}

func waitStats(t *testing.T, srv *testServer, name, country, want string) {
	t.Helper()
	p, _ := srv.store.Identify(context.Background(), srv.logIn(t, name, country).AccessToken)
	for range 50 {
		if rows, _ := srv.store.Stats(context.Background(), p); len(rows) == 1 && rows[0].Games == 1 && (want != "win" || rows[0].Wins == 1) {
			return
		}
		time.Sleep(100 * time.Millisecond)
	}
	t.Fatal("no se guardó el resultado")
}

func TestSeekAppearsAndCanBeAccepted(t *testing.T) {
	srv := newTestServer(t)
	ana, beto := srv.player(t, "Ana", "CU"), srv.player(t, "Beto", "ES")
	ana.send(message{T: "create", Mode: modeClassic, TimeMs: 900_000})
	lobby := beto.waitUntil(func(m message) bool { return m.T == "lobby" && len(m.Seeks) == 1 })
	if s := lobby.Seeks[0]; s.Host != "Ana" || s.Country != "CU" || s.TimeMs != 900_000 || s.Mine {
		t.Fatalf("%+v", s)
	}
	beto.send(message{T: "join", Code: lobby.Seeks[0].ID})
	beto.waitFor("state")
	beto.waitUntil(func(m message) bool { return m.T == "lobby" && len(m.Seeks) == 0 })
}

func TestLobbyListsWhoIsOnline(t *testing.T) {
	srv := newTestServer(t)
	ana := srv.player(t, "Ana", "CU")
	srv.player(t, "Beto", "ES")
	lobby := ana.waitUntil(func(m message) bool { return m.T == "lobby" && len(m.People) == 1 })
	if lobby.People[0].Name != "Beto" {
		t.Fatalf("%+v", lobby.People)
	}
}

func TestDirectChallengeIsAccepted(t *testing.T) {
	srv := newTestServer(t)
	ana, beto, carla := srv.player(t, "Ana", "CU"), srv.player(t, "Beto", "ES"), srv.player(t, "Carla", "MX")
	ana.send(message{T: "challenge", Name: "beto", Country: "ES", TimeMs: 600_000})
	invite := beto.waitUntil(func(m message) bool { return m.T == "lobby" && len(m.Invites) == 1 }).Invites[0]
	if lobby := carla.waitFor("lobby"); len(lobby.Seeks) != 0 || len(lobby.Invites) != 0 {
		t.Fatalf("el reto privado se ve: %+v", lobby)
	}
	carla.send(message{T: "join", Code: invite.ID})
	carla.waitFor("error")
	beto.send(message{T: "join", Code: invite.ID})
	beto.waitFor("state")
	ana.waitFor("state")
}

func TestDeclinedChallengeClosesTheRoom(t *testing.T) {
	srv := newTestServer(t)
	ana, beto := srv.player(t, "Ana", "CU"), srv.player(t, "Beto", "ES")
	ana.send(message{T: "challenge", Name: "Beto", Country: "ES"})
	invite := beto.waitUntil(func(m message) bool { return m.T == "lobby" && len(m.Invites) == 1 }).Invites[0]
	beto.send(message{T: "decline", Code: invite.ID})
	if closed := ana.waitFor("closed"); !strings.Contains(closed.Reason, "Beto") {
		t.Fatal(closed.Reason)
	}
}

func TestChallengeNeedsAnOnlinePlayer(t *testing.T) {
	srv := newTestServer(t)
	ana := srv.player(t, "Ana", "CU")
	srv.signUp(t, "Beto", "ES")
	ana.send(message{T: "challenge", Name: "Beto", Country: "ES"})
	ana.waitFor("error")
}

func TestUntimedVoidClassicKeepsTheTurn(t *testing.T) {
	srv := newTestServer(t)
	ana, beto := srv.player(t, "Ana", "CU"), srv.player(t, "Beto", "ES")
	ana.send(message{T: "create", Mode: modeClassic, Void: true, Untimed: true})
	mine := ana.mySeeks(1)[0]
	if mine.TimeMs != 0 || !mine.Void || !mine.Untimed {
		t.Fatalf("%+v", mine)
	}
	beto.send(message{T: "join", Code: mine.ID})
	first := ana.waitFor("state")
	beto.waitFor("state")
	onTurn := beto
	if turnIsFirst(t, first.Save) {
		onTurn = ana
	}
	onTurn.send(message{T: "move", Input: "h8 ZZXQ"})
	if r := onTurn.waitFor("rejected"); r.Reason == "" {
		t.Fatal("sin motivo")
	}
}

func (c *testClient) mySeeks(n int) []seek {
	c.t.Helper()
	lobby := c.waitUntil(func(m message) bool { return m.T == "lobby" && len(mine(m.Seeks)) == n })
	return mine(lobby.Seeks)
}

func mine(seeks []seek) []seek {
	var out []seek
	for _, s := range seeks {
		if s.Mine {
			out = append(out, s)
		}
	}
	return out
}

func TestSeveralSeeksAndTheRestAreWithdrawn(t *testing.T) {
	srv := newTestServer(t)
	ana, beto := srv.player(t, "Ana", "CU"), srv.player(t, "Beto", "ES")
	ana.send(message{T: "create", Mode: modeClassic, TimeMs: 600_000})
	ana.send(message{T: "create", Mode: modeClassic, Void: true})
	ana.send(message{T: "create", Mode: modeDuplicate})
	seeks := ana.mySeeks(3)
	classic := seeks[0]
	for _, s := range seeks {
		if s.Mode == modeClassic {
			classic = s
		}
	}
	beto.send(message{T: "join", Code: classic.ID})
	ana.waitFor("state")
	ana.mySeeks(0)
}

func TestCancelledSeekDisappears(t *testing.T) {
	srv := newTestServer(t)
	ana, beto := srv.player(t, "Ana", "CU"), srv.player(t, "Beto", "ES")
	ana.send(message{T: "create", Mode: modeClassic})
	code := ana.mySeeks(1)[0].ID
	ana.send(message{T: "cancel", Code: code})
	ana.mySeeks(0)
	beto.send(message{T: "join", Code: code})
	beto.waitFor("error")
}

func TestNoPublishingDuringAGame(t *testing.T) {
	srv := newTestServer(t)
	ana, beto := srv.player(t, "Ana", "CU"), srv.player(t, "Beto", "ES")
	ana.send(message{T: "create", Mode: modeClassic})
	beto.send(message{T: "join", Code: ana.mySeeks(1)[0].ID})
	beto.waitFor("state")
	beto.send(message{T: "create", Mode: modeClassic})
	beto.waitFor("error")
}

func TestChatIsRelayedWithinLimits(t *testing.T) {
	srv := newTestServer(t)
	ana, beto := srv.player(t, "Ana", "CU"), srv.player(t, "Beto", "ES")
	ana.send(message{T: "create", Mode: modeClassic})
	beto.send(message{T: "join", Code: ana.mySeeks(1)[0].ID})
	beto.waitFor("state")
	ana.waitFor("state")
	ana.send(message{T: "chat", Text: "  Hola  "})
	if m := beto.waitFor("chat"); m.Name != "Ana" || m.Text != "Hola" {
		t.Fatalf("%+v", m)
	}
	ana.send(message{T: "chat", Text: "otra vez"})
	if m := ana.waitFor("error"); m.Reason != chatTooSoon {
		t.Fatal(m.Reason)
	}
	ana.send(message{T: "chat", Text: strings.Repeat("a", chatMaxRunes+1)})
	ana.waitFor("error")
}

func TestChatWaitsForAnAnswer(t *testing.T) {
	from, other := &seat{name: "Ana"}, &seat{name: "Beto"}
	seats := []*seat{from, other}
	for range chatUnanswered {
		from.chat.last = time.Time{}
		relayChat(from, seats, "hola")
	}
	from.chat.last = time.Time{}
	if chatRefusal(from, "hola") != chatWaitAnswer {
		t.Fatal("no espera respuesta")
	}
	relayChat(other, seats, "dime")
	if chatRefusal(from, "hola") != "" {
		t.Fatal("la respuesta no desbloquea")
	}
}

func TestPhotoIsSavedServedAndRemoved(t *testing.T) {
	srv := newTestServer(t)
	sess := srv.signUp(t, "Ana", "CU")
	jpeg := base64.StdEncoding.EncodeToString([]byte{0xFF, 0xD8, 0xFF, 0xE0, 1, 2, 3})
	put := func(data string) int {
		body, _ := json.Marshal(map[string]string{"data": data})
		req, _ := http.NewRequest("PUT", srv.URL+"/photo", bytes.NewReader(body))
		req.Header.Set("Authorization", "Bearer "+sess.AccessToken)
		res, err := http.DefaultClient.Do(req)
		if err != nil {
			t.Fatal(err)
		}
		res.Body.Close()
		return res.StatusCode
	}
	if put("bm8gZXMgdW5hIGZvdG8=") != http.StatusBadRequest || put(jpeg) != http.StatusOK {
		t.Fatal("validación")
	}
	res, _ := http.Get(srv.URL + "/photo?name=ana&country=CU")
	if res.StatusCode != http.StatusOK || res.Header.Get("Content-Type") != "image/jpeg" {
		t.Fatal(res.Status)
	}
	again, _ := http.NewRequest("GET", srv.URL+"/photo?name=Ana&country=CU", nil)
	again.Header.Set("If-None-Match", res.Header.Get("ETag"))
	if cached, _ := http.DefaultClient.Do(again); cached.StatusCode != http.StatusNotModified {
		t.Fatal(cached.Status)
	}
	put("")
	if gone, _ := http.Get(srv.URL + "/photo?name=Ana&country=CU"); gone.StatusCode != http.StatusNotFound {
		t.Fatal(gone.Status)
	}
}

func TestSpectatorWatchesAndIsCounted(t *testing.T) {
	srv := newTestServer(t)
	ana, beto, carla := srv.player(t, "Ana", "CU"), srv.player(t, "Beto", "ES"), srv.player(t, "Carla", "MX")
	ana.send(message{T: "create", Mode: modeClassic})
	beto.send(message{T: "join", Code: ana.mySeeks(1)[0].ID})
	beto.waitFor("state")
	game := carla.waitUntil(func(m message) bool { return m.T == "lobby" && len(m.Games) == 1 }).Games[0]
	if game.Players[0] != "Ana" || game.Countries[1] != "ES" {
		t.Fatalf("%+v", game)
	}
	carla.send(message{T: "watch", Code: game.ID})
	if room := carla.waitFor("room"); !room.Watching {
		t.Fatalf("%+v", room)
	}
	carla.waitFor("state")
	ana.waitUntil(func(m message) bool { return m.T == "watchers" && m.Watchers == 1 })
	carla.send(message{T: "leave"})
	ana.waitUntil(func(m message) bool { return m.T == "watchers" && m.Watchers == 0 })
}

func TestAccountCanBeEdited(t *testing.T) {
	srv := newTestServer(t)
	sess := srv.signUp(t, "Ana", "CU")
	srv.signUp(t, "Beto", "ES")
	edit := func(a account) int {
		body, _ := json.Marshal(a)
		req, _ := http.NewRequest("PUT", srv.URL+"/account", bytes.NewReader(body))
		req.Header.Set("Authorization", "Bearer "+sess.AccessToken)
		res, err := http.DefaultClient.Do(req)
		if err != nil {
			t.Fatal(err)
		}
		res.Body.Close()
		return res.StatusCode
	}
	if edit(account{Name: "Beto", Country: "ES"}) == http.StatusOK {
		t.Fatal("tomó un nombre ocupado")
	}
	if code := edit(account{Name: "Anita", Country: "MX", Email: "ana@correo.com"}); code != http.StatusOK {
		t.Fatal(code)
	}
	if again := srv.logIn(t, "anita", "MX"); again.Name != "Anita" {
		t.Fatalf("%+v", again)
	}
}

func TestSeeksGoAwayWithTheirHost(t *testing.T) {
	srv := newTestServer(t)
	ana, beto := srv.player(t, "Ana", "CU"), srv.player(t, "Beto", "ES")
	ana.send(message{T: "create", Mode: modeClassic})
	beto.waitUntil(func(m message) bool { return m.T == "lobby" && len(m.Seeks) == 1 })
	ana.conn.Close(websocket.StatusNormalClosure, "")
	beto.waitUntil(func(m message) bool { return m.T == "lobby" && len(m.Seeks) == 0 })
	again := srv.connect(t, srv.logIn(t, "Ana", "CU"))
	again.mySeeks(0)
}

func TestNewConnectionReplacesTheOldOne(t *testing.T) {
	srv := newTestServer(t)
	sess := srv.signUp(t, "Ana", "CU")
	first := srv.connect(t, sess)
	srv.connect(t, sess)
	if m := first.waitFor("refused"); m.Reason == "" {
		t.Fatal("sin motivo")
	}
}
