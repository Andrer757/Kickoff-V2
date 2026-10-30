---
name: android-arch-conventions-skill
description: >
  Workspace layout, technology stack, module patterns, and test conventions for
  Kickoff Now-in-Android applications and Android SDK projects.
  Use when: implementing features, understanding repo structure, following naming
  conventions, or working with Gradle module topology.
  Loaded by: AndroidProgrammer.
---

# Android Architecture — Conventions & Patterns

> Atomic skill. Covers workspace structure, tech stack, and implementation conventions.
> For layer/invariant rules → load `android-arch-layers-skill`.
> For build/test commands → load `android-arch-build-skill`.

---

## 1. Workspace Layout (App Projects — `--type app`)

The project follows Google's **Now in Android** multi-module architecture:

```
:app                          → Android application entry point, KickoffApplication, MainActivity, NavHost
build-logic/                  → Gradle convention plugins (kickoff.android.*) and embedded quality-verifier
core/
├── model/                    → Pure domain models and entity definitions (@Serializable, zero Android imports)
├── common/                   → Dispatchers (@Dispatcher), Result monad (AppResult), coroutines extensions
├── connectivity/             → NetworkMonitor Flow via ConnectivityManager
├── analytics/                → AnalyticsHelper interface and logging
├── permissions/              → Compose permission handlers (PermissionHandler, rememberPermissionLauncher)
├── network/                  → Retrofit, OkHttp, Kotlinx Serialization, ApiService
├── database/                 → Room DB (AppDatabase), DAOs, Room entities (*Entity)
├── datastore/                → Preferences DataStore (UserPreferencesDataSource)
├── designsystem/             → Material 3 theme (dynamic colors, typography, shapes)
├── ui/                       → Common Compose UI layouts (Loading, Error)
├── data/                     → Offline-first repositories (*Repository, OfflineFirst*Repository)
└── testing/                  → Deterministic test doubles (MainDispatcherRule, Fake*Repository)
feature/
└── <name>/                   → Feature modules (e.g. :feature:home) with ViewModels, Compose screens, UI state, routes
```

---

## 2. Universal Technology Stack

| Dimension | Canonical Choice |
|-----------|-----------------|
| Language | Kotlin 2.0+ only — no new Java |
| UI framework | Jetpack Compose + Material 3 |
| Architecture | MVVM / MVI + Clean Architecture |
| Concurrency | Kotlin Coroutines + Flow (`StateFlow`, `SharedFlow`) |
| DI | Hilt/Dagger (`@Inject`, `@HiltViewModel`, `@Binds`) in App projects; manual DI in `:sdk-core` |
| Build system | Gradle 8.12 multi-module with convention plugins and `gradle/libs.versions.toml` |
| JVM Toolchain | JDK 21 (`gradle/gradle-daemon-jvm.properties`) |
| Testing | JUnit 5 (`useJUnitPlatform()`) + Turbine + `kotlinx-coroutines-test` + Fakes |

---

## 3. Android SDK Layout (SDK Projects — `--type sdk`)

SDK projects follow a binary-compatible library layout:

```
:sdk-core                     → Headless SDK engine (explicitApi(), zero-Hilt public surface, BCV api/sdk-core.api)
:sdk-ui                       → Optional Jetpack Compose UI components (@Immutable, BCV api/sdk-ui.api)
:sdk-testing                  → Consumer-facing Fake implementations (FakeSdkClient) and test rules
:sample                       → Showcase Android application consuming :sdk-core and :sdk-ui
```

Adding or removing modules or altering public `.api` baselines is a **structural governance event** — requires an ACC registered in `.github/governance/android/acc-registry-android.yaml`.

---

## 4. Module Profiles & Boundaries

### `:core:model`
- Pure Kotlin domain data classes.
- Zero dependencies on Android framework, Room, Retrofit, Hilt, or other internal modules.

### `:core:database`, `:core:network`, `:core:datastore`
- Low-level persistence and network data sources.
- Encapsulated behind `:core:data` repositories; never imported directly by `:feature:*` modules.

### `:core:data`
- Exposes clean Kotlin `Flow` repository interfaces and `OfflineFirst*Repository` implementations.
- Maps between Room `*Entity` / network DTOs and `:core:model` domain types.

### `:feature:<name>`
- Self-contained feature slice (`<Name>Route`, `<Name>Screen`, `<Name>ViewModel`, `<Name>UiState`).
- Depends on `:core:model`, `:core:data`, `:core:designsystem`, `:core:ui`, `:core:common`.
- Must never depend on another `:feature:*` module or directly on `:core:database` / `:core:network` / `:core:datastore`.

### `:app`
- Top-level application module that wires all `:feature:*` routes into `MainActivity`'s `NavHost`.

---

## 5. Day-2 Deterministic CLI Generators

Prefer Kickoff's built-in generators before hand-writing boilerplate:
- `./kickoff add entity --name <EntityName> --fields "title:String,done:Boolean"` — generates `:core:model` class, `:core:database` Entity + DAO + `AppDatabase` registration, and `:core:data` repository.
- `./kickoff add feature --name <feature>` — generates `:feature:<feature>` module, Route, Screen, ViewModel, unit test, and wires `settings.gradle.kts`, `app/build.gradle.kts`, and `MainActivity.kt`.
- `./kickoff add module --name <module> --type <core|library|compose|sdk>` — generates a new module and registers it in `settings.gradle.kts`.
- `./kickoff arch-check` — verifies all Clean Architecture module boundaries in `<100ms`.

---

## 6. Test Conventions

| Test type | Tools |
|-----------|-------|
| Unit — ViewModel | JUnit 5, `MainDispatcherRule`, Turbine (`.test {}`), `Fake*Repository` |
| Unit — Repository | JUnit 5, `StandardTestDispatcher`, `runTest`, fake DAO / data source |
| Compose UI | Compose testing APIs, `createComposeRule()` |

**Mandatory:** Every public repository or domain interface must have a deterministic `Fake<InterfaceName>` implementation for unit testing.
