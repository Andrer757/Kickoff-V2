plugins {
    id("kickoff.android.library")
}

android {
    namespace = "${configs.packageName}.core.testing"
}

dependencies {
    api(project(":core:model"))
    api(project(":core:common"))
    api(project(":core:data"))
    api(project(":core:connectivity"))
    api(project(":core:analytics"))

    api(libs.junit.jupiter)
    api(libs.kotlinx.coroutines.test)
    api(libs.turbine)
}
