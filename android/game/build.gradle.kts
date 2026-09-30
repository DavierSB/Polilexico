plugins {
  alias(libs.plugins.android.library)
}

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
