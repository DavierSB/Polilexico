plugins {
  alias(libs.plugins.android.library)
}

// Lo que genera build_engine.sh: classes.jar, jni/<abi>/*.so y assets/data.
val goOutput = layout.buildDirectory.dir("gomobile")
// El motor en Go y lo que usa viven fuera del proyecto Android, en la raiz del repositorio.
val engine = rootProject.file("../engine")
val macondo = rootProject.file("../third_party/macondo")
val lexica = rootProject.file("../third_party/woogles-lexica")

// gomobile bind tarda unos minutos; Gradle solo lo repite si cambia engine/, la referencia al
// diccionario (sources.txt), macondo o el propio script.
val buildEngineGo = tasks.register<Exec>("buildEngineGo") {
  description = "Compila engine/ (macondo) con gomobile bind."
  inputs.file("build_engine.sh")
  inputs.files(fileTree(engine) { include("**/*.go", "go.mod", "go.sum") })
  inputs.files(lexica.resolve("sources.txt"), lexica.resolve("download.sh"))
  inputs.files(fileTree(macondo) {
    include("**/*.go", "go.mod", "go.sum")
    include("data/letterdistributions/spanish", "data/strategy/default/*.json", "data/strategy/default/*.csv")
  })
  outputs.dir(goOutput)
  commandLine("./build_engine.sh", goOutput.get().asFile.absolutePath)
}

android {
  namespace = "app.lexico.engine"
  compileSdk = 36
  defaultConfig {
    minSdk = 24
    consumerProguardFiles("consumer-rules.pro")
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  sourceSets {
    getByName("main") {
      // Rutas fijas: AGP 9 no admite Provider aqui. preBuild depende de buildEngineGo.
      jniLibs.srcDir(goOutput.get().dir("jni"))
      assets.srcDir(goOutput.get().dir("assets"))
    }
  }
  androidResources {
    // El diccionario (.kwg/.klv2) se copia tal cual al almacenamiento de la app.
    noCompress += listOf("kwg", "klv2")
  }
}

kotlin {
  jvmToolchain(17)
}

tasks.named("preBuild") { dependsOn(buildEngineGo) }

dependencies {
  // api: :game usa las clases que genera gomobile (app.lexico.go.*); quien dependa de :game no.
  api(files(goOutput.map { it.file("classes.jar") }).builtBy(buildEngineGo))
  testImplementation(libs.junit)
}
