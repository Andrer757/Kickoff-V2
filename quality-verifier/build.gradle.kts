plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    `maven-publish`
    signing
    id("com.gradleup.nmcp")
}

java {
    withSourcesJar()
    withJavadocJar()
}

gradlePlugin {
    plugins {
        create("qualityVerifier") {
            id = "pt.andrer757.quality-verifier"
            implementationClass = "pt.andrer757.verifier.QualityVerifierPlugin"
            displayName = "Android Quality Verifier"
            description = "Static analysis plugin bundling Detekt (with Compose rules), Spotless/Ktlint, Android Lint, and Kover."
        }
    }
}

publishing {
    publications.withType<MavenPublication>().configureEach {
        pom {
            name.set("Android Quality Verifier")
            description.set("Static analysis and Clean Architecture verification plugin bundling Detekt (with Compose rules), Spotless/Ktlint, Android Lint, and Kover.")
            url.set("https://github.com/Andrer757/Kickoff-V2")
            licenses {
                license {
                    name.set("The Apache Software License, Version 2.0")
                    url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                }
            }
            developers {
                developer {
                    id.set("andrer757")
                    name.set("André Santos")
                    url.set("https://github.com/Andrer757")
                }
            }
            scm {
                connection.set("scm:git:git://github.com/Andrer757/Kickoff-V2.git")
                developerConnection.set("scm:git:ssh://github.com:Andrer757/Kickoff-V2.git")
                url.set("https://github.com/Andrer757/Kickoff-V2")
            }
        }
    }
}

signing {
    val signingKey = findProperty("signingKey")?.toString() ?: System.getenv("SIGNING_KEY")
    val signingPassword = findProperty("signingPassword")?.toString() ?: System.getenv("SIGNING_PASSWORD")
    if (!signingKey.isNullOrBlank()) {
        useInMemoryPgpKeys(signingKey, signingPassword)
        sign(publishing.publications)
    }
}

dependencies {
    compileOnly(gradleApi())
    compileOnly("com.android.tools.build:gradle:8.8.0")
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
