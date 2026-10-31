plugins {
    `kotlin-dsl`
}

group = "${configs.packageName}.buildlogic"

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.compiler.gradlePlugin)
    compileOnly(libs.dokka.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("sdkLibrary") {
            id = "kickoff.sdk.library"
            implementationClass = "SdkLibraryConventionPlugin"
        }
        register("sdkCompose") {
            id = "kickoff.sdk.compose"
            implementationClass = "SdkComposeConventionPlugin"
        }
        register("sdkPublish") {
            id = "kickoff.sdk.publish"
            implementationClass = "SdkPublishConventionPlugin"
        }
        register("androidApplication") {
            id = "kickoff.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
    }
}
