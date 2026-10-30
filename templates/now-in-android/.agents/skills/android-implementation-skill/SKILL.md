---
name: android-implementation-skill
description: >
  Production Android implementation skill for the multi-module Kotlin/Compose ecosystem.
  Use when: implementing Kotlin features, writing UseCases/Repositories/ViewModels, scaffolding
  Compose screens, writing Hilt DI modules, implementing Coroutines/Flow pipelines, adding tests,
  or modifying existing code across :app, :feature:*, :core:*, and :sdk-* modules.
  Always operates within explicit ACC boundaries.
---

# Android Implementation Skill

> Load `android-arch-conventions-skill` and `android-arch-layers-skill` (indexed by `android-architecture-core-skill`) before using this skill.
> Never implement without a valid `acc_id` (`STRUCTURAL`) or inline `ACC-lite` (`LOCAL`).

---

## 1. Pre-Implementation Checklist (ACC Input Contract)

Before writing any code, verify the **ACC Input Contract**:

- [ ] `acc_id` (`STRUCTURAL`) or inline `ACC-lite` (`LOCAL`) is confirmed
- [ ] Target Gradle module(s) identified (`:feature:*`, `:core:*`, or `:sdk-*`)
- [ ] `settings.gradle.kts` / `build.gradle.kts` checked — no unintended dependency changes
- [ ] At least one analogous implementation in the workspace read and understood
- [ ] Hilt module entry point located
- [ ] Navigation and `UiState` patterns for the target feature module identified
- [ ] All new repository/domain interfaces have a `Fake` planned

---

## 2. Domain & Data Layer Implementation

### Domain Model (`:core:model`)

```kotlin
@Serializable
data class <Name>(
    val id: String,
    val title: String
)
```

Rules:
- Zero Android framework, Room, or Hilt imports in `:core:model`.

### Repository Interface & Offline-First Implementation (`:core:data`)

```kotlin
interface <Name>Repository {
    val items: Flow<List<<Name>>>
    suspend fun sync(): AppResult<Unit>
}

@Singleton
class OfflineFirst<Name>Repository @Inject constructor(
    private val dao: <Name>Dao,
    @Dispatcher(AppDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) : <Name>Repository {

    override val items: Flow<List<<Name>>> = dao.getAll().map { list ->
        list.map { it.asExternalModel() }
    }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun sync(): AppResult<Unit> = withContext(ioDispatcher) {
        try {
            AppResult.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.Error(e)
        }
    }
}
```

Rules:
- Maps at every boundary — never expose Room `*Entity` or network DTOs outside `:core:data`.
- Inject `CoroutineDispatcher` (`@Dispatcher(AppDispatchers.IO)`).
- Always rethrow `CancellationException` before catching general `Exception`.
- Provide a corresponding `Fake<Name>Repository` for unit tests.

---

## 3. Presentation Layer Implementation (`:feature:<name>`)

### UiState & ViewModel Pattern

```kotlin
@Serializable
data object <Name>Route

sealed interface <Name>UiState {
    data object Loading : <Name>UiState
    data class Success(val items: List<<Name>>) : <Name>UiState
    data class Error(val message: String?) : <Name>UiState
}

@HiltViewModel
class <Name>ViewModel @Inject constructor(
    private val repository: <Name>Repository
) : ViewModel() {

    val uiState: StateFlow<<Name>UiState> = repository.items
        .map<<Name>UiState>(<Name>UiState::Success)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = <Name>UiState.Loading
        )
}
```

### Compose Route & Screen Pattern

```kotlin
@Composable
fun <Name>Route(
    modifier: Modifier = Modifier,
    viewModel: <Name>ViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    <Name>Screen(
        uiState = uiState,
        modifier = modifier
    )
}

@Composable
fun <Name>Screen(
    uiState: <Name>UiState,
    modifier: Modifier = Modifier
) {
    Scaffold(modifier = modifier) { padding ->
        // Render state
    }
}
```

Rules:
- `<Name>Route` owns `hiltViewModel()`; `<Name>Screen` is stateless and accepts `modifier: Modifier = Modifier`.
- Never call `suspend` functions directly inside a Composable body — use `LaunchedEffect`.

---

## 4. Hilt DI Module Pattern

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bind<Name>Repository(
        impl: OfflineFirst<Name>Repository
    ): <Name>Repository
}
```

Rules:
- `@Binds` preferred over `@Provides` for interface binding.
- No business logic inside DI modules.

---

## 5. Test Implementation Patterns (JUnit 5 + Turbine + Fake)

### ViewModel Unit Test

```kotlin
class <Name>ViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcherRule = MainDispatcherRule()

    private val fakeRepository = Fake<Name>Repository()

    @Test
    fun `uiState emits Success when repository emits items`() = runTest {
        val viewModel = <Name>ViewModel(repository = fakeRepository)
        fakeRepository.setItems(listOf(<Name>(id = "1", title = "Sample")))

        viewModel.uiState.test {
            val item = awaitItem()
            val state = if (item is <Name>UiState.Loading) awaitItem() else item
            val success = assertInstanceOf(<Name>UiState.Success::class.java, state)
            assertEquals(1, success.items.size)
        }
    }
}
```

---

## 6. Post-Implementation Checklist

- [ ] All new repository/domain interfaces have a corresponding `Fake` implementation
- [ ] No `GlobalScope`, no `runBlocking` in production code
- [ ] Dispatchers injected — not hardcoded
- [ ] `StateFlow` exposed — not `MutableStateFlow`
- [ ] Room entities and network DTOs mapped to `:core:model` inside `:core:data`
- [ ] All coroutine tests use `runTest` and `turbine` — no `Thread.sleep`
- [ ] `:core:model` has zero Android framework imports
- [ ] `./kickoff arch-check` and `./gradlew testDebugUnitTest` pass
