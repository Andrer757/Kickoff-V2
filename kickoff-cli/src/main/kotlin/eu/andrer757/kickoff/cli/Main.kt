package eu.andrer757.kickoff.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.int
import eu.andrer757.kickoff.generator.ProjectGenerator
import eu.andrer757.kickoff.model.ProjectConfig
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class KickoffCommand : CliktCommand(name = "kickoff") {
    private val noColor by option("--no-color", help = "Disable ANSI color output (also honors NO_COLOR)").flag(default = false)

    override fun run() {
        // CLI emits plain semantic text with no ANSI sequences; flag kept for accessibility/NO_COLOR parity.
        @Suppress("UNUSED_EXPRESSION")
        noColor
    }
}

class NewCommand : CliktCommand(name = "new") {
    private val name by option("-n", "--name", help = "Project name").required()
    private val packageName by option("-p", "--package", help = "Application package name (e.g. com.example.app)").required()
    private val output by option("-o", "--output", help = "Target output directory").default("")
    private val template by option("-t", "--template", help = "Path to template directory").default("")
    private val type by option("--type", help = "Project archetype: app (Now-in-Android), sdk (Android SDK + Sample)").default("app")
    private val minSdk by option("--min-sdk", help = "Minimum SDK API level").int().default(ProjectConfig.DEFAULT_MIN_SDK)
    private val targetSdk by option("--target-sdk", help = "Target SDK API level").int().default(ProjectConfig.DEFAULT_TARGET_SDK)
    private val ai by option("--ai", help = "Target AI environment: copilot, claude, cursor, codex, generic, antigravity, all, none (supports comma-separated)").default("all")
    private val git by option("--git", help = "Initialize git repository").flag("--no-git", default = true)
    private val engram by option("--engram", help = "Opt in to Engram MCP memory (generates MCP configs; agents default to file-based state)").flag(default = false)
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
            templateDir = ProjectUtils.resolveTemplateDir(effectiveTemplate),
            minSdk = minSdk,
            targetSdk = targetSdk,
            aiEnvironments = if (aiSet.isEmpty()) setOf("all") else aiSet,
            projectType = type.lowercase(),
            gitInit = git,
            engram = engram
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
            throw ProgramResult(1)
        }
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
