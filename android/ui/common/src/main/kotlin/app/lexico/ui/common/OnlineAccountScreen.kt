package app.lexico.ui.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import java.util.Locale

data class AccountInput(val name: String, val country: String, val password: String, val email: String)

data class Country(val code: String, val name: String) {
  val flag: String get() = code.uppercase().map { String(Character.toChars(0x1F1E6 + (it - 'A'))) }.joinToString("")
}

val COUNTRIES: List<Country> by lazy {
  val spanish = Locale.forLanguageTag("es")
  Locale.getISOCountries().map { Country(it, Locale.Builder().setRegion(it).build().getDisplayCountry(spanish)) }.sortedBy { it.name }
}

fun flagOf(code: String): String = Country(code, "").flag

@Composable
fun OnlineAccountScreen(
  busy: Boolean, error: String?, onBack: () -> Unit, onServer: () -> Unit, onSignUp: (AccountInput) -> Unit, onLogIn: (AccountInput) -> Unit,
) {
  var existing by rememberSaveable { mutableStateOf(false) }
  var input by remember { mutableStateOf(AccountInput("", Locale.getDefault().country, "", "")) }
  FitColumn(10.dp) {
    Header("En línea", onBack) { BarTextButton("Servidor", onServer) }
    TwoOptions("Crear cuenta", "Ya tengo cuenta", existing) { existing = it }
    AccountFields(input, existing) { input = it }
    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    AccountButton(if (existing) "Entrar" else "Crear cuenta", busy, valid(input)) { if (existing) onLogIn(input) else onSignUp(input) }
  }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountryField(code: String, onChange: (String) -> Unit) {
  var open by remember { mutableStateOf(false) }
  val chosen = COUNTRIES.find { it.code == code }
  ExposedDropdownMenuBox(open, { open = it }) {
    OutlinedTextField(
      chosen?.let { "${it.flag}  ${it.name}" }.orEmpty(), {}, Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
      readOnly = true, singleLine = true, label = { Text("País") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(open) },
    )
    ExposedDropdownMenu(open, { open = false }) {
      COUNTRIES.forEach { c -> DropdownMenuItem({ Text("${c.flag}  ${c.name}") }, { onChange(c.code); open = false }) }
    }
  }
}

@Composable
private fun AccountButton(text: String, busy: Boolean, valid: Boolean, onClick: () -> Unit) {
  Button(modifier = Modifier.fillMaxWidth().height(52.dp), enabled = valid && !busy, onClick = onClick) {
    if (busy) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp) else Text(text, fontWeight = FontWeight.Bold)
  }
}

private fun valid(input: AccountInput): Boolean = input.name.length >= 3 && input.country.length == 2 && input.password.length >= 6

private const val NAME_CHARS = 20
