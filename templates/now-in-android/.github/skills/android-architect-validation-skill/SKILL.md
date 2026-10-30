---
name: android-architect-validation-skill
description: >
  Architecture validation workflow for the Android multi-module ecosystem. Use when:
  classifying scope as LOCAL or STRUCTURAL, issuing ACCEPTABLE/INSUFFICIENT architecture verdicts,
  generating ACC or ACC-lite entries, performing dependency direction analysis, Hilt graph hygiene review,
  or Gradle module topology review.
---

# Android Architect Validation

> Load `shared-architect-validation-protocol-skill` first, then `android-arch-layers-skill`.
> This skill adds Android-specific scope classification, Gradle/Hilt checklists, ACC YAML template, and risk escalation.
> Used exclusively by **AndroidArchitectAgent**.

---

## 1. Scope Classification

Before every architectural decision, classify the proposed change:

| Classification | Criterion |
|---------------|-----------|
| **LOCAL (Lite Lane)** | Change confined to a single Gradle module, OR adding a standard `:feature:<name>` screen module (`./kickoff add feature`) that only consumes existing `:core:data` and `:core:model` contracts without altering database schemas, adding external dependencies, or changing Hilt component scopes |
| **STRUCTURAL (Full Lane)** | Change modifies `:core:model`, `:core:database` (Room entities/DAOs/migrations), `:core:network`, or `:sdk-*` public APIs; introduces a new `:core:*` or `:sdk-*` module; adds new external dependencies in `libs.versions.toml`; modifies Hilt component hierarchy; or alters security/privacy storage |

**If genuinely ambiguous between LOCAL and STRUCTURAL, classify as STRUCTURAL.**

---

## 2. Architecture Verdict Protocol

For every architectural review, produce a concise verdict in this format:

```
## Architecture Verdict

**Scope:** LOCAL | STRUCTURAL
**ACC required:** YES | NO (ACC-lite for LOCAL scope)
**Verdict:** ACCEPTABLE | INSUFFICIENT
**Risk level:** LOW | MEDIUM | HIGH

### Findings
- <finding 1>
- <finding 2>

### Required ACC fields (if STRUCTURAL)
- acc_id: AND-ACC-<YYYY>-<NNN>
- acc_scope: STRUCTURAL
- acc_storage_path: .github/governance/android/acc-registry-android.yaml

### Conditions for ACCEPTABLE
- <condition if verdict is INSUFFICIENT>
```

---

## 3. ACC Template (STRUCTURAL scope)

When issuing a new ACC for a `STRUCTURAL` change, register it in `.github/governance/android/acc-registry-android.yaml` (keep `summary` ≤ 30 lines):

```yaml
- acc_id: AND-ACC-YYYY-NNN
  title: "<short description>"
  scope: STRUCTURAL
  date: "YYYY-MM-DD"
  status: APPROVED | PENDING | REJECTED
  author: AndroidArchitectAgent
  lead_approval: APPROVED | PENDING
  summary: >
    <Concise summary (<= 30 lines): what changes, why, and which modules are affected.>
  affected_modules:
    - :core:<module>
    - :feature:<module>
  dependency_changes:
    - from: <source>
      to: <target>
      type: new | removed
  hilt_impact: none | scope-change | new-binding | multi-binding
  invariants_verified:
    - domain_no_android_imports: true | false | N/A
    - no_cross_layer_data_leak: true | false | N/A
    - fake_contract_maintained: true | false | N/A
```

---

## 4. ACC-lite Template (LOCAL scope)

For `LOCAL` scope only — no registry entry required, declare inline in the implementation plan:

```
## ACC-lite
- scope: LOCAL
- module: :<module>
- summary: <What changes and why (<= 10 lines)>
- layer_boundaries_respected: YES | NO
- hilt_impact: none | constructor injection only
```

---

## 5. Architectural Review Checklist

### Layering
- [ ] `:core:model` imports no Android framework, Room, or Hilt classes
- [ ] `:feature:*` does not import `:core:database`, `:core:network`, `:core:datastore`, or another `:feature:*` module
- [ ] `:core:data` maps entities/DTOs to `:core:model` domain types
- [ ] `:app` module contains navigation and top-level DI wiring only — no business logic

### Dependency Direction
- [ ] Inter-module dependencies pass `./kickoff arch-check`
- [ ] No circular module dependencies

### Hilt/DI
- [ ] Component scope is appropriate (`@Singleton` / `@HiltViewModel`)
- [ ] No ServiceLocator usage in app modules (`:sdk-core` maintains zero-Hilt public surface)
- [ ] Fakes provided for new repository/domain interfaces

### Coroutines/Flow
- [ ] No `GlobalScope` or `runBlocking` in production code
- [ ] Dispatchers injected (`@Dispatcher(AppDispatchers.IO)`)
- [ ] ViewModels expose `StateFlow` — not `MutableStateFlow`

### Testing
- [ ] Fake provided for each new repository/domain interface
- [ ] Coroutine tests use `runTest` + `MainDispatcherRule`
- [ ] Flow assertions use Turbine — no `Thread.sleep`

---

## 6. Risk Escalation

Escalate to `LeadAndroidAgent` when:

- Risk level is **HIGH**
- ACC status is **PENDING** (not yet `APPROVED`)
- Change affects ≥3 `:core:*` modules simultaneously
- Hilt `SingletonComponent` binding or database migration changes
- Public `:sdk-*` binary compatibility (`api/*.api`) is broken
- Proposed change violates any non-negotiable invariant

**INSUFFICIENT** verdict blocks implementation. `AndroidProgrammer` must not proceed until verdict is resolved.

---

## 7. Output Contract

Every response from `AndroidArchitectAgent` using this skill must include:

1. Scope classification (`LOCAL` / `STRUCTURAL`)
2. Architecture verdict (`ACCEPTABLE` / `INSUFFICIENT`)
3. ACC or ACC-lite reference
4. Checklist evaluation summary
5. Open risks or conditions (if `INSUFFICIENT`)
