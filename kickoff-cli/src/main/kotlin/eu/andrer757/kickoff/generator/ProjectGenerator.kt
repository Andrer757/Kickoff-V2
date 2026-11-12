package eu.andrer757.kickoff.generator

import eu.andrer757.kickoff.model.GenerationResult
import eu.andrer757.kickoff.model.ProjectConfig
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class ProjectGenerator {

    private val textExtensions = setOf(
        "kt", "kts", "xml", "properties", "toml", "md", "gradle", "json", "yaml", "yml", "txt", "pro", "sh", "ps1", "mdc", "gitignore", "api"
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
        "copilot", "claude", "cursor", "codex", "generic", "antigravity", "all", "none"
    )

    fun validate(config: ProjectConfig): String? {
        if (config.projectName.isBlank()) {
            return "Project name cannot be blank."
        }
        if (config.projectName.any { it.isISOControl() }) {
            return "Project name cannot contain control characters or newlines."
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

        if (config.targetSdk > 35) {
            return "targetSdk (${config.targetSdk}) cannot exceed template compileSdk (35)."
        }

        if (config.projectType !in setOf("app", "sdk")) {
            return "Invalid project type '${config.projectType}'. Allowed values are: app, sdk."
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
                // AI directories and governance are handled by AiKitSynthesizer
                if (normalizedPath.startsWith(".agents") ||
                    normalizedPath.startsWith(".claude") ||
                    normalizedPath.startsWith(".cursor") ||
                    (normalizedPath.startsWith(".github") && !normalizedPath.startsWith(".github/workflows"))
                ) {
                    return@forEach
                }

                if (selectedAi.contains("none") && normalizedPath.startsWith("scripts/setup-ai")) {
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

            // Synthesize AI Kit
            val canonicalAgents = File(templateDir, ".agents").let {
                if (it.exists()) it else File(templateDir.parentFile, "now-in-android/.agents")
            }
            if (canonicalAgents.exists()) {
                AiKitSynthesizer().synthesize(canonicalAgents, targetDir, selectedAi, config.engram)
            }

            // Embed quality-verifier in composite build (build-logic/quality-verifier) for zero mavenLocal reliance
            embedQualityVerifier(templateDir, targetDir)

            // Git Initialization
            if (config.gitInit) {
                initializeGitRepository(targetDir)
            }

            val entryPoints = if (config.projectType == "sdk") {
                mapOf(
                    "sdkClient" to "sdk-core/src/main/kotlin/$packagePath/sdk/core/SdkClient.kt",
                    "sdkUiComponent" to "sdk-ui/src/main/kotlin/$packagePath/sdk/ui/SdkStatusBanner.kt",
                    "sampleActivity" to "sample/src/main/kotlin/$packagePath/sample/SampleMainActivity.kt"
                )
            } else {
                mapOf(
                    "application" to "app/src/main/kotlin/$packagePath/KickoffApplication.kt",
                    "mainActivity" to "app/src/main/kotlin/$packagePath/MainActivity.kt",
                    "homeScreen" to "feature/home/src/main/kotlin/$packagePath/feature/home/HomeScreen.kt"
                )
            }

            return GenerationResult(
                status = "success",
                projectPath = targetDir.absolutePath,
                projectName = config.projectName,
                packageName = config.packageName,
                projectType = config.projectType,
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

    private fun embedQualityVerifier(templateDir: File, targetDir: File) {
        val repoRoot = templateDir.parentFile?.parentFile
        val qvSource = listOfNotNull(
            repoRoot?.let { File(it, "quality-verifier") },
            File(System.getProperty("user.dir"), "quality-verifier")
        ).firstOrNull { it.exists() && File(it, "build.gradle.kts").exists() } ?: return

        val targetQvDir = File(targetDir, "build-logic/quality-verifier")
        targetQvDir.mkdirs()

        File(targetQvDir, "build.gradle.kts").writeText(
            toEmbeddedBuildScript(File(qvSource, "build.gradle.kts").readText(Charsets.UTF_8)),
            Charsets.UTF_8
        )
        val srcMain = File(qvSource, "src/main")
        if (srcMain.exists()) {
            srcMain.copyRecursively(File(targetQvDir, "src/main"), overwrite = true)
        }

        File(targetQvDir, "settings.gradle.kts").writeText(
            """
            dependencyResolutionManagement {
                repositories {
                    google()
                    mavenCentral()
                    gradlePluginPortal()
                }
            }

            rootProject.name = "quality-verifier"
            """.trimIndent() + "\n",
            Charsets.UTF_8
        )
    }

    /**
     * The upstream quality-verifier build script carries Maven Central publishing setup
     * (nmcp, signing, POM). Its nmcp plugin version is declared in Kickoff's own settings file,
     * so inside a generated project the plugin cannot resolve and Gradle configuration fails.
     * The embedded copy only needs to compile the plugin, so publishing concerns are removed.
     */
    internal fun toEmbeddedBuildScript(source: String): String {
        val publishingPlugins = Regex("""^\s*(id\("com\.gradleup\.nmcp"\)|`maven-publish`|signing)\s*$""")
        val withoutPlugins = source.lines().filterNot { publishingPlugins.matches(it) }.joinToString("\n")
        val withoutBlocks = listOf("publishing", "signing", "java", "nmcp").fold(withoutPlugins) { acc, block ->
            removeTopLevelBlock(acc, block)
        }
        return withoutBlocks.replace(Regex("\n{3,}"), "\n\n").trim() + "\n"
    }

    private fun removeTopLevelBlock(source: String, name: String): String {
        val start = Regex("""(?m)^$name\s*\{""").find(source) ?: return source
        var depth = 0
        var index = start.range.last
        while (index < source.length) {
            when (source[index]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) {
                        return source.removeRange(start.range.first, index + 1)
                    }
                }
            }
            index++
        }
        return source
    }

    private fun initializeGitRepository(targetDir: File) {
        try {
            ProcessBuilder("git", "init")
                .directory(targetDir)
                .redirectErrorStream(true)
                .start()
                .waitFor()

            ProcessBuilder("git", "add", ".")
                .directory(targetDir)
                .redirectErrorStream(true)
                .start()
                .waitFor()

            ProcessBuilder("git", "commit", "-m", "chore: initial project scaffolded by Kickoff")
                .directory(targetDir)
                .redirectErrorStream(true)
                .start()
                .waitFor()
        } catch (_: Exception) {
            // Non-fatal if git is missing or restricted
        }
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
                .replace("\${", "\\\${")
                .let { if (it.startsWith("@") || it.startsWith("?")) "\\$it" else it }
            "json" -> config.projectName
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
            else -> config.projectName
        }

        val rawReplaced = content
            .replace("\${configs.projectName}", escapedProjectName)
            .replace("\${configs.minSdk}", config.minSdk.toString())
            .replace("\${configs.targetSdk}", config.targetSdk.toString())

        return if (ext == "api") {
            rawReplaced.replace("\${configs.packageName}", config.packageName.replace('.', '/'))
        } else {
            val substituted = rawReplaced.replace("\${configs.packageName}", config.packageName)
            if (ext == "kt" || ext == "kts") sortKotlinImports(substituted) else substituted
        }
    }

    /**
     * Sorts contiguous Kotlin `import` blocks to match Ktlint's default ordering
     * (`*`, `java.**`, `javax.**`, `kotlin.**`, aliases) so arbitrary user package
     * names remain compliant with Spotless/Ktlint.
     */
    private fun sortKotlinImports(content: String): String {
        val importOrder = compareBy<String>(
            { line ->
                val target = line.removePrefix("import ").trim()
                when {
                    target.contains(" as ") -> 4
                    target.startsWith("kotlin.") -> 3
                    target.startsWith("javax.") -> 2
                    target.startsWith("java.") -> 1
                    else -> 0
                }
            },
            { it }
        )
        val lines = content.split("\n")
        val result = ArrayList<String>(lines.size)
        var i = 0
        while (i < lines.size) {
            if (lines[i].startsWith("import ")) {
                val start = i
                while (i < lines.size && lines[i].startsWith("import ")) {
                    i++
                }
                result.addAll(lines.subList(start, i).sortedWith(importOrder))
            } else {
                result.add(lines[i])
                i++
            }
        }
        return result.joinToString("\n")
    }
}
