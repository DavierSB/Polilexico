plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.compose.compiler)
}

// Scrabble Sprint: encontrar el scrabble de cada mano antes de que acabe el reloj, con tres
// vidas. Recibe una SprintView y devuelve acciones; no conoce el motor.
android {
  namespace = "app.lexico.ui.sprint"
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
