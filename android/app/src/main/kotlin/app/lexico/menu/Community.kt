package app.lexico.menu

import android.net.Uri
import androidx.compose.ui.graphics.Color

const val COMMUNITY_URL = "https://chat.whatsapp.com/KKFEZpvh2zQHRIoESqzgzQ"

const val AUTHOR_PHONE = "+53 55722267"

const val POLILEXICO_EMAIL = "polilexico@gmail.com"

val WHATSAPP_GREEN = Color(0xFF25D366)

fun authorChatUrl(version: String): String =
  "https://wa.me/${AUTHOR_PHONE.filter(Char::isDigit)}?text=" + Uri.encode("Hola, te escribo desde Poliléxico $version: ")

fun emailUrl(version: String): String = "mailto:$POLILEXICO_EMAIL?subject=" + Uri.encode("Poliléxico $version")
