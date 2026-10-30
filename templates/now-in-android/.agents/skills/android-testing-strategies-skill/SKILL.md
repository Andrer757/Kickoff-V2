---
name: android-testing-strategies-skill
description: Comprehensive testing strategy involving JUnit 5 Unit Tests, Coroutines/Turbine tests, Hilt Integration Tests, and Screenshot tests.
---

# Android Testing Strategies

This skill provides guidance on testing modern Android applications following Google's **Now in Android** architecture. It covers **Unit Tests (JUnit 5 + Turbine)**, **Hilt Integration Tests**, and **Screenshot Testing**.

## Testing Pyramid

1. **Unit Tests**: Fast, deterministic JVM tests for ViewModels, Repositories, and domain models using Fake implementations (`:core:testing` / `:sdk-testing`).
2. **Integration Tests**: Test interactions across Room DAOs and DataStore.
3. **UI / Screenshot Tests**: Verify Compose UI state rendering.

## Dependencies (`gradle/libs.versions.toml`)

```toml
[libraries]
junit-jupiter = { group = "org.junit.jupiter", name = "junit-jupiter", version.ref = "junitJupiter" }
kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "kotlinxCoroutines" }
turbine = { group = "app.cash.turbine", name = "turbine", version.ref = "turbine" }
androidx-compose-ui-test-junit4 = { group = "androidx.compose.ui", name = "ui-test-junit4" }
hilt-android-testing = { group = "com.google.dagger", name = "hilt-android-testing", version.ref = "hilt" }
```

## Deterministic ViewModel & Repository Unit Testing

Use JUnit 5 (`@RegisterExtension` with `MainDispatcherRule`), `runTest`, `Turbine`, and Fake repositories from `:core:testing`:

```kotlin
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
            val state = if (item is HomeUiState.Loading) awaitItem() else item
            val successState = assertInstanceOf(HomeUiState.Success::class.java, state)
            assertEquals(1, successState.users.size)
        }
    }
}
```

## Hilt Integration Testing

Use `HiltAndroidRule` to inject dependencies in instrumented tests:

```kotlin
@HiltAndroidTest
class UserDaoTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var database: AppDatabase
    private lateinit var userDao: UserDao

    @Before
    fun init() {
        hiltRule.inject()
        userDao = database.userDao()
    }
}
```

## Running Tests

Always run tests from the project root using `./gradlew`:

- **All Unit Tests**:
  `./gradlew testDebugUnitTest`
- **Single Module Unit Tests**:
  `./gradlew :feature:home:testDebugUnitTest`
  `./gradlew :core:data:testDebugUnitTest`
- **Specific Test Class**:
  `./gradlew :feature:home:testDebugUnitTest --tests "com.example.app.feature.home.HomeViewModelTest"`
- **Full Quality & Architecture Verification**:
  `./gradlew check`
