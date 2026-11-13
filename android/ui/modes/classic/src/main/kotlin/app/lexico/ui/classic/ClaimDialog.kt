package app.lexico.ui.classic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.lexico.ui.common.VerdictDialog
import app.lexico.ui.common.VerdictLine
import app.lexico.ui.common.VerdictTone

@Composable
internal fun ClaimDialog(claim: ClaimVerdict, opponent: String, close: () -> Unit) {
  VerdictDialog(claimTitle(claim), claimTone(claim), close, titleSize = 40.sp, celebrate = false) {
    ClaimedWords(claim)
    VerdictLine(claimLine(claim, opponent))
  }
}

private fun claimTitle(claim: ClaimVerdict): String = when {
  claim.mine && claim.upheld -> "Jugada anulada"
  claim.mine -> "Jugada válida"
  claim.upheld -> "Te reclamaron"
  else -> "Reclamación fallida"
}

private fun claimTone(claim: ClaimVerdict): VerdictTone = when {
  !claim.upheld -> VerdictTone.NEUTRAL
  claim.mine -> VerdictTone.WIN
  else -> VerdictTone.LOSS
}

private fun claimLine(claim: ClaimVerdict, opponent: String): String = when {
  claim.mine && claim.upheld -> "$opponent pierde ${claim.points} puntos"
  claim.mine -> "Sigues en tu turno"
  claim.upheld -> "Pierdes ${claim.points} puntos y el turno"
  else -> "Tu jugada es válida"
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ClaimedWords(claim: ClaimVerdict) {
  FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
    claim.words.forEach { ClaimedWord(it, it in claim.invalid) }
  }
}

@Composable
private fun ClaimedWord(word: String, invalid: Boolean) {
  Text(
    word, color = if (invalid) VerdictTone.LOSS.color else VerdictTone.WIN.color, fontSize = 22.sp, fontWeight = FontWeight.Bold,
    letterSpacing = 0.7.sp, textDecoration = if (invalid) TextDecoration.LineThrough else null,
  )
}
