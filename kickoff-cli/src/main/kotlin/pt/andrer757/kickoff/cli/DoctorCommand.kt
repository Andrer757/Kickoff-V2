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
data class DiagnosticItem(
    val name: String,
    val category: String,
    val passed: Boolean,
    val details: String,
    val recommendation: String? = null
)

@Serializable
data class DoctorReport(
    val status: String,
    val items: List<DiagnosticItem>
)

object DoctorService {

    fun diagnose(projectRoot: File?): DoctorReport {
        val items = mutableListOf<DiagnosticItem>()

        // 1. Java Runtime Check
        val javaVersion = System.getProperty("java.version") ?: "unknown"
        val javaVendor = System.getProperty("java.vendor") ?: "unknown"
        val isJavaOk = javaVersion.startsWith("17") || javaVersion.startsWith("21") || javaVersion.startsWith("25")
        items.add(
            DiagnosticItem(
                name = "Java JDK",
                category = "Toolchain",
                passed = isJavaOk,
                details = "$javaVendor JDK $javaVersion",
                recommendation = if (!isJavaOk) "Use JDK 17 or JDK 21 (Temurin-21 recommended)." else null
            )
        )

        // 2. Android SDK Check
        val androidHome = System.getenv("ANDROID_HOME") ?: System.getenv("ANDROID_SDK_ROOT")
        val hasAndroidSdk = !androidHome.isNullOrBlank() && File(androidHome).exists()
        val hasApi35 = hasAndroidSdk && File(androidHome, "platforms/android-35").exists()

        items.add(
            DiagnosticItem(
                name = "Android SDK",
                category = "Toolchain",
                passed = hasAndroidSdk,
                details = if (hasAndroidSdk) "Located at $androidHome" else "ANDROID_HOME not found in environment",
                recommendation = if (!hasAndroidSdk) "Install Android SDK and export ANDROID_HOME." else null
            )
        )

        items.add(
            DiagnosticItem(
                name = "Android API 35 Platform",
                category = "Toolchain",
                passed = hasApi35,
                details = if (hasApi35) "android-35 installed" else "android-35 platform package missing",
                recommendation = if (!hasApi35) "Install via SDK manager: sdkmanager 'platforms;android-35'" else null
            )
        )

        // 3. Git Check
        val hasGit = isCommandAvailable("git")
        items.add(
            DiagnosticItem(
                name = "Git Version Control",
                category = "Toolchain",
                passed = hasGit,
                details = if (hasGit) "git command is available" else "git command not found",
                recommendation = if (!hasGit) "Install git package on host machine." else null
            )
        )

        // 4. Project Root & Gradle Wrapper Check (if in project)
        if (projectRoot != null && projectRoot.exists()) {
            val gradlew = File(projectRoot, "gradlew")
            val hasGradlew = gradlew.exists() && gradlew.canExecute()
            items.add(
                DiagnosticItem(
                    name = "Gradle Wrapper",
                    category = "Project",
                    passed = hasGradlew,
                    details = if (hasGradlew) "gradlew found and executable" else "gradlew missing or not executable in $projectRoot",
                    recommendation = if (!hasGradlew) "Run chmod +x gradlew or scaffold with Kickoff." else null
                )
            )
        }

        // 5. Optional AI Tools
        val aiTools = listOf(
            "code" to "VS Code CLI (code)",
            "claude" to "Claude Code CLI (claude)",
            "android" to "Google Android CLI (android)",
            "engram" to "Engram MCP Local Memory (engram)",
            "graphify" to "Graphify Codebase Knowledge Graph (graphify)"
        )

        for ((bin, label) in aiTools) {
            val available = isCommandAvailable(bin)
            items.add(
                DiagnosticItem(
                    name = label,
                    category = "AI Ecosystem",
                    passed = available,
                    details = if (available) "Found in PATH" else "Optional tool not found in PATH",
                    recommendation = if (!available) "Run ./scripts/setup-ai.sh to install optional tool." else null
                )
            )
        }

        val allCorePassed = items.filter { it.category == "Toolchain" }.all { it.passed }

        return DoctorReport(
            status = if (allCorePassed) "success" else "degraded",
            items = items
        )
    }

    private fun isCommandAvailable(cmd: String): Boolean {
        return try {
            val process = ProcessBuilder("which", cmd)
                .redirectErrorStream(true)
                .start()
            process.waitFor() == 0
        } catch (_: Exception) {
            false
        }
    }
}

class DoctorCommand : CliktCommand(name = "doctor") {
    private val projectDir by option("-d", "--project-dir", help = "Project root directory").default("")
    private val strict by option("--strict", help = "Exit with code 1 if any toolchain check fails").flag(default = false)
    private val json by option("--json", help = "Output result as structured JSON").flag(default = false)

    private val jsonSerializer = Json { prettyPrint = true }

    override fun run() {
        val root = if (projectDir.isNotBlank()) File(projectDir) else ProjectUtils.findProjectRoot()
        val report = DoctorService.diagnose(root)

        if (json) {
            echo(jsonSerializer.encodeToString(report))
        } else {
            echo(" Kickoff Doctor Environment Report")
            echo("--------------------------------------------------------------------------------")
            var lastCat = ""
            for (item in report.items) {
                if (item.category != lastCat) {
                    echo("\n [${item.category}]")
                    lastCat = item.category
                }
                val mark = if (item.passed) "✓" else if (item.category == "AI Ecosystem") "⚠" else "✗"
                echo("   $mark ${item.name}: ${item.details}")
                if (!item.passed && item.recommendation != null) {
                    echo("     👉 ${item.recommendation}")
                }
            }
            echo("--------------------------------------------------------------------------------")
            if (report.status == "success") {
                echo(" Overall Status: Ready to develop and scaffold Android applications!")
            } else {
                echo(" Overall Status: Degraded toolchain dependencies detected.")
            }
        }

        if (strict && report.status != "success") {
            val coreFailed = report.items.filter { it.category == "Toolchain" }.any { !it.passed }
            if (coreFailed) exitProcess(1)
        }
    }
}
