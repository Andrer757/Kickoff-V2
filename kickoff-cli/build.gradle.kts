plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    application
}

application {
    mainClass.set("eu.andrer757.kickoff.cli.MainKt")
}

dependencies {
    implementation("com.github.ajalt.clikt:clikt:5.0.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")

    testImplementation(kotlin("test"))
}

distributions {
    main {
        contents {
            from("${rootProject.projectDir}/templates") {
                into("templates")
            }
            from("${rootProject.projectDir}/quality-verifier") {
                into("quality-verifier")
                exclude("build", ".gradle", "src/test")
            }
        }
    }
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}
