package ${configs.packageName}.core.data

import ${configs.packageName}.core.common.AppResult
import ${configs.packageName}.core.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    val users: Flow<List<User>>
    val isDarkMode: Flow<Boolean>

    suspend fun setDarkMode(enabled: Boolean)
    suspend fun syncUsers(): AppResult<Unit>
}
