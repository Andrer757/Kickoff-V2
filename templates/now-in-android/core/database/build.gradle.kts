plugins {
    id("kickoff.android.library")
    id("kickoff.android.room")
    id("kickoff.android.hilt")
}

android {
    namespace = "${configs.packageName}.core.database"
}

dependencies {
    implementation(project(":core:model"))
}
