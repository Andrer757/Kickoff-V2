plugins {
    id("kickoff.android.library")
    id("kickoff.android.hilt")
}

android {
    namespace = "${configs.packageName}.core.common"
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
}
