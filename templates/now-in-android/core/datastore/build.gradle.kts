plugins {
    id("kickoff.android.library")
    id("kickoff.android.hilt")
}

android {
    namespace = "${configs.packageName}.core.datastore"
}

dependencies {
    api(libs.androidx.datastore.preferences)
    implementation(project(":core:common"))
}
