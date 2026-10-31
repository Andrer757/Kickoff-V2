package eu.andrer757.kickoff.cli

import com.github.ajalt.clikt.core.CliktCommand
import eu.andrer757.kickoff.mcp.McpServer

class McpCommand : CliktCommand(name = "mcp") {
    override fun run() {
        McpServer().start()
    }
}
