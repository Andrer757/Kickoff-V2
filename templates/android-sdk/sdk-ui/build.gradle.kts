plugins {
    id("kickoff.sdk.library")
    id("kickoff.sdk.compose")
    id("kickoff.sdk.publish")
}

android {
    namespace = "${configs.packageName}.sdk.ui"
}

dependencies {
    api(project(":sdk-core"))
    implementation(libs.androidx.core.ktx)

    testImplementation(project(":sdk-testing"))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.kotlinx.coroutines.test)
}
