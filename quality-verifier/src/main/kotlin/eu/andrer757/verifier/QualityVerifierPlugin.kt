package eu.andrer757.verifier

import com.android.build.api.dsl.CommonExtension
import com.diffplug.gradle.spotless.SpotlessExtension
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.tasks.TaskProvider
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class QualityVerifierPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val extension = project.extensions.create("qualityVerifier", QualityVerifierExtension::class.java)

        // Apply Kover eagerly so its AGP androidComponents.onVariants callbacks register
        // before AGP finalizes variants regardless of plugin application order.
        project.pluginManager.apply("org.jetbrains.kotlinx.kover")

        val verifyArchTask = project.tasks.register("verifyArchitecture", VerifyArchitectureTask::class.java) {
            abortOnError.set(extension.abortOnError)
            modulePath.set(project.path)
            dependentProjectPaths.set(
                project.provider {
                    val paths = mutableSetOf<String>()
                    for (config in project.configurations) {
                        if (config.name in ARCH_CHECK_CONFIGURATIONS) {
                            for (dep in config.dependencies) {
                                if (dep is ProjectDependency) {
                                    paths.add(dep.path)
                                }
                            }
                        }
                    }
                    paths
                }
            )
            externalDeps.set(
                project.provider {
                    val deps = mutableSetOf<String>()
                    for (config in project.configurations) {
                        if (config.name in ARCH_CHECK_CONFIGURATIONS) {
                            for (dep in config.dependencies) {
                                if (dep !is ProjectDependency && dep.group != null) {
                                    deps.add("${dep.group}:${dep.name}")
                                }
                            }
                        }
                    }
                    deps
                }
            )
        }

        project.afterEvaluate {
            if (extension.enableDetekt.get()) {
                configureDetekt(project, extension)
            }
            if (extension.enableSpotless.get()) {
                configureSpotless(project)
            }
            if (extension.enableLint.get()) {
                configureLint(project, extension)
            }
            configureKover(project, extension)
            wireCheckLifecycle(project, extension, verifyArchTask)
        }
    }

    private fun configureDetekt(project: Project, extension: QualityVerifierExtension) {
        project.pluginManager.apply("io.gitlab.arturbosch.detekt")

        project.dependencies.add("detektPlugins", "io.nlopez.compose.rules:detekt:0.4.22")

        val detektExt = project.extensions.findByType(DetektExtension::class.java)
        if (detektExt != null) {
            detektExt.buildUponDefaultConfig = true
            detektExt.allRules = false
            detektExt.ignoreFailures = !extension.abortOnError.get()

            val configFile = if (extension.detektConfigFile.isPresent) {
                extension.detektConfigFile.get().asFile
            } else {
                extractResource(project, "detekt/conf-default.yml", "detekt-config.yml")
            }
            if (configFile.exists()) {
                detektExt.config.setFrom(configFile)
            }
        }
    }

    private fun configureSpotless(project: Project) {
        project.pluginManager.apply("com.diffplug.spotless")

        val spotlessExt = project.extensions.findByType(SpotlessExtension::class.java)
        if (spotlessExt != null) {
            spotlessExt.kotlin {
                target("src/**/*.kt")
                targetExclude("**/build/**", "**/.gradle/**")
                ktlint("1.5.0").editorConfigOverride(
                    mapOf(
                        "ktlint_standard_function-naming" to "disabled",
                        "indent_size" to "4"
                    )
                )
            }
            spotlessExt.kotlinGradle {
                target("*.gradle.kts", "build-logic/**/*.gradle.kts")
                targetExclude("**/build/**", "**/.gradle/**")
                ktlint("1.5.0")
            }
        }
    }

    private fun configureLint(project: Project, extension: QualityVerifierExtension) {
        val configureAndroidLint = {
            try {
                val androidExt = project.extensions.findByType(CommonExtension::class.java)
                if (androidExt != null) {
                    androidExt.lint.abortOnError = extension.abortOnError.get()

                    val configFile = if (extension.lintConfigFile.isPresent) {
                        extension.lintConfigFile.get().asFile
                    } else {
                        extractResource(project, "lint/conf-default.xml", "lint-config.xml")
                    }
                    if (configFile.exists()) {
                        androidExt.lint.lintConfig = configFile
                    }
                }
            } catch (e: Throwable) {
                project.logger.info("QualityVerifier: Android Lint configuration deferred or skipped: ${e.message}")
            }
        }

        if (project.plugins.hasPlugin("com.android.application") || project.plugins.hasPlugin("com.android.library")) {
            configureAndroidLint()
        } else {
            project.pluginManager.withPlugin("com.android.application") { configureAndroidLint() }
            project.pluginManager.withPlugin("com.android.library") { configureAndroidLint() }
        }
    }

    private fun configureKover(project: Project, extension: QualityVerifierExtension) {
        val koverExt = project.extensions.findByType(KoverProjectExtension::class.java) ?: return
        if (!extension.enableKover.get()) {
            koverExt.disable()
            return
        }
        val minCov = extension.minCoveragePercent.getOrElse(0)
        if (minCov > 0) {
            koverExt.reports {
                verify {
                    rule {
                        minBound(minCov)
                    }
                }
            }
        }
    }

    private fun wireCheckLifecycle(
        project: Project,
        extension: QualityVerifierExtension,
        verifyArchTask: TaskProvider<VerifyArchitectureTask>
    ) {
        project.tasks.findByName("check")?.let { checkTask ->
            if (extension.enableDetekt.get()) {
                project.tasks.findByName("detekt")?.let { checkTask.dependsOn(it) }
            }
            if (extension.enableSpotless.get()) {
                project.tasks.findByName("spotlessCheck")?.let { checkTask.dependsOn(it) }
            }
            if (extension.enableArchitectureCheck.get()) {
                checkTask.dependsOn(verifyArchTask)
            }
        }
    }

    private fun extractResource(project: Project, resourcePath: String, targetFileName: String): File {
        val targetDir = project.layout.buildDirectory.dir("quality-verifier").get().asFile
        targetDir.mkdirs()
        val targetFile = File(targetDir, targetFileName)

        val stream: InputStream? = javaClass.classLoader.getResourceAsStream(resourcePath)
        if (stream != null) {
            stream.use { input ->
                Files.copy(input, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        }
        return targetFile
    }

    private companion object {
        val ARCH_CHECK_CONFIGURATIONS = setOf("implementation", "api", "compileOnly", "runtimeOnly", "ksp")
    }
}
