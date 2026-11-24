package eu.andrer757.kickoff.cli

import java.io.File

object ProjectUtils {

    private val KOTLIN_KEYWORDS = setOf(
        "as", "break", "class", "continue", "do", "else", "false", "for", "fun",
        "if", "in", "interface", "is", "null", "object", "package", "return",
        "super", "this", "throw", "true", "try", "typealias", "typeof", "val",
        "var", "when", "while"
    )

    private val JAVA_KEYWORDS = setOf(
        "abstract", "assert", "boolean", "byte", "case", "catch", "char", "const",
        "default", "double", "enum", "extends", "final", "finally", "float",
        "goto", "implements", "import", "instanceof", "int", "long", "native",
        "new", "private", "protected", "public", "short", "static", "strictfp",
        "switch", "synchronized", "throws", "transient", "void", "volatile"
    )

    val RESERVED_KEYWORDS: Set<String> = KOTLIN_KEYWORDS + JAVA_KEYWORDS

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

    fun isReservedKeyword(name: String): Boolean = name.lowercase() in RESERVED_KEYWORDS

    fun isValidIdentifier(name: String): Boolean {
        val cleaned = name.trim()
        return cleaned.matches(Regex("^[A-Za-z][A-Za-z0-9_-]*$")) && !isReservedKeyword(cleaned)
    }

    fun isValidKotlinFieldName(name: String): Boolean {
        val cleaned = name.trim()
        return cleaned.matches(Regex("^[a-zA-Z_][a-zA-Z0-9_]*$")) && !isReservedKeyword(cleaned)
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

    fun resolveTemplateDir(path: String): String {
        val directFile = File(path)
        if (directFile.exists()) return directFile.absolutePath

        val currentWorking = File(System.getProperty("user.dir"), path)
        if (currentWorking.exists()) return currentWorking.absolutePath

        val envTemplate = System.getenv("KICKOFF_TEMPLATE_DIR")
        if (!envTemplate.isNullOrBlank()) {
            val envRoot = File(envTemplate)
            val templateName = path.removePrefix("templates/")
            val nestedByName = File(envRoot, templateName)
            if (nestedByName.exists() && nestedByName.isDirectory) {
                return nestedByName.absolutePath
            }
            val nestedByRel = File(envRoot, path)
            if (nestedByRel.exists() && nestedByRel.isDirectory) {
                return nestedByRel.absolutePath
            }
            if (envRoot.exists() && File(envRoot, "settings.gradle.kts").exists()) {
                return envRoot.absolutePath
            }
        }

        val appHome = System.getProperty("app.home") ?: System.getenv("APP_HOME")
        if (!appHome.isNullOrBlank()) {
            val appHomeTemplate = File(appHome, path)
            if (appHomeTemplate.exists()) return appHomeTemplate.absolutePath
        }

        findInstallDir()?.let { installDir ->
            val bundledTemplate = File(installDir, path)
            if (bundledTemplate.exists()) return bundledTemplate.absolutePath

            val repoTemplate = File(installDir, "../../$path").canonicalFile
            if (repoTemplate.exists()) return repoTemplate.absolutePath
        }

        val parentRepoTemplate = File("../$path")
        if (parentRepoTemplate.exists()) return parentRepoTemplate.absolutePath

        return path
    }

    fun resolveQualityVerifierDir(templateDir: File): File? {
        val appHome = System.getProperty("app.home") ?: System.getenv("APP_HOME")
        val installDir = findInstallDir()
        val candidates = listOfNotNull(
            templateDir.parentFile?.parentFile?.let { File(it, "quality-verifier") },
            File(System.getProperty("user.dir"), "quality-verifier"),
            appHome?.takeIf { it.isNotBlank() }?.let { File(it, "quality-verifier") },
            installDir?.let { File(it, "quality-verifier") },
            installDir?.let { File(it, "../../quality-verifier").canonicalFile },
            File("../quality-verifier")
        )
        return candidates.firstOrNull { it.exists() && File(it, "build.gradle.kts").exists() }
    }

    private fun findInstallDir(): File? = try {
        val codeSourceLocation = ProjectUtils::class.java.protectionDomain?.codeSource?.location
        codeSourceLocation?.let { File(it.toURI()).parentFile?.parentFile }
    } catch (_: Exception) {
        null
    }

    /**
     * Sorts contiguous Kotlin `import` blocks to match Ktlint's default ordering
     * (`*`, `java.**`, `javax.**`, `kotlin.**`, aliases) so arbitrary user package
     * names remain compliant with Spotless/Ktlint.
     */
    fun sortKotlinImports(content: String): String {
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

    /**
     * Strips `//` line comments and `/* ... */` block comments from Kotlin/Gradle source
     * while preserving string literals so regex-based static checks ignore commented lines.
     */
    fun stripKotlinComments(source: String): String {
        val out = StringBuilder(source.length)
        var i = 0
        var inString = false
        while (i < source.length) {
            val ch = source[i]
            val next = if (i + 1 < source.length) source[i + 1] else '\u0000'
            if (inString) {
                out.append(ch)
                if (ch == '\\' && i + 1 < source.length) {
                    out.append(next)
                    i += 2
                    continue
                }
                if (ch == '"') inString = false
                i++
            } else if (ch == '"') {
                inString = true
                out.append(ch)
                i++
            } else if (ch == '/' && next == '/') {
                i += 2
                while (i < source.length && source[i] != '\n') i++
            } else if (ch == '/' && next == '*') {
                i += 2
                var depth = 1
                while (i < source.length && depth > 0) {
                    if (source[i] == '/' && i + 1 < source.length && source[i + 1] == '*') {
                        depth++
                        i += 2
                    } else if (source[i] == '*' && i + 1 < source.length && source[i + 1] == '/') {
                        depth--
                        i += 2
                    } else {
                        if (source[i] == '\n') out.append('\n')
                        i++
                    }
                }
            } else {
                out.append(ch)
                i++
            }
        }
        return out.toString()
    }

    /** Cross-platform executable lookup in PATH (works on Linux, macOS, and Windows). */
    fun isCommandAvailable(cmd: String): Boolean {
        val isWindows = System.getProperty("os.name").orEmpty().lowercase().contains("win")
        val pathEnv = System.getenv("PATH").orEmpty()
        val extensions = if (isWindows) {
            System.getenv("PATHEXT").orEmpty()
                .split(';')
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .ifEmpty { listOf(".EXE", ".CMD", ".BAT", ".COM") }
        } else {
            listOf("")
        }

        for (dir in pathEnv.split(File.pathSeparatorChar)) {
            if (dir.isBlank()) continue
            for (ext in extensions) {
                val candidate = File(dir, cmd + ext)
                if (candidate.isFile && (isWindows || candidate.canExecute())) {
                    return true
                }
            }
        }
        return false
    }
}
