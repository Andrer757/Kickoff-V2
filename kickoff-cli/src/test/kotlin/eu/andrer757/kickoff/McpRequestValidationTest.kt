package eu.andrer757.kickoff

import eu.andrer757.kickoff.mcp.McpServer
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class McpRequestValidationTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun parse(rawResponse: String?): JsonObject {
        assertNotNull(rawResponse)
        return json.parseToJsonElement(rawResponse).jsonObject
    }

    private fun errorCode(response: JsonObject): Int? =
        response["error"]?.jsonObject?.get("code")?.jsonPrimitive?.int

    @Test
    fun `tools call with array arguments returns invalid params`() {
        val request = """{"jsonrpc":"2.0","id":5,"method":"tools/call","params":{"name":"kickoff_doctor","arguments":[]}}"""
        val response = parse(McpServer().handleMessage(request))

        assertEquals("5", response["id"]?.jsonPrimitive?.content)
        assertEquals(-32602, errorCode(response))
    }

    @Test
    fun `tools call with non string name returns invalid params`() {
        val request = """{"jsonrpc":"2.0","id":6,"method":"tools/call","params":{"name":{},"arguments":{}}}"""
        val response = parse(McpServer().handleMessage(request))

        assertEquals(-32602, errorCode(response))
    }

    @Test
    fun `server keeps serving requests after an invalid one`() {
        val input = listOf(
            """{"jsonrpc":"2.0","id":1,"method":"tools/call","params":{"name":"kickoff_doctor","arguments":[]}}""",
            """{"jsonrpc":"2.0","id":2,"method":"tools/list","params":{}}"""
        ).joinToString("\n", postfix = "\n")
        val output = ByteArrayOutputStream()

        McpServer().start(ByteArrayInputStream(input.toByteArray()), PrintStream(output, true, "UTF-8"))

        val responses = output.toString("UTF-8").lines().filter { it.isNotBlank() }.map(::parse)
        assertEquals(2, responses.size, "Both requests should get a response")
        assertEquals(-32602, errorCode(responses[0]))
        assertEquals("2", responses[1]["id"]?.jsonPrimitive?.content)
        assertTrue(responses[1]["result"].toString().contains("kickoff_scaffold_project"))
    }

    @Test
    fun `scaffold tool rejects missing required args and non-string primitives`() {
        val server = McpServer()
        val missingName = """{"jsonrpc":"2.0","id":7,"method":"tools/call","params":{"name":"kickoff_scaffold_project","arguments":{"packageName":"com.example.app"}}}"""
        val res1 = parse(server.handleMessage(missingName))
        assertEquals("true", res1["result"]?.jsonObject?.get("isError")?.jsonPrimitive?.content)

        val nonStringName = """{"jsonrpc":"2.0","id":8,"method":"tools/call","params":{"name":"kickoff_scaffold_project","arguments":{"name":123,"packageName":"com.example.app"}}}"""
        val res2 = parse(server.handleMessage(nonStringName))
        assertEquals("true", res2["result"]?.jsonObject?.get("isError")?.jsonPrimitive?.content)
    }

    @Test
    fun `tools list schema reflects targetSdk 37, codex, and entity migration flags`() {
        val raw = McpServer().handleMessage("""{"jsonrpc":"2.0","id":9,"method":"tools/list","params":{}}""")
        assertNotNull(raw)
        assertTrue(raw.contains("default: 37"))
        assertTrue(raw.contains("codex"))
        assertTrue(raw.contains("noMigration"))
        assertTrue(raw.contains("\"force\""))
        assertTrue(raw.contains("API 37"))
    }
}
