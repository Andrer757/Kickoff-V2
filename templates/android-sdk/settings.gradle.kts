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

include(":sdk-core")
include(":sdk-ui")
include(":sdk-testing")
include(":sample")
