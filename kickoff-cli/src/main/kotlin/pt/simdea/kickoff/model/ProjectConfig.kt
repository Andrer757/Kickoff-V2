package pt.simdea.kickoff.model

import kotlinx.serialization.Serializable

@Serializable
data class ProjectConfig(
    val projectName: String,
    val packageName: String,
    val outputDir: String,
    val templateDir: String = "templates/now-in-android",
    val minSdk: Int = 26,
    val targetSdk: Int = 35,
    val aiEnvironments: Set<String> = setOf("all")
)

@Serializable
data class GenerationResult(
    val status: String,
    val message: String? = null,
    val projectPath: String? = null,
    val projectName: String? = null,
    val packageName: String? = null,
    val modules: List<String> = emptyList(),
    val aiEnvironments: List<String> = emptyList(),
    val entryPoints: Map<String, String> = emptyMap()
)
