plugins {
    id("kickoff.android.library")
    id("kickoff.android.hilt")
}

android {
    namespace = "${configs.packageName}.core.connectivity"
}

dependencies {
    implementation(project(":core:common"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
}
