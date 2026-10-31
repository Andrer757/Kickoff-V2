plugins {
    kotlin("jvm") version "2.0.21" apply false
    kotlin("plugin.serialization") version "2.0.21" apply false
}

allprojects {
    group = "eu.andrer757.kickoff"
    version = "1.0.0"

    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
