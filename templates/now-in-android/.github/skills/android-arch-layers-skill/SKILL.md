---
name: android-arch-layers-skill
description: >
  Layer architecture, dependency direction, Hilt/DI rules, Coroutines/Flow contracts,
  Compose UI contracts, and non-negotiable invariants for the Android ecosystem.
  Use when: validating layer boundaries, checking dependency direction, reviewing DI graph,
  assessing Coroutines/Flow correctness, or enforcing non-negotiable invariants.
  Loaded by: AndroidArchitectAgent, AndroidTestReviewer, LeadAndroidAgent.
---

# Android Architecture — Layers, Invariants & Contracts

> Atomic skill. Covers structural rules only.
> For workspace layout, tech stack and naming conventions → load `android-arch-conventions-skill`.
> For build/test commands → load `android-arch-build-skill`.

---

## 1. Layer Architecture

```
Compose UI (:feature:*)  →  ViewModel (:feature:*)  →  Repository Interface (:core:data)
                                                              ↓
                                                    OfflineFirstRepository (:core:data)
                                                              ↓
                                              DataSource (:core:database / :core:network / :core:datastore)
```

### Layer Ownership

| Layer | Gradle Modules | Allowed Internal Dependencies |
|-------|---------------|-------------------------------|
| App / Host | `:app` (or `:sample`) | `:feature:*`, `:core:*` (or `:sdk-*`) |
| Presentation / Feature | `:feature:<name>` | `:core:model`, `:core:data`, `:core:designsystem`, `:core:ui`, `:core:common`, `:core:connectivity`, `:core:permissions`, `:core:analytics` |
| Data / Orchestration | `:core:data` | `:core:model`, `:core:database`, `:core:network`, `:core:datastore`, `:core:common` |
| Storage / Network | `:core:database`, `:core:network`, `:core:datastore` | `:core:model`, `:core:common` |
| Domain Models | `:core:model` | **none** (innermost pure Kotlin module) |
| SDK Engine / UI | `:sdk-core`, `:sdk-ui`, `:sdk-testing` | `:sdk-ui` → `:sdk-core`; `:sdk-testing` → `:sdk-core`; `:sdk-core` → **none** |

**Invariants:**
- `:core:model` must never import Android framework classes, Room, Retrofit, Hilt, or any other module.
- `:feature:*` must never depend on another `:feature:*` module or directly on `:core:database`, `:core:network`, or `:core:datastore`.

---

## 2. Dependency Direction (Strict)

```
:app          →  :feature:*, :core:designsystem, :core:data, :core:model
:feature:*    →  :core:data, :core:model, :core:ui, :core:designsystem, :core:common
:core:data    →  :core:database, :core:network, :core:datastore, :core:model, :core:common
:core:model   →  (nothing internal or Android-specific)
```

For SDK projects (`--type sdk`):
```
:sample       →  :sdk-ui, :sdk-core
:sdk-ui       →  :sdk-core
:sdk-testing  →  :sdk-core
:sdk-core     →  (nothing internal; zero Hilt dependencies)
```

Reverse dependency direction is **forbidden** and enforced by `./kickoff arch-check` and `./gradlew verifyArchitecture`.

---

## 3. Hilt/DI Graph Rules

1. Component hierarchy: `SingletonComponent` → `ActivityRetainedComponent` → `ViewModelComponent` → `ActivityComponent`
2. Use `@HiltViewModel` for ViewModels, `@Singleton` for stateless/shared repositories and database/network clients.
3. Prefer `@Binds` over `@Provides` for interface-to-implementation bindings.
4. No ServiceLocator pattern in app modules; `:sdk-core` uses an internal manual container (`InternalSdkClient`) to keep a zero-Hilt public surface.
5. No circular DI dependencies.
6. Fakes (not Mocks) are used to replace injected dependencies in unit tests.

---

## 4. Coroutines + Flow Contracts

| Rule | Detail |
|------|--------|
| No `GlobalScope` | Always use structured concurrency (`viewModelScope`, `lifecycleScope`, or injected `CoroutineScope`) |
| No `runBlocking` in production | Only allowed in test entry points if necessary; prefer `runTest` |
| Dispatcher injection | Repositories and IO-bound classes receive `CoroutineDispatcher` via `@Dispatcher(AppDispatchers.IO)` |
| StateFlow preferred | ViewModels expose `StateFlow<UiState>` — never `MutableStateFlow` publicly |
| SharedFlow for events | One-time UI events use `SharedFlow` with `replay = 0` |
| No `Thread.sleep` in tests | Use `runTest` and `MainDispatcherRule` |
| Flow collection in tests | Use `turbine` (`.test { awaitItem() }`) |

---

## 5. Compose UI Contracts

| Rule | Detail |
|------|--------|
| State hoisting | Stateless screen Composables receive `uiState` + callbacks + `modifier: Modifier = Modifier`, never ViewModel or business logic |
| UiState is sealed | Each screen defines a `sealed interface <Name>UiState` or `@Immutable data class` |
| Navigation | Use Jetpack Navigation Compose type-safe routes (`@Serializable data object <Name>Route`) |
| Side effects | Use `LaunchedEffect`, `SideEffect`, `DisposableEffect` — never call `suspend` directly inside composable body |

---

## 6. Non-Negotiable Invariants

1. No `GlobalScope`.
2. No `runBlocking` in production code.
3. No `lateinit var` for ViewModel UI state — use `StateFlow`.
4. No cross-layer DTO/Entity leakage — map Room entities and network DTOs to `:core:model` inside `:core:data`.
5. No direct dependency on `android.content.Context` in `:core:model` or ViewModels.
6. Every public repository interface has a Fake for testing.
7. `:core:model` has zero Android framework dependencies.
8. Gradle module topology or cross-module contract changes require an ACC (`STRUCTURAL`), except adding a standard `:feature:<name>` screen module that only consumes existing `:core:*` contracts (`LOCAL` Lite Lane).
