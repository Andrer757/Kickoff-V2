pluginManagement {
    includeBuild("build-logic")
    if (file("build-logic/quality-verifier").exists()) {
        includeBuild("build-logic/quality-verifier")
    }
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        mavenLocal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        mavenLocal()
    }
}

rootProject.name = "${configs.projectName}"

include(":app")
include(":core:model")
include(":core:common")
include(":core:connectivity")
include(":core:analytics")
include(":core:permissions")
include(":core:network")
include(":core:database")
include(":core:datastore")
include(":core:data")
include(":core:testing")
include(":core:designsystem")
include(":core:ui")
include(":feature:home")
