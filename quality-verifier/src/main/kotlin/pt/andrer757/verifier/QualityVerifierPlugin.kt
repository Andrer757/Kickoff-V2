package pt.andrer757.verifier

import com.diffplug.gradle.spotless.SpotlessExtension
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class QualityVerifierPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val extension = project.extensions.create("qualityVerifier", QualityVerifierExtension::class.java)

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
            if (extension.enableKover.get()) {
                configureKover(project)
            }
            wireCheckLifecycle(project, extension)
        }
    }

    private fun configureDetekt(project: Project, extension: QualityVerifierExtension) {
        project.pluginManager.apply("io.gitlab.arturbosch.detekt")

        project.dependencies.add("detektPlugins", "io.nlopez.compose.rules:detekt:0.4.22")

        val detektExt = project.extensions.findByType(DetektExtension::class.java)
        if (detektExt != null) {
            detektExt.buildUponDefaultConfig = true
            detektExt.allRules = false

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
            val androidExt = project.extensions.findByName("android")
            if (androidExt != null) {
                try {
                    val lintMethod = androidExt.javaClass.getMethod("getLint")
                    val lintObj = lintMethod.invoke(androidExt)
                    val setAbortOnError = lintObj.javaClass.getMethod("setAbortOnError", Boolean::class.java)
                    setAbortOnError.invoke(lintObj, extension.abortOnError.get())

                    val configFile = if (extension.lintConfigFile.isPresent) {
                        extension.lintConfigFile.get().asFile
                    } else {
                        extractResource(project, "lint/conf-default.xml", "lint-config.xml")
                    }
                    if (configFile.exists()) {
                        val setLintConfig = lintObj.javaClass.getMethod("setLintConfig", File::class.java)
                        setLintConfig.invoke(lintObj, configFile)
                    }
                } catch (e: Exception) {
                    project.logger.info("QualityVerifier: Android Lint configuration deferred or not available: ${e.message}")
                }
            }
        }

        if (project.plugins.hasPlugin("com.android.application") || project.plugins.hasPlugin("com.android.library")) {
            configureAndroidLint()
        } else {
            project.pluginManager.withPlugin("com.android.application") { configureAndroidLint() }
            project.pluginManager.withPlugin("com.android.library") { configureAndroidLint() }
        }
    }

    private fun configureKover(project: Project) {
        project.pluginManager.apply("org.jetbrains.kotlinx.kover")
    }

    private fun wireCheckLifecycle(project: Project, extension: QualityVerifierExtension) {
        project.tasks.findByName("check")?.let { checkTask ->
            if (extension.enableDetekt.get()) {
                project.tasks.findByName("detekt")?.let { checkTask.dependsOn(it) }
            }
            if (extension.enableSpotless.get()) {
                project.tasks.findByName("spotlessCheck")?.let { checkTask.dependsOn(it) }
            }
        }
    }

    private fun extractResource(project: Project, resourcePath: String, targetFileName: String): File {
        val targetDir = File(project.layout.buildDirectory.asFile.get(), "quality-verifier")
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
}
