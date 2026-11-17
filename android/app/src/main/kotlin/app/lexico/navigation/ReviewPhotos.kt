package app.lexico.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import app.lexico.online.OnlinePhotos
import app.lexico.ui.common.LocalPhotos
import app.lexico.ui.common.PhotoSource

@Composable
internal fun ProvideRecordPhotos(wanted: Boolean, content: @Composable () -> Unit) {
  if (!wanted) return content()
  val online = rememberOnline()
  val source = remember(online) { RecordPhotos(online.photos) }
  CompositionLocalProvider(LocalPhotos provides source, content = content)
}

private class RecordPhotos(private val photos: OnlinePhotos) : PhotoSource {
  @Composable
  override fun of(name: String, country: String): ImageBitmap? {
    LaunchedEffect(name, country) { photos.request(name, country) }
    return photos.images[OnlinePhotos.key(name, country)]
  }

  @Composable
  override fun ofSeat(seatName: String): ImageBitmap? {
    val found = SEAT_COUNTRY.matchEntire(seatName) ?: return null
    return of(found.groupValues[1], found.groupValues[2])
  }

  @Composable
  override fun ofPlayer(name: String, country: String, id: String): ImageBitmap? = when {
    id.isNotEmpty() -> byId(id)
    country.isNotEmpty() -> of(SEAT_COUNTRY.matchEntire(name)?.groupValues?.get(1) ?: name, country)
    else -> ofSeat(name)
  }

  @Composable
  private fun byId(id: String): ImageBitmap? {
    LaunchedEffect(id) { photos.requestId(id) }
    return photos.images[OnlinePhotos.idKey(id)]
  }
}

private val SEAT_COUNTRY = Regex("^(.*) \\(([A-Z]{2})\\)$")
