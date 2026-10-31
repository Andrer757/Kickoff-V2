package eu.andrer757.kickoff.generator

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class AiKitSynthesizer {

    private val commonToolsList = listOf(
        "execute/runInTerminal", "execute/runTests", "read/readFile",
        "search/codebase", "search/fileSearch", "search/textSearch",
        "edit/createFile", "edit/editFiles"
    )

    fun synthesize(canonicalAgentsDir: File, targetDir: File, selectedAi: Set<String>) {
        if (selectedAi.contains("none") || !canonicalAgentsDir.exists()) {
            return
        }

        val agentsSrc = File(canonicalAgentsDir, "agents")
        val skillsSrc = File(canonicalAgentsDir, "skills")
        val governanceSrc = File(canonicalAgentsDir.parentFile, ".github/governance")

        val isAll = selectedAi.contains("all")
        val enableClaude = isAll || selectedAi.contains("claude")
        val enableCopilot = isAll || selectedAi.contains("copilot")
        val enableCursor = isAll || selectedAi.contains("cursor")
        val enableGeneric = isAll || selectedAi.contains("generic") || selectedAi.contains("antigravity")

        val agentFiles = if (agentsSrc.exists()) agentsSrc.listFiles { f -> f.extension == "md" }?.toList() ?: emptyList() else emptyList()

        // 1. Generic / Antigravity (.agents/)
        if (enableGeneric || enableCursor) {
            val targetAgents = File(targetDir, ".agents/agents")
            val targetSkills = File(targetDir, ".agents/skills")
            targetAgents.mkdirs()
            targetSkills.mkdirs()

            if (enableGeneric) {
                agentFiles.forEach { file ->
                    Files.copy(file.toPath(), File(targetAgents, file.name).toPath(), StandardCopyOption.REPLACE_EXISTING)
                }
            }
            if (skillsSrc.exists()) {
                skillsSrc.copyRecursively(targetSkills, overwrite = true)
            }
        }

        // 2. Claude Code (.claude/)
        if (enableClaude) {
            val claudeAgentsDir = File(targetDir, ".claude/agents")
            val claudeSkillsDir = File(targetDir, ".claude/skills")
            claudeAgentsDir.mkdirs()
            claudeSkillsDir.mkdirs()

            agentFiles.forEach { file ->
                val content = file.readText(Charsets.UTF_8)
                val synthesized = synthesizeClaudeAgent(content)
                File(claudeAgentsDir, file.name).writeText(synthesized, Charsets.UTF_8)
            }

            if (skillsSrc.exists()) {
                skillsSrc.copyRecursively(claudeSkillsDir, overwrite = true)
            }

            val settingsFile = File(targetDir, ".claude/settings.json")
            if (!settingsFile.exists()) {
                settingsFile.writeText(
                    """
                    {
                      "agentsDirectory": ".claude/agents",
                      "skillsDirectory": ".claude/skills"
                    }
                    """.trimIndent(),
                    Charsets.UTF_8
                )
            }
        }

        // 3. GitHub Copilot (.github/)
        if (enableCopilot) {
            val copilotAgentsDir = File(targetDir, ".github/agents")
            val copilotSkillsDir = File(targetDir, ".github/skills")
            copilotAgentsDir.mkdirs()
            copilotSkillsDir.mkdirs()

            agentFiles.forEach { file ->
                val baseName = file.nameWithoutExtension
                val content = file.readText(Charsets.UTF_8)
                val synthesized = synthesizeCopilotAgent(content)
                File(copilotAgentsDir, "$baseName.agent.md").writeText(synthesized, Charsets.UTF_8)
            }

            if (skillsSrc.exists()) {
                skillsSrc.copyRecursively(copilotSkillsDir, overwrite = true)
            }

            if (governanceSrc != null && governanceSrc.exists()) {
                val targetGov = File(targetDir, ".github/governance")
                governanceSrc.copyRecursively(targetGov, overwrite = true)
            }
        }

        // 4. Cursor (.cursor/rules/)
        if (enableCursor) {
            val cursorRulesDir = File(targetDir, ".cursor/rules")
            cursorRulesDir.mkdirs()

            agentFiles.forEach { file ->
                val slug = file.nameWithoutExtension.lowercase().replace(Regex("[^a-z0-9_-]"), "-")
                val content = file.readText(Charsets.UTF_8)
                val synthesized = synthesizeCursorRule(content)
                File(cursorRulesDir, "$slug.mdc").writeText(synthesized, Charsets.UTF_8)
            }
        }
    }

    private fun synthesizeClaudeAgent(content: String): String {
        val (frontmatter, body) = parseFrontmatter(content)
        val name = frontmatter["name"] ?: "Agent"
        val description = frontmatter["description"] ?: ""
        val skillsBlock = formatSkills(frontmatter["skills"])

        return buildString {
            appendLine("---")
            appendLine("name: $name")
            appendLine("description: ${escapeYamlString(description)}")
            appendLine("tools: Read, Bash, Edit, Write, Grep, Glob, WebFetch, TodoWrite, Skill")
            if (skillsBlock.isNotBlank()) {
                appendLine(skillsBlock)
            }
            appendLine("---")
            appendLine()
            append(body.trim())
            appendLine()
        }
    }

    private fun synthesizeCopilotAgent(content: String): String {
        val (frontmatter, body) = parseFrontmatter(content)
        val name = frontmatter["name"] ?: "Agent"
        val description = frontmatter["description"] ?: ""
        val toolsFormatted = commonToolsList.joinToString(", ")

        return buildString {
            appendLine("---")
            appendLine("name: $name")
            appendLine("description: ${escapeYamlString(description)}")
            appendLine("tools: [$toolsFormatted]")
            appendLine("user-invocable: true")
            appendLine("---")
            appendLine()
            append(body.trim())
            appendLine()
        }
    }

    private fun synthesizeCursorRule(content: String): String {
        val (frontmatter, body) = parseFrontmatter(content)
        val description = frontmatter["description"] ?: ""

        return buildString {
            appendLine("---")
            appendLine("description: ${escapeYamlString(description)}")
            appendLine("globs: \"**/*.kt, **/*.kts\"")
            appendLine("alwaysApply: false")
            appendLine("---")
            appendLine()
            append(body.trim())
            appendLine()
        }
    }

    private fun parseFrontmatter(content: String): Pair<Map<String, String>, String> {
        if (!content.startsWith("---")) return emptyMap<String, String>() to content

        val parts = content.split(Regex("^---\\s*$", RegexOption.MULTILINE))
        if (parts.size < 3) return emptyMap<String, String>() to content

        val rawFm = parts[1]
        val body = parts.drop(2).joinToString("---")
        val map = mutableMapOf<String, String>()

        var currentKey: String? = null
        val currentList = mutableListOf<String>()

        for (line in rawFm.lines()) {
            val trimmed = line.trim()
            if (trimmed.startsWith("- ") && currentKey != null) {
                currentList.add(trimmed.substring(2).trim())
            } else if (trimmed.contains(":")) {
                if (currentKey != null && currentList.isNotEmpty()) {
                    map[currentKey] = currentList.joinToString(",")
                    currentList.clear()
                }
                val key = trimmed.substringBefore(":").trim()
                val value = trimmed.substringAfter(":").trim()
                currentKey = key
                if (value.isNotEmpty()) {
                    map[key] = value
                }
            }
        }
        if (currentKey != null && currentList.isNotEmpty()) {
            map[currentKey] = currentList.joinToString(",")
        }

        return map to body
    }

    private fun formatSkills(rawSkills: String?): String {
        if (rawSkills.isNullOrBlank()) return ""
        val skills = rawSkills.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        if (skills.isEmpty()) return ""
        return buildString {
            appendLine("skills:")
            skills.forEach { appendLine("  - $it") }
        }.trimEnd()
    }

    private fun escapeYamlString(value: String): String {
        val clean = value.replace("\"", "\\\"").trim()
        return "\"$clean\""
    }
}
