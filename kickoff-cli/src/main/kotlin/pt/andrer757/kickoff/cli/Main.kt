package pt.andrer757.kickoff.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.int
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import pt.andrer757.kickoff.generator.ProjectGenerator
import pt.andrer757.kickoff.model.GenerationResult
import pt.andrer757.kickoff.model.ProjectConfig
import java.io.File
import kotlin.system.exitProcess

class KickoffCommand : CliktCommand(name = "kickoff") {
    override fun run() = Unit
}

class NewCommand : CliktCommand(name = "new") {
    private val name by option("-n", "--name", help = "Project name").required()
    private val packageName by option("-p", "--package", help = "Application package name (e.g. com.example.app)").required()
    private val output by option("-o", "--output", help = "Target output directory").default("")
    private val template by option("-t", "--template", help = "Path to template directory").default("")
    private val type by option("--type", help = "Project archetype: app (Now-in-Android), sdk (Android SDK + Sample)").default("app")
    private val minSdk by option("--min-sdk", help = "Minimum SDK API level").int().default(26)
    private val targetSdk by option("--target-sdk", help = "Target SDK API level").int().default(35)
    private val ai by option("--ai", help = "Target AI environment: copilot, claude, cursor, generic, antigravity, all, none (supports comma-separated)").default("all")
    private val git by option("--git", help = "Initialize git repository").flag("--no-git", default = true)
    private val json by option("--json", help = "Output result as structured JSON").flag(default = false)

    private val jsonSerializer = Json { prettyPrint = true }

    override fun run() {
        val targetOutput = if (output.isNotBlank()) {
            output
        } else {
            name.lowercase().replace(Regex("[^a-z0-9_-]"), "-")
        }

        val effectiveTemplate = if (template.isNotBlank()) {
            template
        } else if (type.lowercase() == "sdk") {
            "templates/android-sdk"
        } else {
            "templates/now-in-android"
        }

        val aiSet = ai.lowercase()
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()

        val config = ProjectConfig(
            projectName = name,
            packageName = packageName,
            outputDir = targetOutput,
            templateDir = resolveTemplateDir(effectiveTemplate),
            minSdk = minSdk,
            targetSdk = targetSdk,
            aiEnvironments = if (aiSet.isEmpty()) setOf("all") else aiSet,
            projectType = type.lowercase(),
            gitInit = git
        )

        val result = ProjectGenerator().generate(config)

        if (json) {
            echo(jsonSerializer.encodeToString(result))
        } else {
            if (result.status == "success") {
                echo(" Kickoff: Project '${result.projectName}' created successfully!")
                echo(" Location: ${result.projectPath}")
                echo(" Package:  ${result.packageName}")
                echo(" Modules:  ${result.modules.joinToString(", ")}")
                echo(" AI Suite: ${result.aiEnvironments.joinToString(", ")}")
                echo("\nNext steps:")
                echo("  cd ${result.projectPath}")
                echo("  ./gradlew assembleDebug")
                echo("  ./gradlew check")
            } else {
                echo(" Error: ${result.message}", err = true)
            }
        }

        if (result.status != "success") {
            exitProcess(1)
        }
    }

    private fun resolveTemplateDir(path: String): String {
        val directFile = File(path)
        if (directFile.exists()) return directFile.absolutePath

        val currentWorking = File(System.getProperty("user.dir"), path)
        if (currentWorking.exists()) return currentWorking.absolutePath

        val envTemplate = System.getenv("KICKOFF_TEMPLATE_DIR")
        if (!envTemplate.isNullOrBlank() && File(envTemplate).exists()) {
            return File(envTemplate).absolutePath
        }

        val appHome = System.getProperty("app.home") ?: System.getenv("APP_HOME")
        if (!appHome.isNullOrBlank()) {
            val appHomeTemplate = File(appHome, path)
            if (appHomeTemplate.exists()) return appHomeTemplate.absolutePath
        }

        try {
            val codeSourceLocation = NewCommand::class.java.protectionDomain?.codeSource?.location
            if (codeSourceLocation != null) {
                val jarOrClassFile = File(codeSourceLocation.toURI())
                val installDir = jarOrClassFile.parentFile?.parentFile
                if (installDir != null) {
                    val bundledTemplate = File(installDir, path)
                    if (bundledTemplate.exists()) return bundledTemplate.absolutePath

                    val repoTemplate = File(installDir, "../../$path").canonicalFile
                    if (repoTemplate.exists()) return repoTemplate.absolutePath
                }
            }
        } catch (_: Exception) {}

        return path
    }
}

fun main(args: Array<String>) {
    KickoffCommand()
        .subcommands(
            NewCommand(),
            AddCommand().subcommands(
                AddFeatureCommand(),
                AddModuleCommand(),
                AddEntityCommand()
            ),
            ArchCheckCommand(),
            DoctorCommand(),
            McpCommand()
        )
        .main(args)
}
