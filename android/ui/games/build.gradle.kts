plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.compose.compiler)
}

// Las partidas guardadas: las que siguen en curso, las terminadas y la revision turno a turno
// de una terminada. Recibe listas y vistas ya armadas; no conoce el motor.
android {
  namespace = "app.lexico.ui.games"
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
  api(project(":ui:common"))
  implementation(project(":ui:board"))
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.compose.foundation)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui.tooling.preview)
  debugImplementation(libs.androidx.compose.ui.tooling)
}
