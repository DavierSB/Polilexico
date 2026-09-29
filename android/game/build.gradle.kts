plugins {
  alias(libs.plugins.android.library)
}

// Las partidas en marcha para Android: arranca el motor, crea y continua partidas, escucha sus
// avisos, guarda en disco y traduce los tipos del motor. No tiene reglas (son del motor) ni nada
// visual; es lo unico que ve las clases de gomobile.
android {
  namespace = "app.lexico.game"
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
  api(project(":model"))
  implementation(project(":engine-bridge"))
  api(libs.kotlinx.coroutines.android)
  testImplementation(libs.junit)
}
