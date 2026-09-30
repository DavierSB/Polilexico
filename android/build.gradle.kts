plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.android.library) apply false
  alias(libs.plugins.compose.compiler) apply false
}

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
