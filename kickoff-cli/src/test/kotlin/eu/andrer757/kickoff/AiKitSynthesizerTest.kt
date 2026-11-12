package eu.andrer757.kickoff

import eu.andrer757.kickoff.generator.AiKitSynthesizer
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AiKitSynthesizerTest {

    private fun getCanonicalAgentsDir(): File {
        return File("../templates/now-in-android/.agents").let {
            if (it.exists()) it else File("templates/now-in-android/.agents")
        }
    }

    private fun frontmatterOf(file: File): List<String> {
        val lines = file.readLines()
        assertEquals("---", lines.first(), "${file.name} should start with frontmatter")
        val end = lines.drop(1).indexOf("---")
        assertTrue(end >= 0, "${file.name} should close its frontmatter")
        return lines.subList(1, end + 1)
    }

    private fun descriptionOf(file: File): String {
        val line = frontmatterOf(file).single { it.startsWith("description:") }
        return line.removePrefix("description:").trim().removeSurrounding("\"")
    }

    private fun withCanonicalAgent(agentMarkdown: String, block: (File) -> Unit) {
        val canonicalDir = createTempDirectory("kickoff-canonical").toFile()
        val targetDir = createTempDirectory("kickoff-target").toFile()
        try {
            File(canonicalDir, "agents").mkdirs()
            File(canonicalDir, "agents/TestAgent.md").writeText(agentMarkdown.trimIndent())
            AiKitSynthesizer().synthesize(canonicalDir, targetDir, setOf("claude"))
            block(File(targetDir, ".claude/agents/TestAgent.md"))
        } finally {
            canonicalDir.deleteRecursively()
            targetDir.deleteRecursively()
        }
    }

    @Test
    fun `every template agent gets a real description for every ai target`() {
        val targetDir = createTempDirectory("kickoff-aikit-all").toFile()
        try {
            val canonicalDir = getCanonicalAgentsDir()
            val agentNames = File(canonicalDir, "agents").listFiles { f -> f.extension == "md" }!!
                .map { it.nameWithoutExtension }
            assertTrue(agentNames.isNotEmpty())

            AiKitSynthesizer().synthesize(canonicalDir, targetDir, setOf("all"))

            val outputs = agentNames.flatMap { name ->
                listOf(
                    File(targetDir, ".claude/agents/$name.md"),
                    File(targetDir, ".github/agents/$name.agent.md"),
                    File(targetDir, ".cursor/rules/${name.lowercase()}.mdc")
                )
            }
            outputs.forEach { file ->
                assertTrue(file.exists(), "${file.path} should be generated")
                val description = descriptionOf(file)
                assertFalse(description in setOf(">", "|", ">-", "|-", ""), "${file.name} has a placeholder description: $description")
                assertTrue(description.length > 40, "${file.name} description looks truncated: $description")
            }
        } finally {
            targetDir.deleteRecursively()
        }
    }

    @Test
    fun `claude agents have no tools allowlist by default and no settings file`() {
        val targetDir = createTempDirectory("kickoff-aikit-claude").toFile()
        try {
            AiKitSynthesizer().synthesize(getCanonicalAgentsDir(), targetDir, setOf("claude"))

            val agents = File(targetDir, ".claude/agents").listFiles { f -> f.extension == "md" }!!
            assertTrue(agents.isNotEmpty())
            agents.forEach { file ->
                assertFalse(frontmatterOf(file).any { it.startsWith("tools:") }, "${file.name} should not restrict tools")
            }
            assertFalse(File(targetDir, ".claude/settings.json").exists())
        } finally {
            targetDir.deleteRecursively()
        }
    }

    @Test
    fun `claude agent passes through model and effort`() {
        withCanonicalAgent(
            """
            ---
            name: TestAgent
            description: Reviews code.
            model: sonnet
            effort: high
            ---

            Body.
            """
        ) { agent ->
            val frontmatter = frontmatterOf(agent)
            assertTrue("model: sonnet" in frontmatter)
            assertTrue("effort: high" in frontmatter)
            assertFalse(frontmatter.any { it.startsWith("tools:") })
        }
    }

    @Test
    fun `claude agent keeps an explicit canonical tools list`() {
        withCanonicalAgent(
            """
            ---
            name: TestAgent
            description: Reviews code.
            tools:
              - Read
              - Grep
              - mcp__engram__*
            ---

            Body.
            """
        ) { agent ->
            assertTrue("tools: Read, Grep, mcp__engram__*" in frontmatterOf(agent))
        }
    }

    @Test
    fun `folded and literal block scalars are parsed`() {
        withCanonicalAgent(
            """
            ---
            name: TestAgent
            description: >
              First line with a colon: kept,
              folded into one line.

              Second paragraph.
            skills:
              - some-skill
            ---

            Body.
            """
        ) { agent ->
            assertEquals("First line with a colon: kept, folded into one line.\\nSecond paragraph.", descriptionOf(agent))
            val frontmatter = frontmatterOf(agent)
            assertTrue("skills:" in frontmatter)
            assertTrue("  - some-skill" in frontmatter)
        }

        withCanonicalAgent(
            """
            ---
            name: TestAgent
            description: |
              Line one.
              Line "two".
            ---

            Body.
            """
        ) { agent ->
            assertEquals("Line one.\\nLine \\\"two\\\".", descriptionOf(agent))
        }
    }

    @Test
    fun `synthesizes abstract tiers for claude and codex merge-safe AGENTS md`() {
        val root = createTempDirectory("kickoff-codex-test").toFile()
        try {
            val canonical = File(root, "canonical/.agents").apply { mkdirs() }
            File(canonical, "agents").mkdirs()
            File(canonical, "skills/sample-skill").mkdirs()
            File(canonical, "skills/sample-skill/SKILL.md").writeText("---\nname: sample-skill\n---\n")
            File(canonical, "agents/LeadAgent.md").writeText("---\nname: LeadAgent\ntier: lead\ndescription: Lead\n---\n\nLead body.\n")

            val target = File(root, "target").apply { mkdirs() }
            val agentsMd = File(target, "AGENTS.md")
            agentsMd.writeText("# User Notes\n\nKeep me.\n")

            AiKitSynthesizer().synthesize(canonical, target, setOf("claude", "copilot", "codex"))

            val claudeLead = File(target, ".claude/agents/LeadAgent.md").readText()
            assertTrue(claudeLead.contains("model: opus"))
            assertTrue(claudeLead.contains("effort: high"))

            val copilotLead = File(target, ".github/agents/LeadAgent.agent.md").readText()
            assertTrue(copilotLead.contains("> Role Tier: lead"))

            val codexAgents = agentsMd.readText()
            assertTrue(codexAgents.contains("# User Notes"))
            assertTrue(codexAgents.contains("<!-- KICKOFF:BEGIN -->"))
            assertTrue(codexAgents.contains("| `LeadAgent` | `lead` | Lead |"))
            assertTrue(File(target, ".agents/skills/sample-skill/SKILL.md").exists())
        } finally {
            root.deleteRecursively()
        }
    }
}
