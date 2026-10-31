plugins {
    id("kickoff.sdk.library")
    id("kickoff.sdk.publish")
}

android {
    namespace = "${configs.packageName}.sdk.core"
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(project(":sdk-testing"))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
