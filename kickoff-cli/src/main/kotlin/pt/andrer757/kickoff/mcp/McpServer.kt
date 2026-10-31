package pt.andrer757.kickoff.mcp

import com.github.ajalt.clikt.core.parse
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import pt.andrer757.kickoff.cli.AddEntityCommand
import pt.andrer757.kickoff.cli.AddFeatureCommand
import pt.andrer757.kickoff.cli.AddModuleCommand
import pt.andrer757.kickoff.cli.ArchChecker
import pt.andrer757.kickoff.cli.DoctorService
import pt.andrer757.kickoff.cli.ProjectUtils
import pt.andrer757.kickoff.generator.ProjectGenerator
import pt.andrer757.kickoff.model.ProjectConfig
import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.io.PrintStream

class McpServer {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }

    private val tools: List<ToolDefinition> = listOf(
        ToolDefinition(
            name = "kickoff_scaffold_project",
            description = "Scaffolds a new production-ready Android App (Now-in-Android) or Android SDK + Sample project.",
            inputSchema = buildSchema(
                properties = mapOf(
                    "name" to mapOf("type" to "string", "description" to "Project name (e.g. 'Fitness Tracker')"),
                    "packageName" to mapOf("type" to "string", "description" to "Application package name (e.g. 'com.example.fitness')"),
                    "outputDir" to mapOf("type" to "string", "description" to "Target output directory (defaults to sanitized project name)"),
                    "projectType" to mapOf("type" to "string", "enum" to listOf("app", "sdk"), "description" to "Project archetype: 'app' or 'sdk' (default: app)"),
                    "minSdk" to mapOf("type" to "integer", "description" to "Minimum SDK version (default: 26)"),
                    "targetSdk" to mapOf("type" to "integer", "description" to "Target SDK version (default: 35)"),
                    "ai" to mapOf("type" to "string", "description" to "Target AI: copilot, claude, cursor, generic, all, none (default: all)"),
                    "gitInit" to mapOf("type" to "boolean", "description" to "Whether to initialize a git repo (default: true)")
                ),
                required = listOf("name", "packageName")
            )
        ),
        ToolDefinition(
            name = "kickoff_add_feature",
            description = "Scaffolds a new Clean Architecture feature module (:feature:<name>) and auto-wires settings.gradle.kts, app/build.gradle.kts, and MainActivity NavHost.",
            inputSchema = buildSchema(
                properties = mapOf(
                    "name" to mapOf("type" to "string", "description" to "Feature name (e.g. 'settings', 'profile')"),
                    "projectDir" to mapOf("type" to "string", "description" to "Kickoff project root directory (defaults to current dir)")
                ),
                required = listOf("name")
            )
        ),
        ToolDefinition(
            name = "kickoff_add_module",
            description = "Scaffolds a new capability module (:core:<name> for App or :sdk-<name> for SDK) with convention plugins and starter code.",
            inputSchema = buildSchema(
                properties = mapOf(
                    "name" to mapOf("type" to "string", "description" to "Module name (e.g. 'auth', 'analytics')"),
                    "type" to mapOf("type" to "string", "enum" to listOf("core", "library", "compose", "sdk"), "description" to "Module type (default: core)"),
                    "projectDir" to mapOf("type" to "string", "description" to "Kickoff project root directory")
                ),
                required = listOf("name")
            )
        ),
        ToolDefinition(
            name = "kickoff_add_entity",
            description = "Scaffolds a Room entity, DAO, domain model, and repository across :core:model, :core:database, and :core:data.",
            inputSchema = buildSchema(
                properties = mapOf(
                    "name" to mapOf("type" to "string", "description" to "Entity name (e.g. 'Product', 'Article')"),
                    "fields" to mapOf("type" to "string", "description" to "Comma-separated name:Type list (default: 'id:String,title:String')"),
                    "projectDir" to mapOf("type" to "string", "description" to "Kickoff project root directory")
                ),
                required = listOf("name")
            )
        ),
        ToolDefinition(
            name = "kickoff_arch_check",
            description = "Runs fast static Clean Architecture verification on module dependency rules without starting a Gradle daemon.",
            inputSchema = buildSchema(
                properties = mapOf(
                    "projectDir" to mapOf("type" to "string", "description" to "Kickoff project root directory")
                ),
                required = emptyList()
            )
        ),
        ToolDefinition(
            name = "kickoff_doctor",
            description = "Diagnoses developer environment, JDK, Android SDK/API 35, Gradle wrapper, and AI tools.",
            inputSchema = buildSchema(
                properties = mapOf(
                    "projectDir" to mapOf("type" to "string", "description" to "Kickoff project root directory")
                ),
                required = emptyList()
            )
        )
    )

    fun handleMessage(rawMessage: String): String? {
        val trimmed = rawMessage.trim()
        if (trimmed.isEmpty()) return null

        val request = try {
            json.decodeFromString<JsonRpcRequest>(trimmed)
        } catch (_: Exception) {
            return json.encodeToString(
                JsonRpcResponse(
                    error = JsonRpcError(-32700, "Parse error: Invalid JSON")
                )
            )
        }

        // Notification (no id)
        if (request.id == null) {
            return null
        }

        return when (request.method) {
            "initialize" -> {
                val initResult = JsonObject(
                    mapOf(
                        "protocolVersion" to JsonPrimitive("2024-11-05"),
                        "capabilities" to JsonObject(mapOf("tools" to JsonObject(emptyMap()))),
                        "serverInfo" to JsonObject(
                            mapOf(
                                "name" to JsonPrimitive("kickoff-mcp"),
                                "version" to JsonPrimitive("1.0.0")
                            )
                        )
                    )
                )
                json.encodeToString(JsonRpcResponse(id = request.id, result = initResult))
            }
            "tools/list" -> {
                val listResult = JsonObject(
                    mapOf("tools" to json.encodeToJsonElement(tools))
                )
                json.encodeToString(JsonRpcResponse(id = request.id, result = listResult))
            }
            "tools/call" -> {
                val toolName = request.params?.get("name")?.jsonPrimitive?.content ?: ""
                val arguments = request.params?.get("arguments")?.jsonObject ?: JsonObject(emptyMap())
                val callResult = executeTool(toolName, arguments)
                json.encodeToString(JsonRpcResponse(id = request.id, result = json.encodeToJsonElement(callResult)))
            }
            else -> {
                json.encodeToString(
                    JsonRpcResponse(
                        id = request.id,
                        error = JsonRpcError(-32601, "Method not found: ${request.method}")
                    )
                )
            }
        }
    }

    private fun executeTool(name: String, args: JsonObject): ToolCallResult {
        return try {
            when (name) {
                "kickoff_scaffold_project" -> {
                    val projName = args["name"]?.jsonPrimitive?.content ?: "App"
                    val pkgName = args["packageName"]?.jsonPrimitive?.content ?: "com.example.app"
                    val outDir = args["outputDir"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }
                        ?: projName.lowercase().replace(Regex("[^a-z0-9_-]"), "-")
                    val projType = args["projectType"]?.jsonPrimitive?.content ?: "app"
                    val minSdk = args["minSdk"]?.jsonPrimitive?.intOrNull ?: 26
                    val targetSdk = args["targetSdk"]?.jsonPrimitive?.intOrNull ?: 35
                    val ai = args["ai"]?.jsonPrimitive?.content ?: "all"
                    val gitInit = args["gitInit"]?.jsonPrimitive?.booleanOrNull ?: true

                    val templatePath = if (projType == "sdk") "templates/android-sdk" else "templates/now-in-android"
                    val templateDir = resolveTemplateDir(templatePath)

                    val config = ProjectConfig(
                        projectName = projName,
                        packageName = pkgName,
                        outputDir = outDir,
                        templateDir = templateDir,
                        minSdk = minSdk,
                        targetSdk = targetSdk,
                        aiEnvironments = ai.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet(),
                        projectType = projType,
                        gitInit = gitInit
                    )
                    val result = ProjectGenerator().generate(config)
                    ToolCallResult(listOf(ToolContent(text = json.encodeToString(result))), isError = result.status != "success")
                }
                "kickoff_add_feature" -> {
                    val featName = args["name"]?.jsonPrimitive?.content ?: ""
                    val pDir = args["projectDir"]?.jsonPrimitive?.content ?: ""
                    val root = if (pDir.isNotBlank()) File(pDir) else ProjectUtils.findProjectRoot()
                        ?: return ToolCallResult(listOf(ToolContent(text = "Project root not found")), isError = true)

                    val cmdArgs = mutableListOf("-n", featName, "-d", root.absolutePath, "--json")
                    val cmd = AddFeatureCommand()
                    cmd.parse(cmdArgs)
                    ToolCallResult(listOf(ToolContent(text = "Feature '$featName' created successfully!")))
                }
                "kickoff_add_module" -> {
                    val modName = args["name"]?.jsonPrimitive?.content ?: ""
                    val modType = args["type"]?.jsonPrimitive?.content ?: "core"
                    val pDir = args["projectDir"]?.jsonPrimitive?.content ?: ""
                    val root = if (pDir.isNotBlank()) File(pDir) else ProjectUtils.findProjectRoot()
                        ?: return ToolCallResult(listOf(ToolContent(text = "Project root not found")), isError = true)

                    val cmdArgs = mutableListOf("-n", modName, "-t", modType, "-d", root.absolutePath, "--json")
                    val cmd = AddModuleCommand()
                    cmd.parse(cmdArgs)
                    ToolCallResult(listOf(ToolContent(text = "Module '$modName' created successfully!")))
                }
                "kickoff_add_entity" -> {
                    val entName = args["name"]?.jsonPrimitive?.content ?: ""
                    val entFields = args["fields"]?.jsonPrimitive?.content ?: "id:String,title:String"
                    val pDir = args["projectDir"]?.jsonPrimitive?.content ?: ""
                    val root = if (pDir.isNotBlank()) File(pDir) else ProjectUtils.findProjectRoot()
                        ?: return ToolCallResult(listOf(ToolContent(text = "Project root not found")), isError = true)

                    val cmdArgs = mutableListOf("-n", entName, "-f", entFields, "-d", root.absolutePath, "--json")
                    val cmd = AddEntityCommand()
                    cmd.parse(cmdArgs)
                    ToolCallResult(listOf(ToolContent(text = "Entity '$entName' created successfully!")))
                }
                "kickoff_arch_check" -> {
                    val pDir = args["projectDir"]?.jsonPrimitive?.content ?: ""
                    val root = if (pDir.isNotBlank()) File(pDir) else ProjectUtils.findProjectRoot()
                        ?: return ToolCallResult(listOf(ToolContent(text = "Project root not found")), isError = true)
                    val result = ArchChecker.check(root)
                    ToolCallResult(listOf(ToolContent(text = json.encodeToString(result))), isError = result.status != "success")
                }
                "kickoff_doctor" -> {
                    val pDir = args["projectDir"]?.jsonPrimitive?.content ?: ""
                    val root = if (pDir.isNotBlank()) File(pDir) else ProjectUtils.findProjectRoot()
                    val result = DoctorService.diagnose(root)
                    ToolCallResult(listOf(ToolContent(text = json.encodeToString(result))))
                }
                else -> ToolCallResult(listOf(ToolContent(text = "Unknown tool: $name")), isError = true)
            }
        } catch (e: Exception) {
            ToolCallResult(listOf(ToolContent(text = "Tool execution failed: ${e.message}")), isError = true)
        }
    }

    private fun resolveTemplateDir(path: String): String {
        val direct = File(path)
        if (direct.exists()) return direct.absolutePath

        val working = File(System.getProperty("user.dir"), path)
        if (working.exists()) return working.absolutePath

        val envTemplate = System.getenv("KICKOFF_TEMPLATE_DIR")
        if (!envTemplate.isNullOrBlank() && File(envTemplate).exists()) {
            return File(envTemplate).absolutePath
        }

        val appHome = System.getProperty("app.home") ?: System.getenv("APP_HOME")
        if (!appHome.isNullOrBlank()) {
            val appHomeTemplate = File(appHome, path)
            if (appHomeTemplate.exists()) return appHomeTemplate.absolutePath
        }

        try {
            val codeSourceLocation = McpServer::class.java.protectionDomain?.codeSource?.location
            if (codeSourceLocation != null) {
                val jarOrClassFile = File(codeSourceLocation.toURI())
                val installDir = jarOrClassFile.parentFile?.parentFile
                if (installDir != null) {
                    val bundledTemplate = File(installDir, path)
                    if (bundledTemplate.exists()) return bundledTemplate.absolutePath

                    val repoTemplate = File(installDir, "../../$path").canonicalFile
                    if (repoTemplate.exists()) return repoTemplate.absolutePath
                }
            }
        } catch (_: Exception) {}

        val repoTemplate = File("../$path")
        if (repoTemplate.exists()) return repoTemplate.absolutePath

        return path
    }

    fun start(input: InputStream = System.`in`, output: PrintStream = System.out) {
        val reader = BufferedReader(InputStreamReader(input, Charsets.UTF_8))
        while (true) {
            val line = reader.readLine() ?: break
            val response = handleMessage(line)
            if (response != null) {
                output.println(response)
                output.flush()
            }
        }
    }

    companion object {
        fun buildSchema(properties: Map<String, Any>, required: List<String>): JsonObject {
            val propsMap = properties.mapValues { (_, value) ->
                @Suppress("UNCHECKED_CAST")
                val map = value as Map<String, Any>
                JsonObject(map.mapValues { (_, v) ->
                    when (v) {
                        is String -> JsonPrimitive(v)
                        is List<*> -> JsonArray(v.map { JsonPrimitive(it.toString()) })
                        else -> JsonPrimitive(v.toString())
                    }
                })
            }
            return JsonObject(
                mapOf(
                    "type" to JsonPrimitive("object"),
                    "properties" to JsonObject(propsMap),
                    "required" to JsonArray(required.map { JsonPrimitive(it) })
                )
            )
        }
    }
}
