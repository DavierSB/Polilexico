plugins {
  alias(libs.plugins.android.library)
}

// Fichas, tablero y notacion FISE: lo que comparten la interfaz y el juego. Sin Compose ni
// motor, asi que sus pruebas corren en la JVM de la PC.
android {
  namespace = "app.lexico.model"
  compileSdk = 36
  defaultConfig {
    minSdk = 24
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
}

kotlin {
  jvmToolchain(17)
}

dependencies {
  testImplementation(libs.junit)
}
