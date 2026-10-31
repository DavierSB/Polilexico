package app.lexico.menu

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemColors
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.lexico.home.Logo

@Composable
fun SideMenu(
  atHome: Boolean, hasUpdate: Boolean, progress: Float?, onHome: () -> Unit, onStats: () -> Unit, onTheme: () -> Unit, onOptions: () -> Unit,
  onAbout: () -> Unit, onUpdate: () -> Unit,
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
    UpdateItem(hasUpdate, progress, onUpdate)
  }
}

@Composable
private fun MenuItem(text: String, selected: Boolean, onClick: () -> Unit) {
  NavigationDrawerItem(label = { Text(text) }, selected = selected, onClick = onClick)
}

private val UPDATE_YELLOW = Color(0xFFFFC107)

@Composable
private fun UpdateItem(hasUpdate: Boolean, progress: Float?, onClick: () -> Unit) {
  NavigationDrawerItem(
    label = { Text("Actualizar") },
    selected = false,
    onClick = { if (hasUpdate && progress == null) onClick() },
    badge = { progress?.let { Text("${(it * 100).toInt()}%") } },
    colors = updateColors(hasUpdate),
  )
}

@Composable
private fun updateColors(hasUpdate: Boolean): NavigationDrawerItemColors =
  if (hasUpdate) NavigationDrawerItemDefaults.colors(
    unselectedContainerColor = UPDATE_YELLOW, unselectedTextColor = Color.Black, unselectedBadgeColor = Color.Black,
  )
  else NavigationDrawerItemDefaults.colors(unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
