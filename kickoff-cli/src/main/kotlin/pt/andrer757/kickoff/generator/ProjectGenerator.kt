package pt.andrer757.kickoff.generator

import pt.andrer757.kickoff.model.GenerationResult
import pt.andrer757.kickoff.model.ProjectConfig
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class ProjectGenerator {

    private val textExtensions = setOf(
        "kt", "kts", "xml", "properties", "toml", "md", "gradle", "json", "yaml", "yml", "txt", "pro", "sh", "ps1", "mdc", "gitignore"
    )

    private val reservedKeywords = setOf(
        // Kotlin keywords
        "as", "break", "class", "continue", "do", "else", "false", "for", "fun",
        "if", "in", "interface", "is", "null", "object", "package", "return",
        "super", "this", "throw", "true", "try", "typealias", "typeof", "val",
        "var", "when", "while",
        // Java keywords
        "abstract", "assert", "boolean", "byte", "case", "catch", "char", "const",
        "default", "double", "enum", "extends", "final", "finally", "float",
        "goto", "implements", "import", "instanceof", "int", "long", "native",
        "new", "private", "protected", "public", "short", "static", "strictfp",
        "switch", "synchronized", "throws", "transient", "void", "volatile"
    )

    private val validAiEnvironments = setOf(
        "copilot", "claude", "cursor", "generic", "antigravity", "all", "none"
    )

    fun validate(config: ProjectConfig): String? {
        if (config.projectName.isBlank()) {
            return "Project name cannot be blank."
        }

        val packageValidation = validatePackageName(config.packageName)
        if (packageValidation != null) {
            return packageValidation
        }

        if (config.minSdk <= 0) {
            return "minSdk must be greater than 0 (got ${config.minSdk})."
        }

        if (config.targetSdk < config.minSdk) {
            return "targetSdk (${config.targetSdk}) cannot be less than minSdk (${config.minSdk})."
        }

        val invalidAi = config.aiEnvironments.filter { it !in validAiEnvironments }
        if (invalidAi.isNotEmpty()) {
            return "Invalid AI environment(s): ${invalidAi.joinToString(", ")}. Allowed values are: ${validAiEnvironments.joinToString(", ")}."
        }

        return null
    }

    private fun validatePackageName(pkg: String): String? {
        if (pkg.isBlank()) return "Package name cannot be blank."
        val segments = pkg.split('.')
        if (segments.size < 2) {
            return "Package name '$pkg' must contain at least two segments separated by dots (e.g., com.example.app)."
        }
        val segmentRegex = Regex("^[a-zA-Z_][a-zA-Z0-9_]*$")
        for (segment in segments) {
            if (!segmentRegex.matches(segment)) {
                return "Package segment '$segment' in '$pkg' is not a valid Java/Kotlin identifier."
            }
            if (segment.lowercase() in reservedKeywords) {
                return "Package segment '$segment' in '$pkg' is a reserved keyword in Java/Kotlin."
            }
        }
        return null
    }

    fun generate(config: ProjectConfig): GenerationResult {
        val validationError = validate(config)
        if (validationError != null) {
            return GenerationResult(
                status = "error",
                message = validationError
            )
        }

        val templateDir = File(config.templateDir)
        if (!templateDir.exists() || !templateDir.isDirectory) {
            return GenerationResult(
                status = "error",
                message = "Template directory '${templateDir.absolutePath}' does not exist or is not a directory."
            )
        }

        val targetDir = File(config.outputDir)
        val targetExisted = targetDir.exists()
        if (targetExisted && targetDir.listFiles()?.isNotEmpty() == true) {
            return GenerationResult(
                status = "error",
                message = "Target directory '${targetDir.absolutePath}' already exists and is not empty."
            )
        }

        try {
            if (!targetExisted) {
                targetDir.mkdirs()
            }

            val packagePath = config.packageName.replace('.', File.separatorChar)
            val modules = mutableListOf<String>()
            val selectedAi = config.aiEnvironments.map { it.lowercase() }.toSet()

            templateDir.walkTopDown().forEach { file ->
                val relativePath = file.relativeTo(templateDir).path
                if (relativePath.isEmpty()) return@forEach

                val normalizedPath = relativePath.replace('\\', '/')
                if (!shouldIncludePath(normalizedPath, selectedAi)) {
                    return@forEach
                }

                val pathSegments = relativePath.split(File.separatorChar).map { segment ->
                    when (segment) {
                        "app_package" -> packagePath
                        "gitignore" -> ".gitignore"
                        else -> segment
                    }
                }
                val mappedPath = pathSegments.joinToString(File.separator)
                val destinationFile = File(targetDir, mappedPath)

                if (file.isDirectory) {
                    destinationFile.mkdirs()
                    if (file.parentFile == templateDir && File(file, "build.gradle.kts").exists()) {
                        modules.add(":${file.name}")
                    } else if (file.parentFile?.parentFile == templateDir && File(file, "build.gradle.kts").exists()) {
                        modules.add(":${file.parentFile.name}:${file.name}")
                    }
                } else {
                    destinationFile.parentFile?.mkdirs()
                    if (isTextFile(file)) {
                        val rawContent = file.readText(Charsets.UTF_8)
                        val transformedContent = substituteTokens(rawContent, config, file)
                        destinationFile.writeText(transformedContent, Charsets.UTF_8)
                    } else {
                        Files.copy(file.toPath(), destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
                    }

                    if (file.name == "gradlew" || file.name.endsWith(".sh")) {
                        destinationFile.setExecutable(true, false)
                    }
                }
            }

            val entryPoints = mapOf(
                "application" to "app/src/main/kotlin/$packagePath/KickoffApplication.kt",
                "mainActivity" to "app/src/main/kotlin/$packagePath/MainActivity.kt",
                "homeScreen" to "feature/home/src/main/kotlin/$packagePath/feature/home/HomeScreen.kt"
            )

            return GenerationResult(
                status = "success",
                projectPath = targetDir.absolutePath,
                projectName = config.projectName,
                packageName = config.packageName,
                modules = modules.sorted(),
                aiEnvironments = selectedAi.sorted(),
                entryPoints = entryPoints
            )
        } catch (e: Exception) {
            if (!targetExisted && targetDir.exists()) {
                targetDir.deleteRecursively()
            }
            return GenerationResult(
                status = "error",
                message = "Project generation failed: ${e.message}"
            )
        }
    }

    private fun shouldIncludePath(path: String, selectedAi: Set<String>): Boolean {
        if (selectedAi.contains("none")) {
            if (path.startsWith(".github") ||
                path.startsWith(".claude") ||
                path.startsWith(".cursor") ||
                path.startsWith(".agents") ||
                path.startsWith("scripts/setup-ai")
            ) {
                return false
            }
            return true
        }

        if (selectedAi.contains("all")) {
            return true
        }

        if (path.startsWith(".github")) {
            return selectedAi.contains("copilot")
        }
        if (path.startsWith(".claude")) {
            return selectedAi.contains("claude")
        }
        if (path.startsWith(".cursor")) {
            return selectedAi.contains("cursor")
        }
        if (path.startsWith(".agents")) {
            return selectedAi.contains("generic") || selectedAi.contains("cursor") || selectedAi.contains("antigravity")
        }
        if (path.startsWith("scripts/setup-ai")) {
            return selectedAi.any { it in setOf("copilot", "claude", "cursor", "generic", "antigravity") }
        }

        return true
    }

    private fun isTextFile(file: File): Boolean {
        if (file.name == "gradlew") return true
        val ext = file.extension.lowercase()
        return ext in textExtensions || file.name.startsWith(".git")
    }

    private fun substituteTokens(content: String, config: ProjectConfig, file: File): String {
        val ext = file.extension.lowercase()
        val escapedProjectName = when (ext) {
            "kt", "kts", "gradle" -> config.projectName
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\$", "\\\$")
            "xml" -> config.projectName
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;")
            "json" -> config.projectName
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
            else -> config.projectName
        }

        return content
            .replace("\${configs.projectName}", escapedProjectName)
            .replace("\${configs.packageName}", config.packageName)
            .replace("\${configs.minSdk}", config.minSdk.toString())
            .replace("\${configs.targetSdk}", config.targetSdk.toString())
    }
}
