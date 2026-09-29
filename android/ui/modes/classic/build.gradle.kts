plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.compose.compiler)
}

// La partida clasica contra un bot: su pantalla y sus componentes (marcadores, foto y mano
// del rival, movidas...). Recibe una ClassicView y devuelve acciones; no conoce el motor.
android {
  namespace = "app.lexico.ui.classic"
  compileSdk = 36
  defaultConfig {
    minSdk = 24
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  buildFeatures {
    compose = true
  }
}

kotlin {
  jvmToolchain(17)
}

dependencies {
  implementation(project(":ui:common"))
  implementation(project(":ui:board"))
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.compose.foundation)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui.tooling.preview)
  debugImplementation(libs.androidx.compose.ui.tooling)
}
