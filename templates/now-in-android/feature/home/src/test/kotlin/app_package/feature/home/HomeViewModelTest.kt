package ${configs.packageName}.feature.home

import ${configs.packageName}.core.model.User
import ${configs.packageName}.core.testing.FakeNetworkMonitor
import ${configs.packageName}.core.testing.FakeUserRepository
import ${configs.packageName}.core.testing.MainDispatcherRule
import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class HomeViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcherRule = MainDispatcherRule()

    private val fakeUserRepository = FakeUserRepository()
    private val fakeNetworkMonitor = FakeNetworkMonitor()

    @Test
    fun `uiState emits Success when repository emits data`() = runTest {
        val viewModel = HomeViewModel(
            userRepository = fakeUserRepository,
            networkMonitor = fakeNetworkMonitor
        )

        fakeUserRepository.setUsers(listOf(User("1", "Alice", "alice@example.com")))
        fakeNetworkMonitor.setConnected(true)

        viewModel.uiState.test {
            val item = awaitItem()
            // May start with Loading or immediately emit Success depending on dispatcher
            val successState = if (item is HomeUiState.Loading) awaitItem() else item
            assertTrue(successState is HomeUiState.Success)
            assertEquals(1, successState.users.size)
            assertTrue(successState.isOnline)
            assertFalse(successState.isDarkMode)
        }
    }

    @Test
    fun `toggleDarkMode updates repository state`() = runTest {
        val viewModel = HomeViewModel(
            userRepository = fakeUserRepository,
            networkMonitor = fakeNetworkMonitor
        )

        viewModel.toggleDarkMode(current = false)

        fakeUserRepository.isDarkMode.test {
            assertTrue(awaitItem())
        }
    }
}
