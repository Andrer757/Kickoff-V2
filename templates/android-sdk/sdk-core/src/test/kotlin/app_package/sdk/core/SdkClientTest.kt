package ${configs.packageName}.sdk.core

import ${configs.packageName}.sdk.testing.FakeSdkClient
import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SdkClientTest {

    @Test
    fun `sdk client initializes successfully`() = runTest {
        val client = FakeSdkClient()

        client.status.test {
            assertEquals(SdkStatus.READY, awaitItem())
        }
    }

    @Test
    fun `sdk client handles failure`() = runTest {
        val client = FakeSdkClient()
        client.shouldFailInitialize = true

        val result = client.initialize()
        assertTrue(result.isFailure)
        assertEquals(SdkStatus.ERROR, client.status.value)
    }

    @Test
    fun `sdk config builder constructs valid configuration`() {
        val config = SdkConfig.Builder("api-key-123")
            .setEnvironment("staging")
            .setDebugLoggingEnabled(true)
            .build()

        assertEquals("api-key-123", config.apiKey)
        assertEquals("staging", config.environment)
        assertTrue(config.debugLoggingEnabled)
    }
}
