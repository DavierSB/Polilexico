package app.lexico.games.export

import app.lexico.ui.classic.BOTS
import java.text.Normalizer

internal fun withBotAliases(gcg: String): String = BOTS.fold(gcg) { text, bot -> renamed(text, bot.name, bot.alias) }

private fun renamed(text: String, name: String, alias: String): String {
  val player = Regex("^(#player[12]) ${Regex.escape(nickname(name))} ${Regex.escape(name)}$", RegexOption.MULTILINE)
  return text.replace(player) { "${it.groupValues[1]} ${nickname(alias)} $alias" }.replace(">${nickname(name)}:", ">${nickname(alias)}:")
}

private fun nickname(name: String): String =
  Normalizer.normalize(name.lowercase(), Normalizer.Form.NFD).filter { it in 'a'..'z' || it in '0'..'9' }
