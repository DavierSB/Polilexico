package app.lexico.ui.games

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import app.lexico.ui.classic.BotPhoto
import app.lexico.ui.common.LocalPhotos
import app.lexico.ui.common.PlayerAvatar
import app.lexico.ui.common.opensProfile

sealed interface Face {
  data object Bot : Face

  data class Person(val country: String = "", val id: String = "") : Face
}

internal fun ReviewView.faceOf(name: String): Face? = when (name) {
  me -> myFace
  opponent -> opponentFace
  else -> null
}

internal fun ReviewView.photoOf(name: String, size: Dp): (@Composable () -> Unit)? =
  faceOf(name)?.let { face -> { FacePhoto(face, name, size) } }

@Composable
private fun FacePhoto(face: Face, name: String, size: Dp) = when (face) {
  Face.Bot -> BotPhoto(name, size)
  is Face.Person -> PersonPhoto(face, name, size)
}

@Composable
private fun PersonPhoto(face: Face.Person, name: String, size: Dp) {
  Box(Modifier.opensProfile(plainName(name), face.country)) {
    PlayerAvatar(name, highlighted = false, size, LocalPhotos.current?.ofPlayer(name, face.country, face.id))
  }
}

private fun plainName(name: String): String = SEAT_COUNTRY.matchEntire(name)?.groupValues?.get(1) ?: name

private val SEAT_COUNTRY = Regex("^(.*) \\(([A-Z]{2})\\)$")
