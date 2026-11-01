# Kickoff AI Development Kit & MCP Server

Every project scaffolded by Kickoff can include a comprehensive **Multi-Agent AI Swarm** pre-aligned with Android Clean Architecture, Jetpack Compose, Now-in-Android, and Android SDK design patterns, backed by a native **Model Context Protocol (MCP)** server.

---

## 1. Single Source of Truth (SSoT) & Dynamic Synthesis

Rather than duplicating agent instructions across four different IDE formats in git, Kickoff maintains a **Single Source of Truth** inside `templates/now-in-android/.agents/`.

When `./kickoff new ... --ai <env>` runs, `AiKitSynthesizer` dynamically compiles the canonical markdown specifications into IDE-native formats:

| AI Tool | Scaffolded Directory | Dynamic Synthesis Behavior |
|---|---|---|
| **GitHub Copilot** | `.github/agents/`, `.github/skills/`, `.github/governance/` | Generates `.agent.md` files with VS Code tool bindings and `user-invocable: true` tags. |
| **Claude Code** | `.claude/agents/`, `.claude/skills/`, `.claude/settings.json` | Generates `.md` files with Claude Code tool bindings and settings schema. |
| **Cursor** | `.cursor/rules/*.mdc`, `.agents/skills/` | Generates official `.mdc` rules with glob filters (`**/*.kt, **/*.kts`) and alwaysApply options. |
| **Antigravity / Generic** | `.agents/agents/`, `.agents/skills/` | Standard cross-agent Markdown specifications. |

---

## 2. Model Context Protocol (MCP) Server (`kickoff mcp`)

Kickoff includes a native stdio **JSON-RPC 2.0 MCP Server** built directly into the CLI executable. This allows AI assistants like Claude Desktop, Cursor, Antigravity, and Windsurf to execute scaffolding, module generation, and architecture checks as deterministic native tools.

### Running the MCP Server
```bash
./kickoff mcp
```

### IDE Configuration

#### Claude Desktop (`~/.config/Claude/claude_desktop_config.json`)
```json
{
  "mcpServers": {
    "kickoff": {
      "command": "kickoff",
      "args": ["mcp"]
    }
  }
}
```

#### Antigravity / Cursor
Register `kickoff` with command `path/to/kickoff` and argument `mcp`.

### Exposed Native Tools

| Tool Name | Parameters | Description |
|---|---|---|
| `kickoff_scaffold_project` | `name`, `packageName`, `outputDir`, `projectType`, `minSdk`, `targetSdk`, `ai`, `gitInit` | Scaffolds a new Now-in-Android App (`app`) or Android SDK (`sdk`) project. |
| `kickoff_add_feature` | `name`, `projectDir` | Scaffolds `:feature:<name>` with routes, ViewModel, screen, test, and wires NavHost and Gradle. |
| `kickoff_add_module` | `name`, `type` (`core`, `library`, `compose`, `sdk`), `projectDir` | Scaffolds `:core:<name>` or `:sdk-<name>` with convention plugins, consumer rules, and tests. |
| `kickoff_add_entity` | `name`, `fields`, `projectDir` | Scaffolds domain model, Room entity, DAO, and offline-first repository. |
| `kickoff_arch_check` | `projectDir` | Runs fast static Clean Architecture verification on module boundaries. |
| `kickoff_doctor` | `projectDir` | Runs environment diagnostics for JDK, Android SDK/API 35, and AI tools. |

---

## 3. The 16 Specialized AI Agents

Kickoff's AI kit follows the **Separation of Concerns** principle with dedicated specialist agents:

### Core Architecture & Leadership
- **`LeadAndroidAgent`**: The orchestrator and planner. Breaks down user requirements, tracks ACCs (Architecture Conformance Contracts), sequences tasks, and issues final acceptance verdicts.
- **`AndroidArchitectAgent`**: Evaluates proposed changes, classifies scopes as `LOCAL` or `STRUCTURAL`, validates dependency directions, checks Hilt DI graph hygiene, and enforces Clean Architecture invariants.

### Implementation Specialists
- **`AndroidProgrammer`**: The core implementation engine. Writes production Kotlin code across `:core:*`, `:feature:*`, and `:sdk-*` modules, scaffolding UseCases, Repositories, ViewModels, and Compose screens.
- **`AndroidComposeSpecialist`**: Dedicated to Jetpack Compose UI. Focuses on Composable functions, state hoisting, `@Stable`/`@Immutable` annotations, recomposition minimization, and Compose performance audits.
- **`AndroidCoroutinesSpecialist`**: Focused on concurrency. Manages `StateFlow`, `SharedFlow`, structured concurrency scopes, dispatcher injection (`@Dispatcher(IO)`), and Turbine-based flow testing.

### Quality, Security & Guardianship
- **`AndroidTestWriter`**: Authors deterministic unit, integration, and UI tests using JUnit 5, Turbine, and Fake repositories (`MainDispatcherRule`, `FakeUserRepository`, `FakeSdkClient`).
- **`AndroidTestReviewer`**: Acts as an adversarial quality gate with veto authority. Audits tests for concurrency flakiness, missing edge cases, and architectural compliance before acceptance.
- **`AndroidPrivacyGuardian`**: Audits code to prevent user data leaks, unauthorized permission requests, and hardcoded secrets.
- **`AndroidSDKGuardian`**: Protects the stability, backward compatibility, and binary ABI of core shared module interfaces and SDK libraries (`explicitApi()`, BCV `apiCheck`/`apiDump`, zero-Hilt public surface).

### Code Review Suite
- **`LeadCodeReviewAgent`**: Orchestrates deep code reviews across multiple investigative axes.
- **`CodeReviewDiffSummaryAgent`**: Produces concise, structured summaries of git changes.
- **`CodeReviewImpactSurfaceAgent`**: Maps out which downstream modules and interfaces are affected by a diff.
- **`CodeReviewGitHistoryAgent`**: Analyzes git blame and commit patterns for context.
- **`CodeReviewAssumptionTrackerAgent`**: Identifies unverified assumptions in implementation logic.
- **`CodeReviewDecisionMemoryAgent`**: Cross-references changes against the project's immutable decision log.
- **`GitForensicsAgent`**: Investigates regressions and historical architectural changes.

---

## 4. The 25 Atomic Architecture Skills

Agents load atomic, single-responsibility skills to keep their context windows focused and efficient:

| Category | Skills | Description |
|---|---|---|
| **Architecture & Layers** | `android-arch-layers-skill`, `android-arch-conventions-skill`, `android-architecture-core-skill`, `android-architect-validation-skill` | Structural layer rules, dependency directions, module layouts, and ACC templates. |
| **Build & Optimization** | `android-arch-build-skill`, `android-resource-stewardship-skill` | Gradle tasks, dependency hygiene, Proguard/R8 rules, and memory management. |
| **Compose & Concurrency** | `android-compose-ui-skill`, `android-compose-performance-audit-skill`, `android-coroutines-flows-skill` | Recomposition rules, Compose stability, StateFlow/SharedFlow patterns. |
| **Implementation & Quality** | `android-implementation-skill`, `android-testing-strategies-skill`, `android-test-chaos-skill` | Scaffolding contracts, adversarial test patterns, and verification protocols. |
| **Security & Privacy** | `android-privacy-security-skill` | Data leakage prevention and runtime permission handling. |
| **Forensics & Review** | `git-forensics-skill`, `code-review-*` | Multi-dimensional code review and forensic analysis. |

---

## 5. Cross-Platform Setup Scripts

Each generated project includes automated dependency setup scripts in `scripts/`:

### macOS & Linux (`scripts/setup-ai.sh`)
```bash
cd <your-scaffolded-project>
./scripts/setup-ai.sh
```
- Checks Git, Java, and VS Code CLI prerequisites.
- Checks/installs the official Google Android CLI.
- Configures Engram local context memory MCP.
- Installs Graphify for codebase knowledge graph navigation.

### Windows (`scripts/setup-ai.ps1`)
```powershell
cd <your-scaffolded-project>
.\scripts\setup-ai.ps1
```
- Automated PowerShell script performing equivalent dependency verification on Windows.
