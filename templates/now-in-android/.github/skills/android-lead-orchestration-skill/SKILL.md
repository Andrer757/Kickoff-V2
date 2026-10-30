---
name: android-lead-orchestration-skill
description: >
  Lead orchestration workflow for the Android multi-module ecosystem. Use when:
  planning and delegating Android scopes, routing between Lite Lane (LOCAL) and Full Lane (STRUCTURAL),
  registering ACCs, managing gate sequencing, enforcing backlog state, or issuing final
  APPROVED/REJECTED/BLOCKED verdicts.
---

# Android Lead Orchestration

> This skill is used exclusively by **LeadAndroidAgent**.
> Always load `android-arch-layers-skill` first.

---

## 1. Orchestration State Machine & Execution Lanes

Every governed scope follows the state machine from `IntentDefined` to `LeadFinalized`, using either the **Lite Lane (`LOCAL`)** or **Full Lane (`STRUCTURAL`)**:

### Lite Lane (`LOCAL` Scope — Fast Path, ≤ 3 Sub-Agent Turns)
Use for single-module changes, bug fixes, UI/ViewModel updates, or adding a standard `:feature:<name>` screen module (`./kickoff add feature`) that consumes existing `:core:data` and `:core:model` contracts:

```
IntentDefined (Lead declares inline ACC-lite, ≤ 30 lines; total plan ≤ 150 lines)
    ↓
Implemented            ← AndroidProgrammer delivers code + Fakes + unit tests
    ↓
ChaosValidated         ← AndroidTestReviewer (single pass): ACCEPTABLE required
    ↓
LeadFinalized          ← LeadAndroidAgent: APPROVED | REJECTED | BLOCKED
```
- **No separate `AndroidArchitectAgent`, `AndroidPrivacyGuardian`, or `AndroidSDKGuardian` turn** unless a structural, privacy/auth, or `:sdk-*` public API trigger applies.

### Full Lane (`STRUCTURAL` Scope)
Use when modifying `:core:model`, `:core:database` schemas/entities, `:core:network`, `:sdk-*` public APIs, adding `:core:*` modules, or adding external dependencies:

```
IntentDefined
    ↓ (optional HistoricalContext via GitForensicsAgent)
ArchitectValidated     ← AndroidArchitectAgent: ACCEPTABLE required
    ↓ (acc_id APPROVED in .github/governance/android/acc-registry-android.yaml)
ACCLocked
    ↓ (implementation starts)
Implemented            ← AndroidProgrammer delivers code + Fakes + unit tests
    ↓ (tests submitted)
ChaosValidated         ← AndroidTestReviewer: ACCEPTABLE required
    ↓ (lead reviews)
LeadFinalized          ← LeadAndroidAgent: APPROVED | REJECTED | BLOCKED
```

---

## 2. ACC Reference Contract & Size Budgets

- **Plan Budget:** Lead proposals and task breakdowns must be **≤ 150 lines** (≤ 8 KB).
- **ACC Summary Budget:** Registry summaries must be **≤ 30 lines**.

For `STRUCTURAL` scopes, the following must be present before implementation proceeds:

```
acc_id:           AND-ACC-YYYY-NNN
acc_scope:        STRUCTURAL
acc_storage_path: .github/governance/android/acc-registry-android.yaml
acc_status:       APPROVED
```

For `LOCAL` scopes, declare an inline `ACC-lite` in the implementation plan:

```
acc_scope:        LOCAL
acc_lite_summary: <what changes, which module, why it is LOCAL>
```

---

## 3. ACC Registration Protocol

### `STRUCTURAL` scope
1. Lead or Architect assigns `acc_id: AND-ACC-YYYY-NNN`
2. Entry added to `.github/governance/android/acc-registry-android.yaml` with `status: PENDING`
3. `AndroidArchitectAgent` reviews and issues verdict
4. Lead updates `status: APPROVED` before delegating to `AndroidProgrammer`
5. `acc_id` referenced in all subsequent agent inputs

### `LOCAL` scope (Lite Lane)
1. `ACC-lite` declared inline in the Lead plan
2. No YAML registry entry required
3. `AndroidProgrammer` implements against the `ACC-lite`
4. `AndroidTestReviewer` verifies tests and confirms scope remained `LOCAL`

---

## 4. Delegation Contracts

### Delegating to `AndroidArchitectAgent` (`STRUCTURAL` scopes only)
Provide:
```
- acc_id (if assigned) or scope classification request
- Change description: modules affected (:core:*, :feature:*, :sdk-*), dependency changes, Hilt DI impact
```
Expect: Architecture verdict (`ACCEPTABLE` / `INSUFFICIENT`) + ACC entry (≤ 30 lines).

### Delegating to `AndroidProgrammer`
Provide:
```
- acc_id (STRUCTURAL) or inline ACC-lite (LOCAL)
- acc_scope: STRUCTURAL | LOCAL
- Target Gradle module(s) (e.g. :feature:home, :core:data)
- Feature description and acceptance criteria
- Existing analogous files to mirror
- Test requirements (JUnit 5 + runTest + Turbine + Fakes)
```
Require: Implementation + unit tests + Fake for every new repository/domain interface + passing `./kickoff arch-check` and `./gradlew :<module>:testDebugUnitTest`.

### Delegating to `AndroidTestReviewer`
Provide:
```
- acc_id or ACC-lite reference
- acc_scope: STRUCTURAL | LOCAL
- Changed implementation and test file paths
- 3 specific adversarial scenarios to probe
- Independence instruction: evaluate current code and test execution only; do not query prior review verdicts
```
Expect: Quality verdict (`ACCEPTABLE` / `INSUFFICIENT`).

---

## 5. Final Verdict Protocol

Lead issues the final verdict after required gates pass:

1. For `STRUCTURAL` scopes, ensure the `APPROVED` entry is recorded in `.github/governance/android/acc-registry-android.yaml`
2. Append the completed scope to `.github/backlog/Android/lead-backlog.md`
3. Append to `.github/backlog/Android/decisions.md` only when a new architectural precedent (`AND-DEC-NNN`) is set

Verdict format:
```
## Lead Final Verdict

**Scope:** <description>
**acc_id:** AND-ACC-YYYY-NNN | ACC-lite
**Architecture verdict:** ACCEPTABLE (or Lite Lane LOCAL)
**Test quality verdict:** ACCEPTABLE (by AndroidTestReviewer, <date>)
**Final verdict:** APPROVED | REJECTED | BLOCKED

### Rationale
<Concise rationale>

### Conditions (if REJECTED/BLOCKED)
<What must change or be resolved>
```

---

## 6. Blocking & Rejection Conditions

Issue **BLOCKED** (not `REJECTED`) when:
- Mandatory input or product requirement is missing/ambiguous
- Another prerequisite scope must be completed first
- External dependency or environment prerequisite is unavailable

Issue **REJECTED** when:
- Architecture verdict is `INSUFFICIENT` after revision
- Test quality verdict is `INSUFFICIENT` after revision
- Non-negotiable invariant was violated and not resolved

---

## 7. Git-Tracked State & Optional Engram Protocol

Git-tracked files under `.github/governance/android/acc-registry-android.yaml` and `.github/backlog/Android/` are the default, self-contained persistence layer.

When `--engram` is enabled in the workspace and Engram MCP tools (`mem_save` / `mem_search`) are available:
- Save active scope milestones under `"Android acc [acc_id] ..."`
- Never block delivery if Engram is not configured or unavailable.

---

## 8. ACC Scope Policy

| Scope | Lane | ACC required | Registry entry | Separate Architect Turn |
|-------|------|-------------|----------------|-------------------------|
| `LOCAL` | Lite Lane | `ACC-lite` | NO | NO |
| `STRUCTURAL` | Full Lane | `AND-ACC-YYYY-NNN` | YES | YES |

**`STRUCTURAL` triggers:**
- Modifying `:core:model`, `:core:database` (Room entities/DAOs/migrations), `:core:network`, or `:sdk-*` public APIs
- Adding a new `:core:*` or `:sdk-*` module
- Adding a new external dependency in `gradle/libs.versions.toml`
- Changing Hilt component hierarchy or security/privacy storage

---

## 9. Local Governance Gate

Run the local governance gate to validate the ACC registry, agent consistency, and skill coverage:
```bash
python3 .github/policies/android/android-governance-local-gate.py
```

---

## 10. Non-Delegable Authorities

Only `LeadAndroidAgent` may:
- Register a new `AND-ACC-YYYY-NNN` ID
- Change ACC status from `PENDING` to `APPROVED` or `REJECTED`
- Issue `BLOCKED` verdict
- Promote or rollback governance enforcement stages
- Approve Gradle module topology changes
- Approve changes to `android-architecture-core-skill` invariants
