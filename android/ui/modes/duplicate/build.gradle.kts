plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.compose.compiler)
}

// La duplicada contra el master: su pantalla y sus componentes (reloj del turno, marcador,
// rondas...). Recibe una DuplicateView y devuelve acciones; no conoce el motor.
android {
  namespace = "app.lexico.ui.duplicate"
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
