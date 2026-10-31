package ${configs.packageName}.core.data

import ${configs.packageName}.core.common.AppDispatchers
import ${configs.packageName}.core.common.AppResult
import ${configs.packageName}.core.common.Dispatcher
import ${configs.packageName}.core.database.UserDao
import ${configs.packageName}.core.database.UserEntity
import ${configs.packageName}.core.datastore.UserPreferencesDataSource
import ${configs.packageName}.core.model.User
import ${configs.packageName}.core.network.ApiService
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineFirstUserRepository @Inject constructor(
    private val userDao: UserDao,
    private val apiService: ApiService,
    private val userPreferencesDataSource: UserPreferencesDataSource,
    @Dispatcher(AppDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) : UserRepository {

    override val users: Flow<List<User>> = userDao.getUsers().map { entities ->
        entities.map { it.asExternalModel() }
    }

    override val isDarkMode: Flow<Boolean> = userPreferencesDataSource.isDarkMode

    override suspend fun setDarkMode(enabled: Boolean) {
        userPreferencesDataSource.setDarkMode(enabled)
    }

    override suspend fun syncUsers(): AppResult<Unit> = withContext(ioDispatcher) {
        try {
            val remoteUsers = apiService.getUsers()
            userDao.insertUsers(remoteUsers.map { it.asEntity() })
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(e)
        }
    }
}

private fun UserEntity.asExternalModel(): User = User(
    id = id,
    name = name,
    email = email
)

private fun User.asEntity(): UserEntity = UserEntity(
    id = id,
    name = name,
    email = email
)
