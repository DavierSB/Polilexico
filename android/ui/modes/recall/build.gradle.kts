plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.compose.compiler)
}

// "¿Cuántas recuerdas?": se ve una partida rapida y despues hay que armar sus palabras mas
// valiosas a partir de sus letras. Recibe las partidas de una GameSource; no conoce el motor.
android {
  namespace = "app.lexico.ui.recall"
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
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
}
