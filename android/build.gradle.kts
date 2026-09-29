plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.android.library) apply false
  // Fija Kotlin (2.3.20) para todo el proyecto; lo usan los modulos con Compose.
  alias(libs.plugins.compose.compiler) apply false
}

// Compose (BOM 2026.03.01) pide algunas dependencias transitivas en versiones que no estan en
// el .gradle-home compartido; se suben a las que si estan (las mismas que usa WooglesMovil),
// asi el proyecto compila con --offline.
val offlineVersions = mapOf(
  "androidx.lifecycle:lifecycle-runtime-compose" to "2.10.0",
  "androidx.savedstate:savedstate-compose" to "1.4.0",
  "androidx.activity:activity-ktx" to "1.13.0",
  "androidx.core:core-ktx" to "1.18.0",
  "androidx.navigationevent:navigationevent-android" to "1.0.2",
  "androidx.tracing:tracing" to "1.2.0",
)

subprojects {
  configurations.configureEach {
    resolutionStrategy.eachDependency {
      offlineVersions["${requested.group}:${requested.name}"]?.let {
        useVersion(it)
        because("version disponible sin conexion")
      }
    }
  }
}
