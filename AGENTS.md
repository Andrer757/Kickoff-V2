# AGENTS.md

## Project Overview
Kickoff is a deterministic CLI project scaffolding tool and Gradle quality plugin suite for Android. It generates production-grade Android applications following Google's Now-in-Android multi-module architecture, Jetpack Compose, Material 3, Hilt, Room, Retrofit, and integrated static code analysis.

## Monorepo Subprojects
- `:kickoff-cli`: Standalone CLI application built with Kotlin 2.4.21 and Clikt. Generates multi-module Android projects from uncompressed templates with machine-readable `--json` output.
- `:quality-verifier`: Standalone Gradle plugin (`eu.andrer757.quality-verifier`) bundling Detekt (with Compose rules), Spotless/Ktlint, Android Lint, and Kover.
- `templates/now-in-android`: Uncompressed, git-tracked template featuring 14 modules (`:app`, 12 `:core:*` modules, `:feature:home`), `build-logic` convention plugins, Navigation 3, and version catalog.
- `templates/android-sdk`: Uncompressed, git-tracked template featuring 4 modules (`:sdk-core`, `:sdk-ui`, `:sdk-testing`, `:sample`), BCV, and Dokka.

## Development Guidelines
- **Build System**: Gradle 9.9.1 (`./gradlew check` / `./gradlew test`)
- **JVM Toolchain**: Java 21 (Temurin-21 located in `~/.jdks/temurin-21`)
- **Languages**: Kotlin 2.4.21
- **Android Toolchain**: AGP 9.5.1, `compileSdk = 37`, default `targetSdk = 37`
- **CLI Executable**: `./kickoff new --name "<Name>" --package "<Package>" --output "<Dir>" [--json]`

## Project Skills
The following domain skill pack is installed in `.agents/skills/`:
- `kickoff-scaffold`: Scaffolding commands, CLI parameters, and machine-readable output schemas for generating Android projects.
