import com.android.build.api.variant.FilterConfiguration
import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
}

// La version: versionCode sube en cada APK que se publica.
val appVersionCode = 1
val appVersionName = "0.1"

// Las arquitecturas del motor (build_engine.sh) y lo que suma cada una al versionCode.
val abiCodes = mapOf("armeabi-v7a" to 1, "arm64-v8a" to 2)

// La aplicacion: arranque, navegacion entre pantallas, menu lateral e inicio. Es el unico
// modulo que conoce todos los demas y los ensambla.
android {
  namespace = "app.lexico"
  compileSdk = 36
  defaultConfig {
    applicationId = "app.lexico"
    minSdk = 24
    targetSdk = 36
    versionCode = appVersionCode
    versionName = appVersionName
  }
  signingConfigs {
    releaseKey()?.let { key ->
      create("release") {
        storeFile = file(key.getProperty("storeFile"))
        storePassword = key.getProperty("storePassword")
        keyAlias = key.getProperty("keyAlias")
        keyPassword = key.getProperty("keyPassword")
      }
    }
  }
  buildTypes {
    release {
      isMinifyEnabled = false
      // Sin keystore.properties el release sale sin firmar (app-*-release-unsigned.apk).
      signingConfig = signingConfigs.findByName("release")
    }
  }
  splits {
    // Un APK por arquitectura (el motor nativo es lo que mas pesa) y uno universal para
    // pasarlo de un telefono a otro sin saber cual es cual.
    abi {
      isEnable = true
      reset()
      include(*abiCodes.keys.toTypedArray())
      isUniversalApk = true
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  buildFeatures {
    compose = true
  }
  packaging {
    jniLibs {
      // El motor en Go pesa ~26 MB por arquitectura; comprimido dentro del APK ocupa bastante menos.
      useLegacyPackaging = true
      // El motor solo existe para ARM: sin esto el universal lleva las .so x86 de AndroidX y se
      // instalaria (y fallaria) en un x86.
      excludes += listOf("lib/x86/**", "lib/x86_64/**")
    }
  }
}

kotlin {
  jvmToolchain(17)
}

// Cada APK necesita su propio versionCode: appVersionCode * 10 + el de su arquitectura (el
// universal, + 0). arm64 va por encima para que un telefono de 64 bits se quede con el suyo.
androidComponents {
  onVariants { variant ->
    variant.outputs.forEach { output ->
      val abi = output.filters.find { it.filterType == FilterConfiguration.FilterType.ABI }?.identifier
      output.versionCode.set(appVersionCode * 10 + (abiCodes[abi] ?: 0))
    }
  }
}

dependencies {
  implementation(project(":model"))
  implementation(project(":game"))
  implementation(project(":ui:common"))
  implementation(project(":ui:board"))
  implementation(project(":ui:modes:classic"))
  implementation(project(":ui:modes:duplicate"))
  implementation(project(":ui:modes:analysis"))
  implementation(project(":ui:games"))
  implementation(project(":ui:modes:recall"))
  implementation(project(":ui:modes:sprint"))

  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.compose.foundation)
  implementation(libs.androidx.compose.material3)
}

/**
 * La clave de firma de release, de android/keystore.properties (fuera de git): storeFile,
 * storePassword, keyAlias y keyPassword. Null si el archivo no existe.
 */
fun releaseKey(): Properties? =
  rootProject.file("keystore.properties").takeIf { it.exists() }
    ?.let { file -> Properties().apply { file.inputStream().use(::load) } }
