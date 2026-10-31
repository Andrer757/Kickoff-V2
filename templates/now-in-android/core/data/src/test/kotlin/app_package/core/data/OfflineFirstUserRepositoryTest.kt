package ${configs.packageName}.core.data

import ${configs.packageName}.core.common.AppResult
import ${configs.packageName}.core.database.UserDao
import ${configs.packageName}.core.database.UserEntity
import ${configs.packageName}.core.datastore.UserPreferencesDataSource
import ${configs.packageName}.core.model.User
import ${configs.packageName}.core.network.ApiService
import app.cash.turbine.test
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class OfflineFirstUserRepositoryTest {

    private val testDispatcher = StandardTestDispatcher()

    private val fakeDao = object : UserDao {
        private val list = MutableStateFlow<List<UserEntity>>(emptyList())
        override fun getUsers(): Flow<List<UserEntity>> = list.asStateFlow()
        override suspend fun insertUsers(users: List<UserEntity>) {
            list.value = users
        }
    }

    private val fakeApi = object : ApiService {
        var throwError: Boolean = false
        override suspend fun getUsers(): List<User> {
            if (throwError) throw RuntimeException("Network error")
            return listOf(User(id = "1", name = "Test User", email = "test@example.com"))
        }
    }

    @Test
    fun `users flow maps entities to domain models`() = runTest(testDispatcher) {
        val repository = OfflineFirstUserRepository(
            userDao = fakeDao,
            apiService = fakeApi,
            userPreferencesDataSource = createDummyPreferencesSource(),
            ioDispatcher = testDispatcher
        )

        fakeDao.insertUsers(listOf(UserEntity(id = "1", name = "Alice", email = "alice@example.com")))

        repository.users.test {
            val userList = awaitItem()
            assertEquals(1, userList.size)
            assertEquals("Alice", userList[0].name)
        }
    }

    @Test
    fun `syncUsers inserts remote users into dao on success`() = runTest(testDispatcher) {
        val repository = OfflineFirstUserRepository(
            userDao = fakeDao,
            apiService = fakeApi,
            userPreferencesDataSource = createDummyPreferencesSource(),
            ioDispatcher = testDispatcher
        )

        val result = repository.syncUsers()
        assertTrue(result is AppResult.Success)

        repository.users.test {
            val userList = awaitItem()
            assertEquals(1, userList.size)
            assertEquals("Test User", userList[0].name)
        }
    }

    @Test
    fun `syncUsers returns error on failure`() = runTest(testDispatcher) {
        fakeApi.throwError = true
        val repository = OfflineFirstUserRepository(
            userDao = fakeDao,
            apiService = fakeApi,
            userPreferencesDataSource = createDummyPreferencesSource(),
            ioDispatcher = testDispatcher
        )

        val result = repository.syncUsers()
        assertTrue(result is AppResult.Error)
    }

    private fun createDummyPreferencesSource(): UserPreferencesDataSource {
        // Return dummy instance or mock for unit test
        return UserPreferencesDataSource(
            dataStore = FakePreferencesDataStore()
        )
    }

    private class FakePreferencesDataStore : androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences> {
        private val state = MutableStateFlow(androidx.datastore.preferences.core.emptyPreferences())
        override val data: Flow<androidx.datastore.preferences.core.Preferences> = state.asStateFlow()
        override suspend fun updateData(transform: suspend (t: androidx.datastore.preferences.core.Preferences) -> androidx.datastore.preferences.core.Preferences): androidx.datastore.preferences.core.Preferences {
            val next = transform(state.value)
            state.value = next
            return next
        }
    }
}
