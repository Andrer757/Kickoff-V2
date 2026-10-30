package pt.simdea.kickoff

import pt.simdea.kickoff.generator.ProjectGenerator
import pt.simdea.kickoff.model.ProjectConfig
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ProjectGeneratorTest {

    private fun getTemplateDir(): File {
        return File("../templates/now-in-android").let {
            if (it.exists()) it else File("templates/now-in-android")
        }
    }

    @Test
    fun `generator creates project structure and substitutes tokens`() {
        val tempDir = createTempDirectory("kickoff-test").toFile()
        try {
            val templateDir = getTemplateDir()

            val config = ProjectConfig(
                projectName = "Test Demo App",
                packageName = "com.test.demo",
                outputDir = tempDir.absolutePath,
                templateDir = templateDir.absolutePath,
                minSdk = 28,
                targetSdk = 35,
                aiEnvironments = setOf("all")
            )

            val result = ProjectGenerator().generate(config)

            assertEquals("success", result.status)
            assertEquals("Test Demo App", result.projectName)
            assertEquals("com.test.demo", result.packageName)
            assertTrue(result.modules.contains(":app"))
            assertTrue(result.modules.contains(":core:common"))
            assertTrue(result.modules.contains(":core:connectivity"))
            assertTrue(result.modules.contains(":feature:home"))

            // Check settings.gradle.kts
            val settingsFile = File(tempDir, "settings.gradle.kts")
            assertTrue(settingsFile.exists())
            assertTrue(settingsFile.readText().contains("rootProject.name = \"Test Demo App\""))

            // Check package mapping
            val mainActivity = File(tempDir, "app/src/main/kotlin/com/test/demo/MainActivity.kt")
            assertTrue(mainActivity.exists(), "MainActivity should exist at mapped package directory")
            val mainActivityContent = mainActivity.readText()
            assertTrue(mainActivityContent.contains("package com.test.demo"))
            assertTrue(mainActivityContent.contains("import com.test.demo.core.designsystem.AppTheme"))

            // Check minSdk & targetSdk substituted in convention plugin
            val appConvention = File(tempDir, "build-logic/convention/src/main/kotlin/AndroidApplicationConventionPlugin.kt")
            assertTrue(appConvention.exists())
            val appConventionContent = appConvention.readText()
            assertTrue(appConventionContent.contains("minSdk = 28"))
            assertTrue(appConventionContent.contains("targetSdk = 35"))

            // Check .gitignore exists in target
            val gitignore = File(tempDir, ".gitignore")
            assertTrue(gitignore.exists(), ".gitignore should exist in generated project")

            // Check AI directories exist for 'all'
            assertTrue(File(tempDir, ".claude/agents").exists())
            assertTrue(File(tempDir, ".github/agents").exists())
            assertTrue(File(tempDir, ".cursor/rules").exists())
            assertTrue(File(tempDir, ".agents/agents").exists())
            assertTrue(File(tempDir, "scripts/setup-ai.sh").exists())
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `generator escapes special characters in project name`() {
        val tempDir = createTempDirectory("kickoff-test-escape").toFile()
        try {
            val templateDir = getTemplateDir()

            val config = ProjectConfig(
                projectName = "Foo \"Bar\" & <Baz> \$Qux",
                packageName = "com.test.escape",
                outputDir = tempDir.absolutePath,
                templateDir = templateDir.absolutePath
            )

            val result = ProjectGenerator().generate(config)
            assertEquals("success", result.status)

            val settingsFile = File(tempDir, "settings.gradle.kts")
            assertTrue(settingsFile.exists())
            val settingsContent = settingsFile.readText()
            assertTrue(settingsContent.contains("rootProject.name = \"Foo \\\"Bar\\\" & <Baz> \\\$Qux\""))

            val manifestFile = File(tempDir, "app/src/main/AndroidManifest.xml")
            assertTrue(manifestFile.exists())
            val manifestContent = manifestFile.readText()
            assertTrue(manifestContent.contains("&amp;"))
            assertTrue(manifestContent.contains("&lt;Baz&gt;"))
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `generator rejects invalid package names`() {
        val tempDir = createTempDirectory("kickoff-test-invalid-pkg").toFile()
        try {
            val templateDir = getTemplateDir()
            val generator = ProjectGenerator()

            // Keyword in package
            val res1 = generator.generate(ProjectConfig("App", "com.fun.app", tempDir.absolutePath, templateDir.absolutePath))
            assertEquals("error", res1.status)
            assertTrue(res1.message!!.contains("reserved keyword"))

            // Single segment
            val res2 = generator.generate(ProjectConfig("App", "myapp", tempDir.absolutePath, templateDir.absolutePath))
            assertEquals("error", res2.status)
            assertTrue(res2.message!!.contains("at least two segments"))

            // Invalid characters
            val res3 = generator.generate(ProjectConfig("App", "com.my-app.test", tempDir.absolutePath, templateDir.absolutePath))
            assertEquals("error", res3.status)
            assertTrue(res3.message!!.contains("not a valid Java/Kotlin identifier"))
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `generator rejects invalid SDK configurations and names`() {
        val tempDir = createTempDirectory("kickoff-test-invalid-sdk").toFile()
        try {
            val templateDir = getTemplateDir()
            val generator = ProjectGenerator()

            // Blank name
            val res1 = generator.generate(ProjectConfig("  ", "com.test.app", tempDir.absolutePath, templateDir.absolutePath))
            assertEquals("error", res1.status)
            assertTrue(res1.message!!.contains("cannot be blank"))

            // minSdk <= 0
            val res2 = generator.generate(ProjectConfig("App", "com.test.app", tempDir.absolutePath, templateDir.absolutePath, minSdk = 0))
            assertEquals("error", res2.status)
            assertTrue(res2.message!!.contains("greater than 0"))

            // targetSdk < minSdk
            val res3 = generator.generate(ProjectConfig("App", "com.test.app", tempDir.absolutePath, templateDir.absolutePath, minSdk = 30, targetSdk = 28))
            assertEquals("error", res3.status)
            assertTrue(res3.message!!.contains("cannot be less than minSdk"))

            // invalid AI option
            val res4 = generator.generate(ProjectConfig("App", "com.test.app", tempDir.absolutePath, templateDir.absolutePath, aiEnvironments = setOf("invalid_ai")))
            assertEquals("error", res4.status)
            assertTrue(res4.message!!.contains("Invalid AI environment"))
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `generator with ai none excludes all ai directories and scripts`() {
        val tempDir = createTempDirectory("kickoff-test-none").toFile()
        try {
            val templateDir = getTemplateDir()

            val config = ProjectConfig(
                projectName = "Clean App",
                packageName = "com.test.clean",
                outputDir = tempDir.absolutePath,
                templateDir = templateDir.absolutePath,
                aiEnvironments = setOf("none")
            )

            val result = ProjectGenerator().generate(config)
            assertEquals("success", result.status)

            // Main app files exist
            assertTrue(File(tempDir, "app/src/main/kotlin/com/test/clean/MainActivity.kt").exists())
            assertTrue(File(tempDir, "gradlew").exists())

            // AI directories must NOT exist
            assertFalse(File(tempDir, ".claude").exists())
            assertFalse(File(tempDir, ".github").exists())
            assertFalse(File(tempDir, ".cursor").exists())
            assertFalse(File(tempDir, ".agents").exists())
            assertFalse(File(tempDir, "scripts/setup-ai.sh").exists())
            assertFalse(File(tempDir, "scripts/setup-ai.ps1").exists())
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `generator with ai claude includes only claude`() {
        val tempDir = createTempDirectory("kickoff-test-claude").toFile()
        try {
            val templateDir = getTemplateDir()

            val config = ProjectConfig(
                projectName = "Claude App",
                packageName = "com.test.claude",
                outputDir = tempDir.absolutePath,
                templateDir = templateDir.absolutePath,
                aiEnvironments = setOf("claude")
            )

            val result = ProjectGenerator().generate(config)
            assertEquals("success", result.status)

            assertTrue(File(tempDir, ".claude/agents").exists())
            assertTrue(File(tempDir, "scripts/setup-ai.sh").exists())

            assertFalse(File(tempDir, ".github").exists())
            assertFalse(File(tempDir, ".cursor").exists())
            assertFalse(File(tempDir, ".agents").exists())
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `generator with ai cursor includes cursor and agents skills`() {
        val tempDir = createTempDirectory("kickoff-test-cursor").toFile()
        try {
            val templateDir = getTemplateDir()

            val config = ProjectConfig(
                projectName = "Cursor App",
                packageName = "com.test.cursor",
                outputDir = tempDir.absolutePath,
                templateDir = templateDir.absolutePath,
                aiEnvironments = setOf("cursor")
            )

            val result = ProjectGenerator().generate(config)
            assertEquals("success", result.status)

            assertTrue(File(tempDir, ".cursor/rules").exists())
            assertTrue(File(tempDir, ".agents/skills").exists())
            assertTrue(File(tempDir, "scripts/setup-ai.sh").exists())

            assertFalse(File(tempDir, ".claude").exists())
            assertFalse(File(tempDir, ".github").exists())
        } finally {
            tempDir.deleteRecursively()
        }
    }
}
