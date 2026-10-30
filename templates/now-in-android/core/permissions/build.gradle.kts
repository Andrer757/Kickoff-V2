plugins {
    id("kickoff.android.library")
    id("kickoff.android.compose")
}

android {
    namespace = "${configs.packageName}.core.permissions"
}

dependencies {
    implementation(libs.androidx.activity.compose)
}
