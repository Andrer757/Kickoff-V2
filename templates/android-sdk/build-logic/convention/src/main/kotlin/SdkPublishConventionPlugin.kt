import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.get

class SdkPublishConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("maven-publish")
                apply("org.jetbrains.dokka")
            }

            afterEvaluate {
                extensions.configure<PublishingExtension> {
                    publications {
                        create<MavenPublication>("release") {
                            from(components["release"])
                            groupId = "${configs.packageName}"
                            artifactId = project.name
                            version = project.version.toString().takeIf { it != "unspecified" } ?: "1.0.0-SNAPSHOT"

                            pom {
                                name.set("${configs.projectName} ${project.name}")
                                description.set("SDK library artifact for ${configs.projectName}")
                                url.set("https://github.com/example/repo")
                                licenses {
                                    license {
                                        name.set("The Apache Software License, Version 2.0")
                                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
