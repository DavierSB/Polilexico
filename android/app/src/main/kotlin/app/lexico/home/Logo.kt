package app.lexico.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.lexico.R
import app.lexico.ui.common.Mulish

@Composable
fun Logo(modifier: Modifier = Modifier, scale: Float = 1f) {
  Row(modifier, verticalAlignment = Alignment.CenterVertically) {
    Image(painterResource(R.drawable.logo_shell), null, Modifier.width((40 * scale).dp))
    Text("Poliléxico", Modifier.padding(start = (10 * scale).dp), color = MaterialTheme.colorScheme.primary,
      fontFamily = Mulish, fontWeight = FontWeight.Black, fontSize = (28 * scale).sp, letterSpacing = (-0.03).em)
  }
}
