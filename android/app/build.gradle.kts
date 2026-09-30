import com.android.build.api.variant.FilterConfiguration
import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
}

val appVersionCode = 2
val appVersionName = "0.2"

val abiCodes = mapOf("armeabi-v7a" to 1, "arm64-v8a" to 2)

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
      signingConfig = signingConfigs.findByName("release")
    }
  }
  splits {
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
      useLegacyPackaging = true
      excludes += listOf("lib/x86/**", "lib/x86_64/**")
    }
  }
}

kotlin {
  jvmToolchain(17)
}

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

fun releaseKey(): Properties? =
  rootProject.file("keystore.properties").takeIf { it.exists() }
    ?.let { file -> Properties().apply { file.inputStream().use(::load) } }
