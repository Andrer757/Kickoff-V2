plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    `maven-publish`
}

gradlePlugin {
    plugins {
        create("qualityVerifier") {
            id = "pt.simdea.quality-verifier"
            implementationClass = "pt.simdea.verifier.QualityVerifierPlugin"
            displayName = "Android Quality Verifier"
            description = "Static analysis plugin bundling Detekt (with Compose rules), Spotless/Ktlint, Android Lint, and Kover."
        }
    }
}

dependencies {
    compileOnly(gradleApi())
    implementation("io.gitlab.arturbosch.detekt:detekt-gradle-plugin:1.23.8")
    implementation("com.diffplug.spotless:spotless-plugin-gradle:7.0.2")
    implementation("org.jetbrains.kotlinx:kover-gradle-plugin:0.9.1")

    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}
