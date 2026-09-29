pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("androidx.*")
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
    // com.android.library vive en el mismo jar que com.android.application; asi se resuelve sin
    // bajar su "plugin marker".
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "com.android.library") {
                useModule("com.android.tools.build:gradle:${requested.version}")
            }
        }
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google {
            content {
                includeGroupByRegex("androidx.*")
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
            }
        }
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "Lexico"

// engine-bridge -> el motor (../engine, en Go) compilado con gomobile como biblioteca Android,
//                  con su diccionario y una fachada Kotlin (WooglesEngine). Ver su README.
// model         -> fichas, tablero y notacion FISE. Sin nada visual ni del motor.
// game          -> las partidas en marcha para Android: llama al motor, escucha sus avisos,
//                  guarda y traduce. Sin reglas (son del motor) ni nada visual.
// ui:*          -> la interfaz (Compose), sin motor: tablero, piezas comunes, un modulo por
//                  modalidad y las partidas guardadas (ui:games).
// app           -> la aplicacion: arranque, navegacion y menu; ensambla todo lo anterior.
include(":engine-bridge")
include(":model")
include(":game")
include(":ui:common", ":ui:board")
include(":ui:modes:classic", ":ui:modes:duplicate", ":ui:modes:analysis", ":ui:modes:recall", ":ui:modes:sprint")
include(":ui:games")
include(":app")
