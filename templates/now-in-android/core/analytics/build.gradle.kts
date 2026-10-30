plugins {
    id("kickoff.android.library")
    id("kickoff.android.hilt")
}

android {
    namespace = "${configs.packageName}.core.analytics"
}
