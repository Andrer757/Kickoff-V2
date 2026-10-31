plugins {
    id("kickoff.android.application")
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "${configs.packageName}.sample"

    defaultConfig {
        applicationId = "${configs.packageName}.sample"
        versionCode = 1
        versionName = "1.0.0"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":sdk-core"))
    implementation(project(":sdk-ui"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(project(":sdk-testing"))
    testImplementation(libs.junit.jupiter)
}
