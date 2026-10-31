plugins {
    id("kickoff.sdk.library")
    id("kickoff.sdk.publish")
}

android {
    namespace = "${configs.packageName}.sdk.testing"
}

dependencies {
    api(project(":sdk-core"))
    api(libs.junit.jupiter)
    api(libs.kotlinx.coroutines.test)
    api(libs.turbine)
}
