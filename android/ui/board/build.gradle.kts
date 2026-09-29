plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.compose.compiler)
}

// El tablero y el atril: dibujarlos y colocar fichas a mano. No sabe de partidas, turnos ni
// motor; recibe un tablero y un atril y entrega la jugada que armo el jugador.
android {
  namespace = "app.lexico.ui.board"
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
  implementation(project(":ui:common"))
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.compose.foundation)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui.tooling.preview)
  debugImplementation(libs.androidx.compose.ui.tooling)
  testImplementation(libs.junit)
}
