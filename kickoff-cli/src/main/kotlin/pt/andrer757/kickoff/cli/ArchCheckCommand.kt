package pt.andrer757.kickoff.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.system.exitProcess

@Serializable
data class ArchViolation(
    val module: String,
    val targetModule: String,
    val rule: String,
    val file: String
)

@Serializable
data class ArchCheckResult(
    val status: String,
    val violationsCount: Int,
    val violations: List<ArchViolation>,
    val checkedModules: List<String>
)

object ArchChecker {

    fun check(projectRoot: File): ArchCheckResult {
        val settingsFile = File(projectRoot, "settings.gradle.kts")
        if (!settingsFile.exists()) {
            return ArchCheckResult(
                status = "error",
                violationsCount = 0,
                violations = emptyList(),
                checkedModules = emptyList()
            )
        }

        val settingsText = settingsFile.readText()
        val moduleRegex = Regex("""include\("([^"]+)"""")
        val modules = moduleRegex.findAll(settingsText).map { it.groupValues[1] }.toList()

        val isSdk = modules.any { it.startsWith(":sdk-") }
        val violations = mutableListOf<ArchViolation>()

        for (module in modules) {
            val moduleRelPath = module.removePrefix(":").replace(':', File.separatorChar)
            val buildFile = File(projectRoot, "$moduleRelPath/build.gradle.kts")
            if (!buildFile.exists()) continue

            val content = buildFile.readText()
            val depRegex = Regex("""(?:implementation|api|compileOnly|runtimeOnly)\s*\(\s*project\("([^"]+)"\)\s*\)""")
            val dependencies = depRegex.findAll(content).map { it.groupValues[1] }.toSet()

            if (module == ":sdk-core" && (content.contains("kickoff.android.hilt") || content.contains("libs.hilt.") || content.contains("com.google.dagger:hilt"))) {
                violations.add(
                    ArchViolation(
                        module = module,
                        targetModule = "dagger.hilt",
                        rule = "Headless engine :sdk-core must maintain a zero-Hilt public surface.",
                        file = buildFile.absolutePath
                    )
                )
            }
            if (module == ":core:model" && (content.contains("libs.androidx.") || content.contains("androidx."))) {
                violations.add(
                    ArchViolation(
                        module = module,
                        targetModule = "androidx.*",
                        rule = "Innermost domain module :core:model must not depend on Android framework libraries.",
                        file = buildFile.absolutePath
                    )
                )
            }

            for (dep in dependencies) {
                if (isSdk) {
                    if (module.startsWith(":sdk-") && dep == ":sample") {
                        violations.add(
                            ArchViolation(
                                module = module,
                                targetModule = dep,
                                rule = "SDK modules must never depend on the :sample application.",
                                file = buildFile.absolutePath
                            )
                        )
                    }
                    if (module == ":sdk-core" && (dep == ":sdk-ui" || dep == ":sdk-testing")) {
                        violations.add(
                            ArchViolation(
                                module = module,
                                targetModule = dep,
                                rule = "Headless engine :sdk-core must never depend on :sdk-ui or :sdk-testing.",
                                file = buildFile.absolutePath
                            )
                        )
                    }
                } else {
                    // App Clean Architecture Rules
                    if (module == ":core:model" && dep != module) {
                        violations.add(
                            ArchViolation(
                                module = module,
                                targetModule = dep,
                                rule = "Innermost domain module :core:model must never depend on other modules.",
                                file = buildFile.absolutePath
                            )
                        )
                    }
                    if (module.startsWith(":feature:")) {
                        if (dep.startsWith(":feature:") && dep != module) {
                            violations.add(
                                ArchViolation(
                                    module = module,
                                    targetModule = dep,
                                    rule = "Feature modules must never depend on other feature modules.",
                                    file = buildFile.absolutePath
                                )
                            )
                        }
                        if (dep in setOf(":core:database", ":core:network", ":core:datastore")) {
                            violations.add(
                                ArchViolation(
                                    module = module,
                                    targetModule = dep,
                                    rule = "Feature modules must not bypass repository layer to depend directly on storage ($dep).",
                                    file = buildFile.absolutePath
                                )
                            )
                        }
                    }
                    if (module.startsWith(":core:") && module != ":core:testing") {
                        if (dep.startsWith(":feature:")) {
                            violations.add(
                                ArchViolation(
                                    module = module,
                                    targetModule = dep,
                                    rule = "Core module must not depend on feature module ($dep).",
                                    file = buildFile.absolutePath
                                )
                            )
                        }
                        if (dep == ":app") {
                            violations.add(
                                ArchViolation(
                                    module = module,
                                    targetModule = dep,
                                    rule = "Core module must not depend on :app.",
                                    file = buildFile.absolutePath
                                )
                            )
                        }
                    }
                }
            }
        }

        return ArchCheckResult(
            status = if (violations.isEmpty()) "success" else "failure",
            violationsCount = violations.size,
            violations = violations,
            checkedModules = modules
        )
    }
}

class ArchCheckCommand : CliktCommand(name = "arch-check") {
    private val projectDir by option("-d", "--project-dir", help = "Project root directory").default("")
    private val json by option("--json", help = "Output result as structured JSON").flag(default = false)

    private val jsonSerializer = Json { prettyPrint = true }

    override fun run() {
        val root = if (projectDir.isNotBlank()) File(projectDir) else ProjectUtils.findProjectRoot()
        if (root == null || !root.exists()) {
            val errorMsg = "Could not find a Kickoff project (settings.gradle.kts missing)."
            if (json) {
                echo(jsonSerializer.encodeToString(ArchCheckResult("error", 0, emptyList(), emptyList())))
            } else {
                echo(" Error: $errorMsg", err = true)
            }
            exitProcess(1)
        }

        val result = ArchChecker.check(root)

        if (json) {
            echo(jsonSerializer.encodeToString(result))
        } else {
            echo(" Kickoff Architecture Check (${result.checkedModules.size} modules scanned)")
            if (result.violations.isEmpty()) {
                echo(" Clean Architecture verification passed! All module boundaries intact.")
            } else {
                echo(" Found ${result.violations.size} architecture violation(s):", err = true)
                for (v in result.violations) {
                    echo("   • [${v.module} -> ${v.targetModule}] ${v.rule}", err = true)
                }
            }
        }

        if (result.status != "success") {
            exitProcess(1)
        }
    }
}
