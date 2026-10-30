# Developer Governance Workflow — Android (EM → Dev → Commit)

## Purpose
Define how the Android team uses the agentic system to ship faster without losing architectural and quality control.

## Workflow

### 1. EM creates sprint tasks
- Each task must define objective, scope, affected Gradle modules, and acceptance criteria.
- If structural impact is expected (cross-module dependency, Gradle topology, local-vs-remote toggle), require ACC context upfront.
- Classify suspected scope: LOCAL or STRUCTURAL.

### 2. Developer executes task with LeadAndroidAgent
- Use LeadAndroidAgent to decompose work into architecture, implementation, and testing gates.
- Keep decisions explicit and persist evidence in governance artifacts when applicable.
- No implementation begins without an APPROVED ACC (STRUCTURAL) or ACC-lite (LOCAL).

### 3. Developer validates locally before commit (mandatory)
Run:

```bash
python3 .github/policies/android/android-governance-local-gate.py
```

This runs strict checks for Android ACC registry, agent consistency, and skill coverage.

### 4. Commit only when local governance gate is green
- If local gate fails, fix findings before commit.
- Avoid creating commits that knowingly violate strict governance checks.

### 5. PR remains organizational safety net
- CI (`governance-check-android.yml`) enforces the same governance checks.
- PR should confirm compliance, not discover first-time failures.

## Definition of Done (Governance)
A task is governance-done only if:
- ACC policy is satisfied for scope (STRUCTURAL: APPROVED ACC; LOCAL: ACC-lite declared).
- Local gate passes with 0 findings.
- Architecture gate: ACCEPTABLE from AndroidArchitectAgent.
- Test gate: ACCEPTABLE from AndroidTestReviewer with ≥3 adversarial scenarios.
- Backlog updated in `.github/backlog/Android/`.

## Roles
| Role | Responsibility |
|------|---------------|
| EM | Task quality, scope clarity, ACC pre-registration for structural work |
| Developer | Local strict validation, compliant implementation, Fake contracts |
| LeadAndroidAgent | Orchestration, ACC registration, gate sequencing, final verdict |
| AndroidArchitectAgent | Architecture review, scope classification, local/structural verdict |
| AndroidTestReviewer | Adversarial test quality gate |

## Notes
- If a strict check appears to be a false positive, follow rollback/triage policy in `.github/governance/android/enforcement-rollout-android.md`.
- Use `.github/governance/android/ops-metrics-android.json` weekly to keep threshold-based recommendations actionable.
- Never toggle local-vs-remote dependency in `settings.gradle` without an ACC — this is always STRUCTURAL scope.
