package app.lexico.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun SwitchPrompt(text: String, onClick: () -> Unit) {
  Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
    TextButton(onClick = onClick) { ButtonText(text) }
  }
}
