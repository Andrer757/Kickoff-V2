---
name: kickoff-scaffold
description: Deterministic Android project scaffolding CLI tool. Use this skill when asked to create, initialize, scaffold, or bootstrap a new Android project with Jetpack Compose, Material 3, Hilt, Room, Retrofit, and multi-module Now-in-Android architecture. Trigger on phrases like "scaffold an android app", "create a new android project", "kickoff a project", "bootstrap android", or "start an android app".
---

# Kickoff Project Scaffolding

Kickoff generates complete, production-grade Android projects based on Google's **Now in Android** multi-module architecture and Jetpack Compose.

## When to Use
- Starting any new Android application from scratch.
- Bootstrapping a multi-module Compose + Material 3 app with Hilt, Room, and Retrofit pre-wired.
- Generating verified architectural boundaries so AI agents and teams can work on independent modules.

---

## Command Usage

Run the CLI tool from the `kickoff-my` root:

```bash
./kickoff new \
  --name "<ProjectName>" \
  --package "<com.example.app>" \
  --output "<destination_path>" \
  --json
```

### CLI Parameters:
- `-n, --name` (Required): Human-readable application name (e.g., `"Fitness Tracker"`).
- `-p, --package` (Required): Application package ID (e.g., `"com.example.fitness"`).
- `-o, --output` (Optional): Output directory. Defaults to sanitized project name.
- `-t, --template` (Optional): Custom template path. Defaults to `templates/now-in-android`.
- `--ai` (Optional): Target AI environment to install (default: `all`):
  - `copilot`: Installs `.github/agents/`, `.github/skills/`, and governance.
  - `claude`: Installs `.claude/agents/`, `.claude/skills/`, and settings.
  - `cursor`: Installs `.cursor/rules/*.mdc` and shared `.agents/skills/`.
  - `generic`: Installs `.agents/agents/` and `.agents/skills/` (Antigravity/Codex).
  - `all` (default): Installs all supported AI agent environments.
  - `none`: Scaffolds a clean Android project without AI directories.
  - Supports comma-separated lists: e.g. `--ai claude,cursor`.
- `--min-sdk` (Optional): Minimum SDK API level (default: `26`).
- `--target-sdk` (Optional): Target SDK API level (default: `35`).
- `--json` (Recommended for AI): Prints machine-readable JSON status and metadata.

---

## Machine-Readable Output (`--json`)

When invoked with `--json`, Kickoff outputs structured JSON:

```json
{
  "status": "success",
  "projectPath": "/absolute/path/to/project",
  "projectName": "Fitness Tracker",
  "packageName": "com.example.fitness",
  "modules": [
    ":app",
    ":core:analytics",
    ":core:common",
    ":core:connectivity",
    ":core:database",
    ":core:datastore",
    ":core:designsystem",
    ":core:model",
    ":core:network",
    ":core:permissions",
    ":core:ui",
    ":feature:home"
  ],
  "entryPoints": {
    "application": "app/src/main/kotlin/com/example/fitness/KickoffApplication.kt",
    "mainActivity": "app/src/main/kotlin/com/example/fitness/MainActivity.kt",
    "homeScreen": "feature/home/src/main/kotlin/com/example/fitness/feature/home/HomeScreen.kt"
  }
}
```

---

## Scaffolded Architecture & Modules

Every generated project includes:
1. **`build-logic/`**: Pre-configured convention plugins:
   - `kickoff.android.application`
   - `kickoff.android.library`
   - `kickoff.android.feature`
   - `kickoff.android.compose`
   - `kickoff.android.hilt`
   - `kickoff.android.room`
2. **`gradle/libs.versions.toml`**: Unified version catalog (AGP 8.8+, Kotlin 2.x, Compose BOM, Hilt, Room, Retrofit).
3. **Core Modules**:
   - `:core:model`: Domain entities (`@Serializable`).
   - `:core:common`: Coroutine dispatchers (`@Dispatcher(IO)`), Result monad (`AppResult`).
   - `:core:connectivity`: Network connectivity monitor (`NetworkMonitor.isOnline` Flow).
   - `:core:analytics`: Typed analytics event logging (`AnalyticsHelper`).
   - `:core:permissions`: Compose runtime permission launcher (`rememberPermissionLauncher`).
   - `:core:network`: OkHttp 4 + Retrofit with Kotlinx Serialization converter.
   - `:core:database`: Room database, entities, and DAOs.
   - `:core:datastore`: Preferences DataStore.
   - `:core:designsystem`: Material 3 theme (dynamic colors, typography, shapes).
   - `:core:ui`: Common Compose state layouts (Loading, Error).
4. **Feature Modules**:
   - `:feature:home`: Working MVI Compose screen (`HomeScreen`, `HomeViewModel`, Type-safe route).
5. **Quality Verification**:
   - Pre-configured with `pt.andrer757.quality-verifier` running Detekt (Compose rules), Spotless (Ktlint), Android Lint, and Kover on `./gradlew check`.

---

## Post-Scaffolding Validation

After generating the project, always run:
```bash
cd <projectPath>
./gradlew assembleDebug
./gradlew check
```
