package eu.andrer757.kickoff.generator

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class AiKitSynthesizer {

    fun synthesize(canonicalAgentsDir: File, targetDir: File, selectedAi: Set<String>, engram: Boolean = false) {
        if (selectedAi.contains("none") || !canonicalAgentsDir.exists()) {
            return
        }

        val agentsSrc = File(canonicalAgentsDir, "agents")
        val skillsSrc = File(canonicalAgentsDir, "skills")
        val governanceSrc = File(canonicalAgentsDir.parentFile, ".github/governance")
        val backlogSrc = File(canonicalAgentsDir.parentFile, ".github/backlog")
        val policiesSrc = File(canonicalAgentsDir.parentFile, ".github/policies")

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

            // Claude Code discovers .claude/agents and .claude/skills by convention, so no settings
            // are needed unless Engram is enabled. Then the project-scoped .mcp.json server is pre-approved.
            if (engram) {
                writeIfAbsent(File(targetDir, ".mcp.json"), mcpServersJson())
                writeIfAbsent(
                    File(targetDir, ".claude/settings.json"),
                    """
                    {
                      "enabledMcpjsonServers": ["engram"]
                    }
                    """.trimIndent() + "\n"
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

            if (engram) {
                writeIfAbsent(
                    File(targetDir, ".vscode/mcp.json"),
                    """
                    {
                      "servers": {
                        "engram": {
                          "type": "stdio",
                          "command": "engram",
                          "args": ["mcp"]
                        }
                      }
                    }
                    """.trimIndent() + "\n"
                )
            }
        }

        // Governance registry, backlog, and local policies are the default (file-based) pipeline state.
        if (governanceSrc.exists()) {
            governanceSrc.copyRecursively(File(targetDir, ".github/governance"), overwrite = true)
        }
        if (backlogSrc.exists()) {
            backlogSrc.copyRecursively(File(targetDir, ".github/backlog"), overwrite = true)
        }
        if (policiesSrc.exists()) {
            policiesSrc.copyRecursively(File(targetDir, ".github/policies"), overwrite = true)
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
            if (engram) {
                writeIfAbsent(File(targetDir, ".cursor/mcp.json"), mcpServersJson())
            }
        }
    }

    private fun mcpServersJson(): String =
        """
        {
          "mcpServers": {
            "engram": {
              "command": "engram",
              "args": ["mcp"]
            }
          }
        }
        """.trimIndent() + "\n"

    private fun writeIfAbsent(file: File, content: String) {
        if (file.exists()) return
        file.parentFile?.mkdirs()
        file.writeText(content, Charsets.UTF_8)
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
            // No default `tools:` allowlist: Claude Code then gives the agent every tool the session has,
            // including MCP servers (mcp__engram__*, mcp__kickoff__*) and Agent. An explicit list without
            // them silently blocks the Engram hand-offs and kickoff_arch_check the agent prompts rely on.
            formatToolList(frontmatter["tools"])?.let { appendLine("tools: $it") }
            frontmatter["model"]?.let { appendLine("model: $it") }
            frontmatter["effort"]?.let { appendLine("effort: $it") }
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
        return buildString {
            appendLine("---")
            appendLine("name: $name")
            appendLine("description: ${escapeYamlString(description)}")
            // No `tools:` list: Copilot then exposes all tools, including MCP servers and sub-agents.
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
            appendLine("alwaysApply: false")
            appendLine("---")
            appendLine()
            append(body.trim())
            appendLine()
        }
    }

    /**
     * Minimal YAML frontmatter reader for agent files: `key: value`, quoted values,
     * folded/literal block scalars (`>`, `|`, with optional chomping indicator) and string lists.
     */
    internal fun parseFrontmatter(content: String): Pair<Map<String, String>, String> {
        val normalized = content.replace("\r\n", "\n")
        if (!normalized.startsWith("---")) return emptyMap<String, String>() to content

        val parts = normalized.split(Regex("^---\\s*$", RegexOption.MULTILINE))
        if (parts.size < 3) return emptyMap<String, String>() to content

        val rawFm = parts[1]
        val body = parts.drop(2).joinToString("---")
        val map = mutableMapOf<String, String>()

        val lines = rawFm.lines()
        var index = 0
        while (index < lines.size) {
            val match = topLevelKeyRegex.matchEntire(lines[index])
            index++
            if (match == null) continue

            val key = match.groupValues[1]
            val value = match.groupValues[2].trim()

            // Everything up to the next top-level key belongs to this key:
            // indented lines, blank lines and `- item` entries.
            val nested = mutableListOf<String>()
            while (index < lines.size && !topLevelKeyRegex.matches(lines[index])) {
                nested.add(lines[index])
                index++
            }

            val parsed = when {
                blockScalarRegex.matches(value) -> parseBlockScalar(nested, folded = value.startsWith(">"))
                value.isEmpty() -> nested.map { it.trim() }
                    .filter { it.startsWith("-") }
                    .joinToString(",") { it.removePrefix("-").trim() }
                else -> (listOf(value) + nested.map { it.trim() }.filter { it.isNotEmpty() })
                    .joinToString(" ")
                    .let(::unquote)
            }
            if (parsed.isNotEmpty()) {
                map[key] = parsed
            }
        }

        return map to body
    }

    /** YAML `|` (literal) and `>` (folded) block scalars with indented continuation lines. */
    private fun parseBlockScalar(lines: List<String>, folded: Boolean): String {
        val content = lines.dropLastWhile { it.isBlank() }
        val indent = content.filter { it.isNotBlank() }.minOfOrNull { line -> line.indexOfFirst { !it.isWhitespace() } } ?: 0
        val stripped = content.map { if (it.isBlank()) "" else it.substring(indent).trimEnd() }
        if (!folded) return stripped.joinToString("\n").trim()

        return buildString {
            stripped.forEach { line ->
                when {
                    line.isEmpty() -> append("\n")
                    isEmpty() || endsWith("\n") -> append(line)
                    else -> append(" ").append(line)
                }
            }
        }.trim()
    }

    private fun unquote(value: String): String = when {
        value.length >= 2 && value.startsWith("\"") && value.endsWith("\"") ->
            value.substring(1, value.length - 1).replace("\\\"", "\"")
        value.length >= 2 && value.startsWith("'") && value.endsWith("'") ->
            value.substring(1, value.length - 1).replace("''", "'")
        else -> value
    }

    private fun formatToolList(rawTools: String?): String? {
        if (rawTools.isNullOrBlank()) return null
        val tools = rawTools.removeSurrounding("[", "]").split(",").map { it.trim() }.filter { it.isNotEmpty() }
        return tools.takeIf { it.isNotEmpty() }?.joinToString(", ")
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
        val clean = value.trim()
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
        return "\"$clean\""
    }

    private companion object {
        val topLevelKeyRegex = Regex("^([A-Za-z0-9_-]+):(?:\\s+(.*))?$")
        val blockScalarRegex = Regex("^[|>][+-]?[1-9]?[+-]?$")
    }
}
