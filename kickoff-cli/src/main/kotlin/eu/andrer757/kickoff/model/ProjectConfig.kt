package eu.andrer757.kickoff.model

import kotlinx.serialization.Serializable

@Serializable
data class ProjectConfig(
    val projectName: String,
    val packageName: String,
    val outputDir: String,
    val templateDir: String = "templates/now-in-android",
    val minSdk: Int = DEFAULT_MIN_SDK,
    val targetSdk: Int = DEFAULT_TARGET_SDK,
    val aiEnvironments: Set<String> = setOf("all"),
    val projectType: String = "app",
    val gitInit: Boolean = true,
    /** Opt-in Engram MCP memory. When false, agents keep pipeline state in git-tracked files. */
    val engram: Boolean = false
) {
    companion object {
        const val DEFAULT_MIN_SDK = 26
        const val TEMPLATE_COMPILE_SDK = 37
        const val DEFAULT_TARGET_SDK = TEMPLATE_COMPILE_SDK
    }
}

@Serializable
data class GenerationResult(
    val status: String,
    val message: String? = null,
    val projectPath: String? = null,
    val projectName: String? = null,
    val packageName: String? = null,
    val projectType: String? = "app",
    val modules: List<String> = emptyList(),
    val aiEnvironments: List<String> = emptyList(),
    val entryPoints: Map<String, String> = emptyMap()
)
