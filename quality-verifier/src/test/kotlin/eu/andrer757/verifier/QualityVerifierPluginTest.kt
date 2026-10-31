package eu.andrer757.verifier

import org.gradle.api.GradleException
import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
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
    fun `verifyArchitecture fails when feature depends on feature`() {
        val root = ProjectBuilder.builder().withName("root").build()
        val featureHome = ProjectBuilder.builder().withName("home").withParent(root).build()
        val featureSettings = ProjectBuilder.builder().withName("settings").withParent(root).build()

        // Name modules as :feature:home and :feature:settings
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
