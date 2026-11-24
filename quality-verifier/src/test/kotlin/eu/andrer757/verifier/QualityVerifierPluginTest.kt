package eu.andrer757.verifier

import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.GradleException
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class QualityVerifierPluginTest {

    @Test
    fun `plugin registers extension and verifyArchitecture task successfully`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("eu.andrer757.quality-verifier")

        val extension = project.extensions.findByType(QualityVerifierExtension::class.java)
        assertNotNull(extension)
        assertTrue(extension.enableDetekt.get())
        assertTrue(extension.enableSpotless.get())
        assertTrue(extension.enableLint.get())
        assertTrue(extension.enableKover.get())
        assertTrue(extension.enableArchitectureCheck.get())
        assertEquals(0, extension.minCoveragePercent.get())

        val task = project.tasks.findByName("verifyArchitecture") as? VerifyArchitectureTask
        assertNotNull(task)
        task.verify()
    }

    @Test
    fun `afterEvaluate wires detekt spotless kover and abortOnError into per-project build dir`() {
        val project = ProjectBuilder.builder().withName("sample-lib").build()
        project.plugins.apply("eu.andrer757.quality-verifier")
        val extension = project.extensions.getByType(QualityVerifierExtension::class.java)
        extension.abortOnError.set(false)
        extension.minCoveragePercent.set(50)

        (project as ProjectInternal).evaluate()

        assertTrue(project.plugins.hasPlugin("io.gitlab.arturbosch.detekt"))
        assertTrue(project.plugins.hasPlugin("com.diffplug.spotless"))
        assertTrue(project.plugins.hasPlugin("org.jetbrains.kotlinx.kover"))

        val detektExt = project.extensions.findByType(DetektExtension::class.java)
        assertNotNull(detektExt)
        assertTrue(detektExt.ignoreFailures, "abortOnError=false should set detekt ignoreFailures=true")

        val perProjectConfig = project.layout.buildDirectory.file("quality-verifier/detekt-config.yml").get().asFile
        assertTrue(perProjectConfig.exists(), "Detekt config should be extracted into the module build directory")
        assertFalse(
            File(project.rootProject.projectDir, ".gradle/quality-verifier/detekt-config.yml").exists(),
            "Should not write shared config into root .gradle directory"
        )
    }

    @Test
    fun `verifyArchitecture is compatible with Gradle configuration cache`() {
        val testDir = createTempDirectory("qv-cc-test").toFile()
        try {
            File(testDir, "settings.gradle.kts").writeText(
                """
                rootProject.name = "cc-test"
                include(":core:model")
                """.trimIndent() + "\n"
            )
            File(testDir, "build.gradle.kts").writeText(
                """
                plugins {
                    id("eu.andrer757.quality-verifier") apply false
                }
                """.trimIndent() + "\n"
            )
            val modelDir = File(testDir, "core/model").apply { mkdirs() }
            File(modelDir, "build.gradle.kts").writeText(
                """
                plugins {
                    id("eu.andrer757.quality-verifier")
                }
                qualityVerifier {
                    enableDetekt.set(false)
                    enableSpotless.set(false)
                    enableLint.set(false)
                    enableKover.set(false)
                }
                """.trimIndent() + "\n"
            )

            val firstRun = GradleRunner.create()
                .withProjectDir(testDir)
                .withPluginClasspath()
                .withArguments(":core:model:verifyArchitecture", "--configuration-cache")
                .build()
            assertEquals(TaskOutcome.SUCCESS, firstRun.task(":core:model:verifyArchitecture")?.outcome)

            val secondRun = GradleRunner.create()
                .withProjectDir(testDir)
                .withPluginClasspath()
                .withArguments(":core:model:verifyArchitecture", "--configuration-cache")
                .build()
            assertTrue(secondRun.output.contains("Reusing configuration cache"))
        } finally {
            testDir.deleteRecursively()
        }
    }

    @Test
    fun `verifyArchitecture fails when feature depends on feature`() {
        val root = ProjectBuilder.builder().withName("root").build()
        val featureDir = ProjectBuilder.builder().withName("feature").withParent(root).build()
        val subFeatureA = ProjectBuilder.builder().withName("home").withParent(featureDir).build()
        val subFeatureB = ProjectBuilder.builder().withName("settings").withParent(featureDir).build()

        subFeatureA.plugins.apply("eu.andrer757.quality-verifier")
        subFeatureA.configurations.create("implementation")
        subFeatureA.dependencies.add("implementation", subFeatureB)

        val task = subFeatureA.tasks.getByName("verifyArchitecture") as VerifyArchitectureTask
        task.abortOnError.set(true)

        assertFailsWith<GradleException> {
            task.verify()
        }
    }

    @Test
    fun `verifyArchitecture fails when core model depends on internal module`() {
        val root = ProjectBuilder.builder().withName("root").build()
        val coreDir = ProjectBuilder.builder().withName("core").withParent(root).build()
        val modelModule = ProjectBuilder.builder().withName("model").withParent(coreDir).build()
        val networkModule = ProjectBuilder.builder().withName("network").withParent(coreDir).build()

        modelModule.plugins.apply("eu.andrer757.quality-verifier")
        modelModule.configurations.create("implementation")
        modelModule.dependencies.add("implementation", networkModule)

        val task = modelModule.tasks.getByName("verifyArchitecture") as VerifyArchitectureTask
        task.abortOnError.set(true)

        assertFailsWith<GradleException> {
            task.verify()
        }
    }

    @Test
    fun `verifyArchitecture fails when sdk core depends on sample`() {
        val root = ProjectBuilder.builder().withName("root").build()
        val sdkCore = ProjectBuilder.builder().withName("sdk-core").withParent(root).build()
        val sample = ProjectBuilder.builder().withName("sample").withParent(root).build()

        sdkCore.plugins.apply("eu.andrer757.quality-verifier")
        sdkCore.configurations.create("implementation")
        sdkCore.dependencies.add("implementation", sample)

        val task = sdkCore.tasks.getByName("verifyArchitecture") as VerifyArchitectureTask
        task.abortOnError.set(true)

        assertFailsWith<GradleException> {
            task.verify()
        }
    }
}
