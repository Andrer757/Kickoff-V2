# Quality Verifier Gradle Plugin

The `pt.andrer757.quality-verifier` Gradle plugin is a standalone static code analysis and Clean Architecture verification suite built directly into Kickoff. It automates and bundles essential quality tools for modern Android and Kotlin applications.

---

## 1. Bundled Static Analysis Tools

| Tool | Purpose | Configuration & Rules |
|---|---|---|
| **Clean Architecture Verifier** | Built-in Gradle task (`verifyArchitecture`) enforcing module boundaries | Validates that `:core:model` is innermost, features never depend on other features or storage directly, and `:sdk-*` modules never depend on `:sample`. |
| **Android Lint** | Official Android Gradle Plugin linting | Configured via typed AGP 8.8 `CommonExtension.lint` API. Critical security issues (`ExportedReceiver`, `HardcodedDebugMode`, `SetJavaScriptEnabled`, `GrantAllUris`, `NewApi`) trigger errors. |
| **Detekt** | Kotlin static code analysis and code smell detection | Bundles `io.nlopez.compose.rules` to enforce Jetpack Compose best practices (e.g., parameter ordering, modifier naming, Composable naming). |
| **Spotless / Ktlint** | Code formatting and style enforcement | Enforces standard Kotlin formatting via Ktlint 1.5.0 with custom overrides for Composable function naming. |
| **Kover** | JetBrains Kotlin code coverage | Generates code coverage reports for unit tests and enforces coverage minimums when configured. |

---

## 2. Applying the Plugin

In your root or module `build.gradle.kts`:

```kotlin
plugins {
    id("pt.andrer757.quality-verifier") version "1.0.0"
}
```

In the Kickoff Now-in-Android and Android SDK templates, the plugin is applied globally across all subprojects:

```kotlin
allprojects {
    apply(plugin = "pt.andrer757.quality-verifier")
}
```

---

## 3. Configuration DSL (`qualityVerifier { ... }`)

You can customize the verification behavior using the `qualityVerifier` extension in your Gradle build scripts:

```kotlin
qualityVerifier {
    // Enable or disable specific static analysis tools (all true by default)
    enableDetekt.set(true)
    enableSpotless.set(true)
    enableLint.set(true)
    enableKover.set(true)
    enableArchitectureCheck.set(true)

    // Whether failures should abort the build (default: false)
    abortOnError.set(false)

    // Optional: minimum test code coverage percent (default: 0)
    minCoveragePercent.set(0)

    // Optional: Point to custom Detekt or Lint config files
    // detektConfigFile.set(file("config/detekt.yml"))
    // lintConfigFile.set(file("config/lint.xml"))
}
```

---

## 4. Gradle Tasks & Lifecycle Integration

The plugin automatically hooks into Gradle's standard `check` task:

```bash
# Run all quality checks across the project (Architecture, Detekt, Spotless, Lint, Kover)
./gradlew check

# Run Clean Architecture boundary verification only
./gradlew verifyArchitecture

# Run Detekt analysis only
./gradlew detekt

# Run Spotless formatting check
./gradlew spotlessCheck

# Auto-format Kotlin code to pass formatting checks
./gradlew spotlessApply

# Generate Kover code coverage report
./gradlew koverHtmlReport
```

---

## 5. Clean Architecture Invariants Enforced by `verifyArchitecture`

The `verifyArchitecture` task verifies:

### App Invariants
- **`:core:model`**: Innermost domain entities. Must not depend on other internal modules.
- **`:feature:*`**: Never depend on another `:feature:*` module (must communicate via `:app` navigation or `:core:model`). Never depend directly on storage modules (`:core:database`, `:core:network`, `:core:datastore`)—must go through repository layer `:core:data`.
- **`:core:*`**: Must not depend on `:feature:*` or `:app` (except `:core:testing` test doubles).

### SDK Invariants
- **`:sdk-*`**: Must never depend on `:sample`.
- **`:sdk-core`**: Headless engine. Must never depend on `:sdk-ui` or `:sdk-testing` or Dagger/Hilt runtime.

---

## 6. Reports

Generated analysis reports are written to standard Gradle build directories:
- **Detekt**: `<module>/build/reports/detekt/detekt.html`
- **Android Lint**: `<module>/build/reports/lint-results.html`
- **Kover**: `<module>/build/reports/kover/html/index.html`
