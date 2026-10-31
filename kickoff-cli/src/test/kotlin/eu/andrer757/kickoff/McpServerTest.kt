package eu.andrer757.kickoff

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import eu.andrer757.kickoff.mcp.McpServer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class McpServerTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `initialize request returns valid protocol capabilities`() {
        val server = McpServer()
        val request = """{"jsonrpc":"2.0","id":1,"method":"initialize","params":{}}"""
        val rawResponse = server.handleMessage(request)

        assertNotNull(rawResponse)
        val response = json.parseToJsonElement(rawResponse).jsonObject
        assertEquals("2.0", response["jsonrpc"]?.jsonPrimitive?.content)
        assertEquals("1", response["id"]?.jsonPrimitive?.content)

        val result = response["result"]?.jsonObject
        assertNotNull(result)
        assertEquals("2024-11-05", result["protocolVersion"]?.jsonPrimitive?.content)
        assertEquals("kickoff-mcp", result["serverInfo"]?.jsonObject?.get("name")?.jsonPrimitive?.content)
    }

    @Test
    fun `tools list returns 6 registered tools`() {
        val server = McpServer()
        val request = """{"jsonrpc":"2.0","id":2,"method":"tools/list","params":{}}"""
        val rawResponse = server.handleMessage(request)

        assertNotNull(rawResponse)
        assertTrue(rawResponse.contains("kickoff_scaffold_project"))
        assertTrue(rawResponse.contains("kickoff_add_feature"))
        assertTrue(rawResponse.contains("kickoff_add_module"))
        assertTrue(rawResponse.contains("kickoff_add_entity"))
        assertTrue(rawResponse.contains("kickoff_arch_check"))
        assertTrue(rawResponse.contains("kickoff_doctor"))
    }

    @Test
    fun `tools call doctor returns diagnostic report`() {
        val server = McpServer()
        val request = """{"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"kickoff_doctor","arguments":{}}}"""
        val rawResponse = server.handleMessage(request)

        assertNotNull(rawResponse)
        assertTrue(rawResponse.contains("Java JDK"))
        assertTrue(rawResponse.contains("Toolchain"))
    }

    @Test
    fun `unknown method returns 32601 error`() {
        val server = McpServer()
        val request = """{"jsonrpc":"2.0","id":4,"method":"non_existent_method","params":{}}"""
        val rawResponse = server.handleMessage(request)

        assertNotNull(rawResponse)
        assertTrue(rawResponse.contains("-32601"))
    }

    @Test
    fun `invalid json returns 32700 parse error`() {
        val server = McpServer()
        val rawResponse = server.handleMessage("not-json")

        assertNotNull(rawResponse)
        assertTrue(rawResponse.contains("-32700"))
    }
}
