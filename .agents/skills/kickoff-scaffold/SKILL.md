---
name: kickoff-scaffold
description: Deterministic Android project scaffolding and Day-2 engineering CLI tool. Use this skill when asked to create, initialize, scaffold, or bootstrap a new Android App or Android SDK project, add features/modules/entities, run architecture checks, diagnose toolchain, or run the Kickoff MCP server. Trigger on phrases like "scaffold an android app", "create a new android project", "kickoff a project", "bootstrap android", "create an android sdk", "add a feature", "add a module", or "check architecture".
---

# Kickoff Project Scaffolding & Engineering Suite

Kickoff generates complete, production-grade Android projects based on Google's **Now in Android** multi-module architecture, or redistributable **Android SDK + Sample** suites with Binary Compatibility Validator and Dokka.

## When to Use
- Starting any new Android application (`--type app`) from scratch.
- Bootstrapping a redistributable multi-module Android SDK (`--type sdk`) with a companion showcase sample app.
- Adding new feature modules with auto-wired NavHost and Gradle dependencies (`kickoff add feature`).
- Adding core capability or SDK modules (`kickoff add module`).
- Scaffolding Room entities, DAOs, domain models, and offline-first repositories (`kickoff add entity`).
- Running sub-100ms static Clean Architecture checks (`kickoff arch-check`).
- Diagnosing local developer environment and toolchain (`kickoff doctor`).
- Launching the native JSON-RPC 2.0 stdio server (`kickoff mcp`).

---

## Command Usage

Run the CLI tool from the `kickoff-my` root:

### 1. Scaffold an Android App (Default)
```bash
./kickoff new \
  --name "Fitness Tracker" \
  --package "com.example.fitness" \
  --output "~/projects/fitness-tracker" \
  --type app \
  --json
```

### 2. Scaffold an Android SDK + Sample App
```bash
./kickoff new \
  --name "Payment SDK" \
  --package "com.example.paymentsdk" \
  --type sdk \
  --output "~/projects/payment-sdk" \
  --json
```

### 3. Add a Feature Module (App)
```bash
./kickoff add feature --name settings -d "~/projects/fitness-tracker" --json
```

### 4. Add a Module
```bash
./kickoff add module --name analytics --type core -d "~/projects/fitness-tracker" --json
./kickoff add module --name telemetry --type sdk -d "~/projects/payment-sdk" --json
```

### 5. Add an Entity (Model + Room + Repository)
```bash
./kickoff add entity --name Workout --fields "id:String,title:String,durationMinutes:Int" -d "~/projects/fitness-tracker" --json
```

### 6. Architecture Verification (<100ms)
```bash
./kickoff arch-check -d "~/projects/fitness-tracker"
```

### 7. Environment & Toolchain Diagnostics
```bash
./kickoff doctor [--json]
```

### 8. Native MCP Server
```bash
./kickoff mcp
```

---

## CLI Parameters for `kickoff new`:
- `-n, --name` (Required): Human-readable application or SDK name (e.g., `"Fitness Tracker"`).
- `-p, --package` (Required): Application package ID (e.g., `"com.example.fitness"`).
- `-o, --output` (Optional): Output directory. Defaults to sanitized project name.
- `--type` (Optional): Project archetype: `app` (Now-in-Android App) or `sdk` (Android SDK + Sample) (Default: `app`).
- `-t, --template` (Optional): Custom template path.
- `--min-sdk` (Optional): Minimum SDK API level (Default: `26`).
- `--target-sdk` (Optional): Target SDK API level (Default: `35`).
- `--ai` (Optional): Target AI environment to install (Default: `all`):
  - `copilot`, `claude`, `cursor`, `generic`, `all`, `none`.
- `--git / --no-git` (Optional): Automatically initialize git repository with initial commit (Default: `true`).
- `--json` (Recommended for AI): Prints machine-readable JSON status and metadata.

---

## Machine-Readable Output (`--json`)

### App Result:
```json
{
  "status": "success",
  "projectPath": "/absolute/path/to/project",
  "projectName": "Fitness Tracker",
  "packageName": "com.example.fitness",
  "projectType": "app",
  "modules": [
    ":app",
    ":core:analytics",
    ":core:common",
    ":core:connectivity",
    ":core:data",
    ":core:database",
    ":core:datastore",
    ":core:designsystem",
    ":core:model",
    ":core:network",
    ":core:permissions",
    ":core:testing",
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

### SDK Result:
```json
{
  "status": "success",
  "projectPath": "/absolute/path/to/project",
  "projectName": "Payment SDK",
  "packageName": "com.example.paymentsdk",
  "projectType": "sdk",
  "modules": [
    ":sample",
    ":sdk-core",
    ":sdk-testing",
    ":sdk-ui"
  ],
  "entryPoints": {
    "sdkClient": "sdk-core/src/main/kotlin/com/example/paymentsdk/sdk/core/SdkClient.kt",
    "sdkUiComponent": "sdk-ui/src/main/kotlin/com/example/paymentsdk/sdk/ui/SdkStatusBanner.kt",
    "sampleActivity": "sample/src/main/kotlin/com/example/paymentsdk/sample/SampleMainActivity.kt"
  }
}
```

---

## Post-Scaffolding Validation

After generating an App project:
```bash
cd <projectPath>
./gradlew assembleDebug
./gradlew check
```

After generating an SDK project:
```bash
cd <projectPath>
./gradlew apiCheck
./gradlew check
./gradlew :sample:assembleDebug
```
