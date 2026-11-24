package eu.andrer757.kickoff.mcp

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.core.context
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
import eu.andrer757.kickoff.cli.AddEntityCommand
import eu.andrer757.kickoff.cli.AddFeatureCommand
import eu.andrer757.kickoff.cli.AddModuleCommand
import eu.andrer757.kickoff.cli.ArchChecker
import eu.andrer757.kickoff.cli.DoctorService
import eu.andrer757.kickoff.cli.ProjectUtils
import eu.andrer757.kickoff.generator.ProjectGenerator
import eu.andrer757.kickoff.model.ProjectConfig
import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.io.PrintStream

class McpServer {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        // A response must carry either "result" or "error", never both. Strict MCP clients
        // (e.g. Claude Code) reject messages that contain `"error": null` and drop the connection.
        explicitNulls = false
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
                    "minSdk" to mapOf("type" to "integer", "description" to "Minimum SDK version (default: ${ProjectConfig.DEFAULT_MIN_SDK})"),
                    "targetSdk" to mapOf("type" to "integer", "description" to "Target SDK version (default: ${ProjectConfig.DEFAULT_TARGET_SDK})"),
                    "ai" to mapOf("type" to "string", "description" to "Target AI: copilot, claude, cursor, codex, generic, antigravity, all, none (default: all)"),
                    "gitInit" to mapOf("type" to "boolean", "description" to "Whether to initialize a git repo (default: true)"),
                    "engram" to mapOf("type" to "boolean", "description" to "Opt in to Engram MCP memory: generates MCP client configs for the selected AI tools (default: false)")
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
                    "projectDir" to mapOf("type" to "string", "description" to "Kickoff project root directory"),
                    "force" to mapOf("type" to "boolean", "description" to "Overwrite existing entity files if present (default: false)"),
                    "noMigration" to mapOf("type" to "boolean", "description" to "Skip incrementing Room @Database(version) and generating a Migration stub (default: false)")
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
            description = "Diagnoses developer environment, JDK, Android SDK/API ${ProjectConfig.TEMPLATE_COMPILE_SDK}, Gradle wrapper, and AI tools.",
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
                                "version" to JsonPrimitive("1.1.0")
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
                val toolName = (request.params?.get("name") as? JsonPrimitive)?.takeIf { it.isString }?.content
                val arguments = when (val raw = request.params?.get("arguments")) {
                    null -> JsonObject(emptyMap())
                    is JsonObject -> raw
                    else -> null
                }
                if (toolName == null || arguments == null) {
                    json.encodeToString(
                        JsonRpcResponse(
                            id = request.id,
                            error = JsonRpcError(-32602, "Invalid params: 'name' must be a string and 'arguments' an object")
                        )
                    )
                } else {
                    val callResult = executeTool(toolName, arguments)
                    json.encodeToString(JsonRpcResponse(id = request.id, result = json.encodeToJsonElement(callResult)))
                }
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

    private fun stringArg(args: JsonObject, key: String): String? {
        val el = args[key] ?: return null
        val prim = el as? JsonPrimitive
            ?: throw IllegalArgumentException("Argument '$key' must be a string.")
        if (!prim.isString) throw IllegalArgumentException("Argument '$key' must be a string.")
        return prim.content
    }

    private fun requiredStringArg(args: JsonObject, key: String): String {
        val value = stringArg(args, key)
        if (value.isNullOrBlank()) throw IllegalArgumentException("Missing required string argument '$key'.")
        return value
    }

    private fun intArg(args: JsonObject, key: String, default: Int): Int {
        val el = args[key] ?: return default
        val prim = el as? JsonPrimitive
            ?: throw IllegalArgumentException("Argument '$key' must be an integer.")
        if (prim.isString) throw IllegalArgumentException("Argument '$key' must be an integer.")
        return prim.intOrNull ?: throw IllegalArgumentException("Argument '$key' must be an integer.")
    }

    private fun booleanArg(args: JsonObject, key: String, default: Boolean): Boolean {
        val el = args[key] ?: return default
        val prim = el as? JsonPrimitive
            ?: throw IllegalArgumentException("Argument '$key' must be a boolean.")
        if (prim.isString) throw IllegalArgumentException("Argument '$key' must be a boolean.")
        return prim.booleanOrNull ?: throw IllegalArgumentException("Argument '$key' must be a boolean.")
    }

    private fun executeTool(name: String, args: JsonObject): ToolCallResult {
        return try {
            when (name) {
                "kickoff_scaffold_project" -> {
                    val projName = requiredStringArg(args, "name")
                    val pkgName = requiredStringArg(args, "packageName")
                    val outDir = stringArg(args, "outputDir")?.takeIf { it.isNotBlank() }
                        ?: projName.lowercase().replace(Regex("[^a-z0-9_-]"), "-")
                    val projType = (stringArg(args, "projectType") ?: "app").trim().lowercase()
                    val minSdk = intArg(args, "minSdk", ProjectConfig.DEFAULT_MIN_SDK)
                    val targetSdk = intArg(args, "targetSdk", ProjectConfig.DEFAULT_TARGET_SDK)
                    val aiRaw = stringArg(args, "ai") ?: "all"
                    val aiSet = aiRaw.lowercase().split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
                    val gitInit = booleanArg(args, "gitInit", true)
                    val engram = booleanArg(args, "engram", false)

                    val templatePath = if (projType == "sdk") "templates/android-sdk" else "templates/now-in-android"
                    val templateDir = ProjectUtils.resolveTemplateDir(templatePath)

                    val config = ProjectConfig(
                        projectName = projName,
                        packageName = pkgName,
                        outputDir = outDir,
                        templateDir = templateDir,
                        minSdk = minSdk,
                        targetSdk = targetSdk,
                        aiEnvironments = if (aiSet.isEmpty()) setOf("all") else aiSet,
                        projectType = projType,
                        gitInit = gitInit,
                        engram = engram
                    )
                    val result = ProjectGenerator().generate(config)
                    ToolCallResult(listOf(ToolContent(text = json.encodeToString(result))), isError = result.status != "success")
                }
                "kickoff_add_feature" -> {
                    val featName = requiredStringArg(args, "name")
                    val pDir = stringArg(args, "projectDir") ?: ""
                    val root = if (pDir.isNotBlank()) File(pDir) else ProjectUtils.findProjectRoot()
                        ?: return ToolCallResult(listOf(ToolContent(text = "Project root not found")), isError = true)

                    val cmdArgs = mutableListOf("-n", featName, "-d", root.absolutePath, "--json")
                    runCommand(AddFeatureCommand(), cmdArgs)
                }
                "kickoff_add_module" -> {
                    val modName = requiredStringArg(args, "name")
                    val modType = (stringArg(args, "type") ?: "core").trim().lowercase()
                    val pDir = stringArg(args, "projectDir") ?: ""
                    val root = if (pDir.isNotBlank()) File(pDir) else ProjectUtils.findProjectRoot()
                        ?: return ToolCallResult(listOf(ToolContent(text = "Project root not found")), isError = true)

                    val cmdArgs = mutableListOf("-n", modName, "-t", modType, "-d", root.absolutePath, "--json")
                    runCommand(AddModuleCommand(), cmdArgs)
                }
                "kickoff_add_entity" -> {
                    val entName = requiredStringArg(args, "name")
                    val entFields = stringArg(args, "fields") ?: "id:String,title:String"
                    val pDir = stringArg(args, "projectDir") ?: ""
                    val force = booleanArg(args, "force", false)
                    val noMigration = booleanArg(args, "noMigration", false)
                    val root = if (pDir.isNotBlank()) File(pDir) else ProjectUtils.findProjectRoot()
                        ?: return ToolCallResult(listOf(ToolContent(text = "Project root not found")), isError = true)

                    val cmdArgs = mutableListOf("-n", entName, "-f", entFields, "-d", root.absolutePath, "--json")
                    if (force) cmdArgs.add("--force")
                    if (noMigration) cmdArgs.add("--no-migration")
                    runCommand(AddEntityCommand(), cmdArgs)
                }
                "kickoff_arch_check" -> {
                    val pDir = stringArg(args, "projectDir") ?: ""
                    val root = if (pDir.isNotBlank()) File(pDir) else ProjectUtils.findProjectRoot()
                        ?: return ToolCallResult(listOf(ToolContent(text = "Project root not found")), isError = true)
                    val result = ArchChecker.check(root)
                    ToolCallResult(listOf(ToolContent(text = json.encodeToString(result))), isError = result.status != "success")
                }
                "kickoff_doctor" -> {
                    val pDir = stringArg(args, "projectDir") ?: ""
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

    /**
     * Runs a Clikt subcommand in-process. Its output is captured instead of going to stdout,
     * which is reserved for JSON-RPC frames, and a failing command returns an error result
     * instead of terminating the server.
     */
    private fun runCommand(command: CliktCommand, args: List<String>): ToolCallResult {
        val captured = StringBuilder()
        command.context {
            echoMessage = { _, message, trailingNewline, _ ->
                captured.append(message)
                if (trailingNewline) captured.append('\n')
            }
        }
        return try {
            command.parse(args)
            ToolCallResult(listOf(ToolContent(text = captured.toString().trim())))
        } catch (e: ProgramResult) {
            ToolCallResult(listOf(ToolContent(text = captured.toString().trim())), isError = e.statusCode != 0)
        } catch (e: CliktError) {
            ToolCallResult(listOf(ToolContent(text = e.message ?: captured.toString().trim())), isError = true)
        }
    }

    fun start(input: InputStream = System.`in`, output: PrintStream = System.out) {
        val reader = BufferedReader(InputStreamReader(input, Charsets.UTF_8))
        while (true) {
            val line = reader.readLine() ?: break
            // One bad request must not end the session: answer it with an error and keep reading.
            val response = try {
                handleMessage(line)
            } catch (e: Exception) {
                System.err.println("kickoff mcp: failed to handle request: ${e.message}")
                internalErrorResponse(line, e)
            }
            if (response != null) {
                output.println(response)
                output.flush()
            }
        }
    }

    private fun internalErrorResponse(rawMessage: String, error: Exception): String? {
        val requestObject = runCatching { json.parseToJsonElement(rawMessage) as? JsonObject }.getOrNull()
        val id = requestObject?.get("id") ?: return null
        return json.encodeToString(
            JsonRpcResponse(id = id, error = JsonRpcError(-32603, "Internal error: ${error.message}"))
        )
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
