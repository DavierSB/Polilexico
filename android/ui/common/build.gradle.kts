plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.compose.compiler)
}

// Lo que comparten todas las pantallas: el tema y piezas comunes de las partidas (barra
// superior, bolsa, dialogos, relojes). Los modos no se ven entre si; lo comun va aqui.
android {
  namespace = "app.lexico.ui.common"
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
  api(project(":model"))
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.compose.foundation)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui.tooling.preview)
  debugImplementation(libs.androidx.compose.ui.tooling)
  testImplementation(libs.junit)
}
