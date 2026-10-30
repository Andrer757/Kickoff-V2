---
name: android-arch-build-skill
description: >
  Expertise in Gradle build systems, dependency hygiene, and build-time optimizations
  for large-scale Android projects. Focuses on binary size, build speed, and R8/Proguard health.
---

# Android Build & Optimization Skill

This skill provides expert guidance on maintaining a healthy Gradle build system and optimizing the final APK/Bundle performance.

## 1. Gradle Entry Points & Commands

This project uses a unified root Gradle wrapper with convention plugins:

| Task | Command | Purpose |
|------|---------|---------|
| Build Debug APK | `./gradlew assembleDebug` | Compiles app and all dependent modules |
| Run Unit Tests | `./gradlew test` | Executes unit tests across all modules |
| Quality Checks | `./gradlew check` | Runs Detekt (Compose rules), Spotless, Android Lint, Kover |
| Module Tests | `./gradlew :feature:home:test` | Focused testing on a single module |
| Auto-Format Code | `./gradlew spotlessApply` | Auto-formats Kotlin and Gradle KTS code |


---

## 2. Dependency Hygiene

- **No Wildcards:** Prohibit the use of dynamic versions (e.g., `+`). Use fixed versions defined in `libs.versions.toml`.
- **Api vs Implementation:** Use `implementation` by default to ensure compile-time isolation. Use `api` only if the consumer module strictly requires access to the transitive dependencies.
- **Version Catalog:** All dependencies and plugins must be centralized in the `libs.versions.toml` file.

## 2. R8/Proguard Health

- **Avoid Broad Keep Rules:** Never use broad rules like `-keep class com.example.app.** { *; }`. Rules must be surgical and documented.
- **Consumer Rules:** Review `consumer-rules.pro` in libraries to ensure they don't force unnecessary code retention in the main app.
- **Optimization:** Ensure R8 optimizations are enabled for release builds to minimize binary size.

## 3. Build Performance

- **Configuration Phase:** Avoid expensive logic in `build.gradle.kts` files that executes during the Gradle configuration phase.
- **KSP Migration:** Prioritize the use of KSP (Kotlin Symbol Processing) over Kapt to reduce compilation time.
- **Modularization:** Ensure proper module boundaries to maximize Gradle's incremental build and parallel execution capabilities.
