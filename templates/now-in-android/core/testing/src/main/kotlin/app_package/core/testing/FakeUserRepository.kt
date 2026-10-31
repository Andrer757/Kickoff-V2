package ${configs.packageName}.core.testing

import ${configs.packageName}.core.common.AppResult
import ${configs.packageName}.core.data.UserRepository
import ${configs.packageName}.core.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeUserRepository : UserRepository {

    private val _users = MutableStateFlow<List<User>>(emptyList())
    override val users: Flow<List<User>> = _users.asStateFlow()

    private val _isDarkMode = MutableStateFlow(false)
    override val isDarkMode: Flow<Boolean> = _isDarkMode.asStateFlow()

    fun setUsers(users: List<User>) {
        _users.value = users
    }

    override suspend fun setDarkMode(enabled: Boolean) {
        _isDarkMode.value = enabled
    }

    override suspend fun syncUsers(): AppResult<Unit> {
        return AppResult.Success(Unit)
    }
}
