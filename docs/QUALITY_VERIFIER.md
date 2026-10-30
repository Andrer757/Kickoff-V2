# Quality Verifier Gradle Plugin

The `pt.simdea.quality-verifier` Gradle plugin is a standalone static code analysis suite built directly into Kickoff. It automates and bundles essential quality tools for modern Android and Kotlin applications.

---

## 1. Bundled Static Analysis Tools

| Tool | Purpose | Configuration & Rules |
|---|---|---|
| **Detekt** | Kotlin static code analysis and code smell detection | Bundles `io.nlopez.compose.rules` to enforce Jetpack Compose best practices (e.g., parameter ordering, modifier naming, Composable naming). |
| **Spotless / Ktlint** | Code formatting and style enforcement | Enforces standard Kotlin formatting via Ktlint 1.5.0 with custom overrides for Composable function naming. |
| **Android Lint** | Official Android Gradle Plugin linting | Hooks into AGP's `lint` task with pre-configured severity and suppression rules. |
| **Kover** | JetBrains Kotlin code coverage | Generates code coverage reports for unit tests. |

---

## 2. Applying the Plugin

In your root or module `build.gradle.kts`:

```kotlin
plugins {
    id("pt.simdea.quality-verifier") version "1.0.0-SNAPSHOT"
}
```

In the Kickoff Now-in-Android template, the plugin is applied globally across all subprojects:

```kotlin
allprojects {
    apply(plugin = "pt.simdea.quality-verifier")
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

    // Whether failures should abort the build (default: false)
    abortOnError.set(false)

    // Optional: Point to custom Detekt or Lint config files
    // detektConfigFile.set(file("config/detekt.yml"))
    // lintConfigFile.set(file("config/lint.xml"))
}
```

---

## 4. Gradle Tasks & Lifecycle Integration

The plugin automatically hooks into Gradle's standard `check` task:

```bash
# Run all quality checks across the project (Detekt, Spotless, Lint, Kover)
./gradlew check

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

## 5. Reports

Generated analysis reports are written to standard Gradle build directories:
- **Detekt**: `<module>/build/reports/detekt/detekt.html`
- **Android Lint**: `<module>/build/reports/lint-results.html`
- **Kover**: `<module>/build/reports/kover/html/index.html`
