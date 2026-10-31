package pt.andrer757.kickoff

import com.github.ajalt.clikt.core.parse
import pt.andrer757.kickoff.cli.ArchChecker
import pt.andrer757.kickoff.cli.DoctorService
import pt.andrer757.kickoff.generator.ProjectGenerator
import pt.andrer757.kickoff.model.ProjectConfig
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CommandsTest {

    private fun getTemplateDir(): File {
        return File("../templates/now-in-android").let {
            if (it.exists()) it else File("templates/now-in-android")
        }
    }

    private fun getSdkTemplateDir(): File {
        return File("../templates/android-sdk").let {
            if (it.exists()) it else File("templates/android-sdk")
        }
    }

    @Test
    fun `arch check passes on clean scaffolded app project`() {
        val tempDir = createTempDirectory("kickoff-arch-test").toFile()
        try {
            val templateDir = getTemplateDir()
            val config = ProjectConfig(
                projectName = "Arch App",
                packageName = "com.test.arch",
                outputDir = tempDir.absolutePath,
                templateDir = templateDir.absolutePath,
                aiEnvironments = setOf("none"),
                gitInit = false
            )

            ProjectGenerator().generate(config)

            val checkResult = ArchChecker.check(tempDir)
            assertEquals("success", checkResult.status)
            assertEquals(0, checkResult.violationsCount)
            assertTrue(checkResult.checkedModules.contains(":app"))
            assertTrue(checkResult.checkedModules.contains(":core:data"))
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `arch check detects cross-feature dependency in app project`() {
        val tempDir = createTempDirectory("kickoff-arch-fail").toFile()
        try {
            val templateDir = getTemplateDir()
            val config = ProjectConfig(
                projectName = "Arch Fail App",
                packageName = "com.test.archfail",
                outputDir = tempDir.absolutePath,
                templateDir = templateDir.absolutePath,
                aiEnvironments = setOf("none"),
                gitInit = false
            )

            ProjectGenerator().generate(config)

            // Inject illegal cross-feature dependency
            val homeBuild = File(tempDir, "feature/home/build.gradle.kts")
            homeBuild.appendText("\ndependencies { implementation(project(\":feature:settings\")) }\n")

            val checkResult = ArchChecker.check(tempDir)
            assertEquals("failure", checkResult.status)
            assertTrue(checkResult.violations.any { it.rule.contains("Feature modules must never depend on other feature modules") })
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `arch check passes on clean scaffolded sdk project`() {
        val tempDir = createTempDirectory("kickoff-arch-sdk").toFile()
        try {
            val sdkTemplateDir = getSdkTemplateDir()
            val config = ProjectConfig(
                projectName = "Arch SDK",
                packageName = "com.test.archsdk",
                outputDir = tempDir.absolutePath,
                templateDir = sdkTemplateDir.absolutePath,
                projectType = "sdk",
                aiEnvironments = setOf("none"),
                gitInit = false
            )

            ProjectGenerator().generate(config)

            val checkResult = ArchChecker.check(tempDir)
            assertEquals("success", checkResult.status)
            assertEquals(0, checkResult.violationsCount)
            assertTrue(checkResult.checkedModules.contains(":sdk-core"))
            assertTrue(checkResult.checkedModules.contains(":sample"))
            assertTrue(File(tempDir, "sdk-testing/src/main/kotlin/com/test/archsdk/sdk/testing/SdkTestRule.kt").exists())
            assertTrue(File(tempDir, "build-logic/quality-verifier/build.gradle.kts").exists())
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `doctor service returns non-empty diagnostic report`() {
        val report = DoctorService.diagnose(null)
        assertNotNull(report.status)
        assertTrue(report.items.isNotEmpty())
        assertTrue(report.items.any { it.name == "Java JDK" })
    }

    @Test
    fun `add feature creates feature files and wires navhost`() {
        val tempDir = createTempDirectory("kickoff-add-feature").toFile()
        try {
            val templateDir = getTemplateDir()
            val config = ProjectConfig(
                projectName = "Feature App",
                packageName = "com.test.featureapp",
                outputDir = tempDir.absolutePath,
                templateDir = templateDir.absolutePath,
                aiEnvironments = setOf("none"),
                gitInit = false
            )
            ProjectGenerator().generate(config)

            pt.andrer757.kickoff.cli.AddFeatureCommand().parse(listOf("-n", "profile", "-d", tempDir.absolutePath))

            val featureBuild = File(tempDir, "feature/profile/build.gradle.kts")
            assertTrue(featureBuild.exists(), "feature/profile/build.gradle.kts should exist")

            val screenFile = File(tempDir, "feature/profile/src/main/kotlin/com/test/featureapp/feature/profile/ProfileScreen.kt")
            assertTrue(screenFile.exists(), "ProfileScreen.kt should exist")

            val navFile = File(tempDir, "feature/profile/src/main/kotlin/com/test/featureapp/feature/profile/ProfileNavigation.kt")
            assertTrue(navFile.exists(), "ProfileNavigation.kt should exist")

            val settingsFile = File(tempDir, "settings.gradle.kts")
            assertTrue(settingsFile.readText().contains(":feature:profile"))

            val appBuild = File(tempDir, "app/build.gradle.kts")
            assertTrue(appBuild.readText().contains(":feature:profile"))

            val mainActivity = File(tempDir, "app/src/main/kotlin/com/test/featureapp/MainActivity.kt")
            assertTrue(mainActivity.readText().contains("ProfileRoute"))
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `add module creates core module in app and sdk module in sdk`() {
        val tempDirApp = createTempDirectory("kickoff-add-mod-app").toFile()
        val tempDirSdk = createTempDirectory("kickoff-add-mod-sdk").toFile()
        try {
            // App mode
            ProjectGenerator().generate(
                ProjectConfig("App", "com.test.modapp", tempDirApp.absolutePath, getTemplateDir().absolutePath, gitInit = false)
            )
            pt.andrer757.kickoff.cli.AddModuleCommand().parse(listOf("-n", "auth", "-d", tempDirApp.absolutePath))
            assertTrue(File(tempDirApp, "core/auth/build.gradle.kts").exists())
            assertTrue(File(tempDirApp, "settings.gradle.kts").readText().contains(":core:auth"))

            // SDK mode
            ProjectGenerator().generate(
                ProjectConfig("Sdk", "com.test.modsdk", tempDirSdk.absolutePath, getSdkTemplateDir().absolutePath, projectType = "sdk", gitInit = false)
            )
            pt.andrer757.kickoff.cli.AddModuleCommand().parse(listOf("-n", "analytics", "-d", tempDirSdk.absolutePath))
            assertTrue(File(tempDirSdk, "sdk-analytics/build.gradle.kts").exists())
            assertTrue(File(tempDirSdk, "settings.gradle.kts").readText().contains(":sdk-analytics"))
        } finally {
            tempDirApp.deleteRecursively()
            tempDirSdk.deleteRecursively()
        }
    }

    @Test
    fun `add entity scaffolds domain model entity dao and repository`() {
        val tempDir = createTempDirectory("kickoff-add-entity").toFile()
        try {
            ProjectGenerator().generate(
                ProjectConfig("App", "com.test.entityapp", tempDir.absolutePath, getTemplateDir().absolutePath, gitInit = false)
            )
            pt.andrer757.kickoff.cli.AddEntityCommand().parse(listOf("-n", "Product", "-f", "id:String,title:String,price:Double", "-d", tempDir.absolutePath))

            val modelFile = File(tempDir, "core/model/src/main/kotlin/com/test/entityapp/core/model/Product.kt")
            assertTrue(modelFile.exists())
            assertTrue(modelFile.readText().contains("val price: Double"))

            val entityFile = File(tempDir, "core/database/src/main/kotlin/com/test/entityapp/core/database/ProductEntity.kt")
            assertTrue(entityFile.exists())

            val daoFile = File(tempDir, "core/database/src/main/kotlin/com/test/entityapp/core/database/ProductDao.kt")
            assertTrue(daoFile.exists())

            val appDbFile = File(tempDir, "core/database/src/main/kotlin/com/test/entityapp/core/database/AppDatabase.kt")
            assertTrue(appDbFile.readText().contains("ProductEntity::class"))
            assertTrue(appDbFile.readText().contains("fun productDao(): ProductDao"))
            assertTrue(appDbFile.readText().contains("fun providesProductDao"))

            val repoFile = File(tempDir, "core/data/src/main/kotlin/com/test/entityapp/core/data/ProductRepository.kt")
            assertTrue(repoFile.exists())

            val dataModuleFile = File(tempDir, "core/data/src/main/kotlin/com/test/entityapp/core/data/DataModule.kt")
            assertTrue(dataModuleFile.readText().contains("bindsProductRepository"))
        } finally {
            tempDir.deleteRecursively()
        }
    }
}
