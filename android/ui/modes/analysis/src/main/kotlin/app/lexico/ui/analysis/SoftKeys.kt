package app.lexico.ui.analysis

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import app.lexico.model.Letters

private const val SENTINEL = "··"

private val SHIFTED = mapOf('C' to "CH", 'L' to "LL", 'R' to "RR")

@Composable
internal fun SoftKeys(focus: FocusRequester, onTile: (String) -> Unit, onBack: () -> Unit) {
  var value by remember { mutableStateOf(resting()) }
  BasicTextField(
    value,
    onValueChange = { next ->
      if (next.text.length < SENTINEL.length) onBack() else next.text.drop(SENTINEL.length).mapNotNull(::tileOf).forEach(onTile)
      value = resting()
    },
    modifier = Modifier.size(1.dp).alpha(0f).focusRequester(focus),
    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, autoCorrectEnabled = false, keyboardType = KeyboardType.Password),
  )
}

private fun resting(): TextFieldValue = TextFieldValue(SENTINEL, TextRange(SENTINEL.length))

internal fun tileOf(c: Char): String? = when {
  c == '?' || c == ' ' -> Letters.BLANK
  c.isUpperCase() && c in SHIFTED -> SHIFTED.getValue(c)
  else -> c.uppercase().takeIf { it in Letters.ALL }
}
