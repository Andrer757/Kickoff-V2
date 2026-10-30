plugins {
    id("kickoff.android.application")
    id("kickoff.android.compose")
    id("kickoff.android.hilt")
}

android {
    namespace = "${configs.packageName}"

    defaultConfig {
        applicationId = "${configs.packageName}"
        versionCode = 1
        versionName = "1.0.0"
    }
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:common"))
    implementation(project(":core:connectivity"))
    implementation(project(":core:analytics"))
    implementation(project(":core:permissions"))
    implementation(project(":core:network"))
    implementation(project(":core:database"))
    implementation(project(":core:datastore"))
    implementation(project(":feature:home"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
}
