# AGENTS.md

## Project Overview
Kickoff is a deterministic CLI project scaffolding tool and Gradle quality plugin suite for Android. It generates production-grade Android applications following Google's Now-in-Android multi-module architecture, Jetpack Compose, Material 3, Hilt, Room, Retrofit, and integrated static code analysis.

## Monorepo Subprojects
- `:kickoff-cli`: Standalone CLI application built with Kotlin 2.0.21 and Clikt. Generates multi-module Android projects from uncompressed templates with machine-readable `--json` output.
- `:quality-verifier`: Standalone Gradle plugin (`pt.andrer757.quality-verifier`) bundling Detekt (with Compose rules), Spotless/Ktlint, Android Lint, and Kover.
- `templates/now-in-android`: Uncompressed, git-tracked template featuring 12 modules (`:app`, `:core:*`, `:feature:home`), `build-logic` convention plugins, and version catalog.

## Development Guidelines
- **Build System**: Gradle 8.12 (`./gradlew build` / `./gradlew test`)
- **JVM Toolchain**: Java 21 (Temurin-21 located in `~/.jdks/temurin-21`)
- **Languages**: Kotlin 2.0.21
- **CLI Executable**: `./kickoff new --name "<Name>" --package "<Package>" --output "<Dir>" [--json]`

## Project Skills
The following domain skill packs are installed in `.agents/skills/`:
- `kickoff-scaffold`: Scaffolding commands, CLI parameters, and machine-readable output schemas for generating Android projects.
- `android`: Android architecture, components, Gradle configs, testing, and best practices.
