package app.lexico.ui.classic

import androidx.compose.runtime.compositionLocalOf

/** Si la bolsa muestra las fichas por salir (bolsa y atril del rival) o solo cuantas quedan. Lo da la app, de sus opciones. */
val LocalShowUnseen = compositionLocalOf { true }
