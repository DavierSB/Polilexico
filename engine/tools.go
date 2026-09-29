//go:build tools

// Mantiene golang.org/x/mobile en go.mod: lo necesita gomobile bind, con el que el puente de
// Android (android/engine-bridge) compila este modulo.
package engine

import _ "golang.org/x/mobile/bind"
