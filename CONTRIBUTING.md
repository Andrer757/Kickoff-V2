# Contributing to Kickoff

Thank you for your interest in contributing to **Kickoff**! As an open-source project, we welcome contributions from the community—whether it's fixing bugs, improving the Now-in-Android template, adding new AI agent capabilities, or enhancing documentation.

---

## 1. Code of Conduct & Values

- **Respect & Inclusivity**: Treat all contributors and users with empathy and respect.
- **Architectural Integrity**: Maintain Google's Now-in-Android multi-module architecture, Clean Architecture boundaries, and modern Kotlin practices.
- **Deterministic Tooling**: Ensure that scaffolding operations remain predictable, fast, and reproducible across different operating systems (Linux, macOS, Windows).

---

## 2. Setting Up Your Development Environment

### Prerequisites
- **Git**
- **JDK 17 or JDK 21** (Eclipse Temurin 21 recommended)
- **Gradle 8.12** (managed automatically by `./gradlew`)
- **Android SDK** (API 35 SDK installed)

### Initial Setup

1. Fork the repository on GitHub.
2. Clone your fork locally:
   ```bash
   git clone https://github.com/<your-username>/kickoff.git
   cd kickoff
   ```
3. Verify your Java setup:
   Ensure `gradle.properties` points to a compatible JDK 21 or that your `JAVA_HOME` is set to Java 21:
   ```properties
   org.gradle.java.home=/path/to/jdk-21
   ```
4. Run initial compilation and tests:
   ```bash
   ./gradlew test
   ```

---

## 3. Monorepo Project Structure

Kickoff is a multi-project Gradle monorepo:

- **`:kickoff-cli`**: Standalone CLI application built with Kotlin 2.0.21 and Clikt 5.
- **`:quality-verifier`**: Standalone Gradle plugin (`pt.andrer757.quality-verifier`) that bundles Detekt (with Compose rules), Spotless/Ktlint, Android Lint, and Kover.
- **`templates/now-in-android`**: Uncompressed, git-tracked template for the generated Android application and AI swarm.
- **`.agents/skills/`**: Domain skill packs for AI coding assistants.

---

## 4. Development & Testing Workflow

### Building the CLI Executable
To install the CLI binary into `kickoff-cli/build/install/kickoff-cli/bin/`:
```bash
./gradlew :kickoff-cli:installDist
```
You can then run the local CLI via the root wrapper:
```bash
./kickoff new --name "Test App" --package "com.example.test" --output "/tmp/test-app" --json
```

### Running Tests
Always ensure all tests pass before opening a Pull Request:
```bash
# Run all unit and integration tests across the monorepo
./gradlew test

# Run tests specifically for the CLI generator
./gradlew :kickoff-cli:test

# Run tests for the Quality Verifier Gradle plugin
./gradlew :quality-verifier:test
```

---

## 5. Coding Standards & Conventions

### Kotlin Guidelines
- Follow standard Kotlin idioms and formatting.
- Maximum line length and formatting are validated via Spotless and Ktlint.
- Auto-format code using:
  ```bash
  ./gradlew spotlessApply
  ```

### Commit Message Conventions
We follow [Conventional Commits](https://www.conventionalcommits.org/):

- `feat:` A new feature or capability (e.g., `feat: add support for KMP template preset`).
- `fix:` A bug fix (e.g., `fix: resolve file permission on gradlew script`).
- `docs:` Documentation improvements (e.g., `docs: update AI agent setup guide`).
- `refactor:` Code refactoring without changing behavior (e.g., `refactor: extract token replacer in ProjectGenerator`).
- `test:` Adding or updating tests (e.g., `test: add e2e test for --ai none flag`).
- `chore:` Maintenance tasks, dependency updates, or build script tweaks.

---

## 6. How to Contribute

### Reporting Issues
- Check existing issues to make sure the problem hasn't already been reported.
- Open a new issue with a clear title, reproduction steps, expected vs. actual behavior, and environment details (OS, JDK, Gradle version).

### Submitting a Pull Request
1. Create a feature branch off `main`:
   ```bash
   git checkout -b feat/your-feature-name
   ```
2. Implement your changes.
3. Add unit or integration tests verifying the behavior.
4. Run all tests and verification gates:
   ```bash
   ./gradlew test
   ```
5. Commit your changes with conventional commit messages.
6. Push to your fork and submit a Pull Request against `main`.
7. Fill out the PR description template clearly stating:
   - What changed and why.
   - Any architectural impact.
   - Evidence of successful test runs.

---

## 7. Community & Questions

If you have questions or ideas for architectural improvements:
- Open a discussion in GitHub Discussions.
- Join the community chat or issue tracker.

Happy hacking! 🚀
