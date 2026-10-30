# Kickoff AI Development Kit

Every project scaffolded by Kickoff can include a comprehensive **Multi-Agent AI Swarm** pre-aligned with Android Clean Architecture, Jetpack Compose, and Google's Now-in-Android design patterns.

---

## 1. Supported AI Environments

Kickoff natively formats agents and skills for all major AI coding assistants:

| AI Tool | Directory in Scaffolded App | Configuration Format |
|---|---|---|
| **GitHub Copilot** | `.github/agents/`, `.github/skills/`, `.github/governance/` | `.agent.md` files with user-invocable tags |
| **Claude Code** | `.claude/agents/`, `.claude/skills/`, `.claude/settings.json` | `.md` files with Claude Code skill bindings |
| **Cursor** | `.cursor/rules/`, `.agents/skills/` | Official `.mdc` rules with glob triggers |
| **Antigravity / Generic** | `.agents/agents/`, `.agents/skills/` | Standard Markdown agent specifications |

---

## 2. The 16 Specialized AI Agents

Kickoff's AI kit follows the **Separation of Concerns** principle: rather than relying on a single generalist prompt, it provides dedicated specialist agents:

### Core Architecture & Leadership
- **`LeadAndroidAgent`**: The orchestrator and planner. Breaks down user requirements, tracks ACCs (Architecture Conformance Contracts), sequences tasks, and issues final acceptance verdicts.
- **`AndroidArchitectAgent`**: Evaluates proposed changes, classifies scopes as `LOCAL` or `STRUCTURAL`, validates dependency directions, checks Hilt DI graph hygiene, and enforces Clean Architecture invariants.

### Implementation Specialists
- **`AndroidProgrammer`**: The core implementation engine. Writes production Kotlin code across `:core:*` and `:feature:*` modules, scaffolding UseCases, Repositories, ViewModels, and Compose screens.
- **`AndroidComposeSpecialist`**: Dedicated to Jetpack Compose UI. Focuses on Composable functions, state hoisting, `@Stable`/`@Immutable` annotations, recomposition minimization, and Compose performance audits.
- **`AndroidCoroutinesSpecialist`**: Focused on concurrency. Manages `StateFlow`, `SharedFlow`, structured concurrency scopes, dispatcher injection (`@Dispatcher(IO)`), and Turbine-based flow testing.

### Quality, Security & Guardianship
- **`AndroidTestWriter`**: Authors deterministic unit, integration, and UI tests using JUnit 5, Turbine, and Fake repositories.
- **`AndroidTestReviewer`**: Acts as an adversarial quality gate with veto authority. Audits tests for concurrency flakiness, missing edge cases, and architectural compliance before acceptance.
- **`AndroidPrivacyGuardian`**: Audits code to prevent user data leaks, unauthorized permission requests, and hardcoded secrets.
- **`AndroidSDKGuardian`**: Protects the stability and backward compatibility of core shared module interfaces.

### Code Review Suite
- **`LeadCodeReviewAgent`**: Orchestrates deep code reviews across multiple investigative axes.
- **`CodeReviewDiffSummaryAgent`**: Produces concise, structured summaries of git changes.
- **`CodeReviewImpactSurfaceAgent`**: Maps out which downstream modules and interfaces are affected by a diff.
- **`CodeReviewGitHistoryAgent`**: Analyzes git blame and commit patterns for context.
- **`CodeReviewAssumptionTrackerAgent`**: Identifies unverified assumptions in implementation logic.
- **`CodeReviewDecisionMemoryAgent`**: Cross-references changes against the project's immutable decision log.
- **`GitForensicsAgent`**: Investigates regressions and historical architectural changes.

---

## 3. The 25 Atomic Architecture Skills

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

## 4. Cross-Platform Setup Scripts

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

---

## 5. How to Invoke Agents in Your IDE

### GitHub Copilot (VS Code)
1. Open the project in VS Code.
2. Open Copilot Chat (`Ctrl+Alt+I` / `Cmd+Alt+I`).
3. Type `@` to select an agent:
   - `@LeadAndroidAgent I need to add a user authentication flow`
   - `@AndroidComposeSpecialist Review this screen for recomposition leaks`

### Claude Code
1. Run `claude` from the project root.
2. Claude Code automatically detects `.claude/agents/` and `.claude/skills/`.
3. Prompt directly:
   `Have AndroidArchitectAgent validate the new :feature:settings module boundaries.`

### Cursor
1. Cursor automatically reads `.cursor/rules/*.mdc`.
2. As you edit Kotlin and Gradle files, Cursor applies the relevant architecture and Compose rules automatically.
