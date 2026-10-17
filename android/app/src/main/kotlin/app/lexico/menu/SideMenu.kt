package app.lexico.menu

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.lexico.home.Logo
import app.lexico.update.Release

@Composable
fun SideMenu(
  atHome: Boolean, update: Release?, onHome: () -> Unit, onStats: () -> Unit, onTheme: () -> Unit, onOptions: () -> Unit,
  onAbout: () -> Unit, onUpdate: (Release) -> Unit,
) {
  ModalDrawerSheet {
    Logo(Modifier.padding(16.dp), scale = 0.8f)
    MenuItem("Inicio", selected = atHome, onHome)
    MenuItem("Mis estadísticas", selected = false, onStats)
    HorizontalDivider(Modifier.padding(vertical = 8.dp))
    MenuItem("Tema", selected = false, onTheme)
    MenuItem("Opciones", selected = false, onOptions)
    HorizontalDivider(Modifier.padding(vertical = 8.dp))
    MenuItem("Acerca de", selected = false, onAbout)
    update?.let { UpdateItem(it) { onUpdate(it) } }
  }
}

@Composable
private fun MenuItem(text: String, selected: Boolean, onClick: () -> Unit) {
  NavigationDrawerItem(label = { Text(text) }, selected = selected, onClick = onClick)
}

@Composable
private fun UpdateItem(update: Release, onClick: () -> Unit) {
  NavigationDrawerItem(
    label = { Text("Versión ${update.version} disponible", color = MaterialTheme.colorScheme.primary) },
    selected = false,
    onClick = onClick,
    badge = { Badge(containerColor = MaterialTheme.colorScheme.primary) },
  )
}
