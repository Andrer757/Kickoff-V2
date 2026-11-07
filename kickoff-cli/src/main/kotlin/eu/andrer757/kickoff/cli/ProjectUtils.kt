package eu.andrer757.kickoff.cli

import java.io.File

object ProjectUtils {

    private val KOTLIN_KEYWORDS = setOf(
        "as", "break", "class", "continue", "do", "else", "false", "for", "fun",
        "if", "in", "interface", "is", "null", "object", "package", "return",
        "super", "this", "throw", "true", "try", "typealias", "typeof", "val",
        "var", "when", "while"
    )

    fun findProjectRoot(dir: File = File(System.getProperty("user.dir"))): File? {
        var current: File? = dir.absoluteFile
        var fallback: File? = null
        while (current != null) {
            val settings = File(current, "settings.gradle.kts")
            if (settings.exists()) {
                if (fallback == null) fallback = current
                // Prefer root containing :app, :sdk-core, or build-logic over nested build-logic/settings.gradle.kts
                if (File(current, "app").isDirectory || File(current, "sdk-core").isDirectory || File(current, "build-logic").isDirectory) {
                    return current
                }
            }
            current = current.parentFile
        }
        return fallback
    }

    fun isValidIdentifier(name: String): Boolean {
        val cleaned = name.trim()
        return cleaned.matches(Regex("^[A-Za-z][A-Za-z0-9_-]*$")) && cleaned.lowercase() !in KOTLIN_KEYWORDS
    }

    fun isContainedIn(root: File, candidate: File): Boolean {
        val rootCanonical = root.canonicalFile.toPath()
        val candidateCanonical = candidate.canonicalFile.toPath()
        return candidateCanonical.startsWith(rootCanonical)
    }

    fun detectPackageName(projectRoot: File): String? {
        val appBuild = File(projectRoot, "app/build.gradle.kts")
        if (appBuild.exists()) {
            val content = appBuild.readText()
            val match = Regex("""namespace\s*=\s*"([^"]+)"""").find(content)
            if (match != null) {
                return match.groupValues[1]
            }
        }

        val sampleBuild = File(projectRoot, "sample/build.gradle.kts")
        if (sampleBuild.exists()) {
            val content = sampleBuild.readText()
            val match = Regex("""namespace\s*=\s*"([^"]+)\.sample"""").find(content)
            if (match != null) {
                return match.groupValues[1]
            }
        }

        val coreModel = File(projectRoot, "core/model/build.gradle.kts")
        if (coreModel.exists()) {
            val content = coreModel.readText()
            val match = Regex("""namespace\s*=\s*"([^"]+)\.core\.model"""").find(content)
            if (match != null) {
                return match.groupValues[1]
            }
        }

        return null
    }

    fun isSdkProject(projectRoot: File): Boolean {
        val settings = File(projectRoot, "settings.gradle.kts")
        if (settings.exists()) {
            val text = settings.readText()
            return text.contains(":sdk-core") || text.contains("sdk-core")
        }
        return false
    }
}
