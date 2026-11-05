package app.lexico.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import java.text.Normalizer
import java.util.Locale

data class AccountInput(val name: String, val country: String, val password: String, val email: String)

data class Country(val code: String, val name: String) {
  val flag: String get() = code.uppercase().map { String(Character.toChars(0x1F1E6 + (it - 'A'))) }.joinToString("")
}

val COUNTRIES: List<Country> by lazy {
  val spanish = Locale.forLanguageTag("es")
  Locale.getISOCountries().map { Country(it, Locale.Builder().setRegion(it).build().getDisplayCountry(spanish)) }
    .sortedWith(compareBy({ countryRank(it.code) }, { plain(it.name) }))
}

private fun countryRank(code: String): Int = when (code) {
  DEFAULT_COUNTRY -> 0
  in HISPANIC -> 1
  in FAVORITES -> 2 + FAVORITES.indexOf(code)
  else -> 2 + FAVORITES.size
}

const val DEFAULT_COUNTRY = "CU"

private val FAVORITES = listOf("FR", "US", "CA", "DE")

private val HISPANIC = setOf(
  "AR", "BO", "CL", "CO", "CR", "CU", "DO", "EC", "ES", "GQ", "GT", "HN", "MX", "NI", "PA", "PE", "PR", "PY", "SV", "UY", "VE",
)

fun flagOf(code: String): String = Country(code, "").flag

@Composable
fun OnlineAccountScreen(
  busy: Boolean, error: String?, onBack: () -> Unit, onSignUp: (AccountInput) -> Unit, onLogIn: (AccountInput) -> Unit,
) {
  var existing by rememberSaveable { mutableStateOf(false) }
  var input by remember { mutableStateOf(AccountInput("", DEFAULT_COUNTRY, "", "")) }
  FitColumn(10.dp) {
    Header("En línea", onBack)
    AccountCard {
      TwoOptions("Crear cuenta", "Ya tengo cuenta", existing) { existing = it }
      AccountFields(input, existing) { input = it }
      error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
      AccountButton(if (existing) "Entrar" else "Crear cuenta", busy, valid(input)) { if (existing) onLogIn(input) else onSignUp(input) }
    }
  }
}

@Composable
private fun AccountCard(content: @Composable ColumnScope.() -> Unit) {
  Column(
    Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp)).padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp), content = content,
  )
}

@Composable
private fun AccountFields(input: AccountInput, existing: Boolean, onChange: (AccountInput) -> Unit) {
  OutlinedTextField(
    input.name, { onChange(input.copy(name = it.take(NAME_CHARS).trim())) }, Modifier.fillMaxWidth(), singleLine = true,
    label = { Text("Usuario") },
  )
  CountryField(input.country) { onChange(input.copy(country = it)) }
  OutlinedTextField(
    input.password, { onChange(input.copy(password = it)) }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Contraseña") },
    visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
  )
  if (!existing) OutlinedTextField(
    input.email, { onChange(input.copy(email = it.trim())) }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Correo (opcional)") },
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
  )
}

@Composable
fun CountryField(code: String, onChange: (String) -> Unit) {
  var open by remember { mutableStateOf(false) }
  val chosen = COUNTRIES.find { it.code == code }
  Box {
    OutlinedTextField(
      chosen?.let { "${it.flag}  ${it.name}" }.orEmpty(), {}, Modifier.fillMaxWidth(), readOnly = true, singleLine = true,
      label = { Text("País") }, trailingIcon = { Text("▾") },
    )
    Box(Modifier.matchParentSize().clickable { open = true })
  }
  if (open) CountryDialog({ onChange(it); open = false }) { open = false }
}

@Composable
private fun CountryDialog(onPick: (String) -> Unit, onClose: () -> Unit) {
  var query by remember { mutableStateOf("") }
  val shown = remember(query) { COUNTRIES.filter { plain(it.name).contains(plain(query)) } }
  AlertDialog(
    onDismissRequest = onClose, title = { Text("País") }, confirmButton = {}, dismissButton = { TextButton(onClick = onClose) { Text("Cancelar") } },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Buscar") })
        LazyColumn(Modifier.heightIn(max = 360.dp)) { items(shown, key = { it.code }) { CountryRow(it, onPick) } }
      }
    },
  )
}

@Composable
private fun CountryRow(country: Country, onPick: (String) -> Unit) {
  Text("${country.flag}  ${country.name}", Modifier.fillMaxWidth().clickable { onPick(country.code) }.padding(vertical = 12.dp), maxLines = 1)
}

private fun plain(text: String): String =
  Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD).replace(DIACRITICS, "")

private val DIACRITICS = Regex("\\p{Mn}+")

@Composable
private fun AccountButton(text: String, busy: Boolean, valid: Boolean, onClick: () -> Unit) {
  Button(modifier = Modifier.fillMaxWidth().height(52.dp), enabled = valid && !busy, onClick = onClick) {
    if (busy) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp) else Text(text, fontWeight = FontWeight.Bold)
  }
}

private fun valid(input: AccountInput): Boolean = input.name.length >= 3 && input.country.length == 2 && input.password.length >= 6

private const val NAME_CHARS = 20
