//go:build js && wasm

package main

import (
	"encoding/json"
	"syscall/js"
)

type handler func(args []json.RawMessage) (any, error)

var handlers = map[string]handler{}

func main() {
	registerEngine()
	registerClassic()
	registerDuplicate()
	registerSprint()
	registerReview()
	js.Global().Set("lexicoCall", js.FuncOf(receive))
	js.Global().Call("lexicoReady")
	select {}
}

func receive(_ js.Value, a []js.Value) any {
	id, name, raw := a[0].Int(), a[1].String(), a[2].String()
	go answer(id, name, raw)
	return nil
}

func answer(id int, name, raw string) {
	value, err := run(name, raw)
	if err != nil {
		reply(id, false, err.Error())
		return
	}
	reply(id, true, value)
}

func run(name, raw string) (any, error) {
	h, ok := handlers[name]
	if !ok {
		return nil, errUnknown(name)
	}
	var args []json.RawMessage
	if err := json.Unmarshal([]byte(raw), &args); err != nil {
		return nil, err
	}
	return h(args)
}

func reply(id int, ok bool, value any) {
	text, err := json.Marshal(value)
	if err != nil {
		ok, text = false, []byte(`"`+err.Error()+`"`)
	}
	js.Global().Call("lexicoReply", id, ok, string(text))
}

func changed(match int) {
	js.Global().Call("lexicoChanged", match)
}
