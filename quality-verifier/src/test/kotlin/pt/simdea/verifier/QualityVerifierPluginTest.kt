package pt.simdea.verifier

import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class QualityVerifierPluginTest {

    @Test
    fun `plugin registers extension successfully`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("pt.simdea.quality-verifier")

        val extension = project.extensions.findByType(QualityVerifierExtension::class.java)
        assertNotNull(extension)
        assertTrue(extension.enableDetekt.get())
        assertTrue(extension.enableSpotless.get())
        assertTrue(extension.enableLint.get())
        assertTrue(extension.enableKover.get())
    }
}
