---
name: android-test-chaos-skill
description: >
  Adversarial Android test quality gate for UseCase and ViewModel validation in the 
  multi-repo Kotlin/Compose ecosystem. Use when: reviewing test robustness, validating 
  Coroutines/Flow determinism, enforcing Architecture Constraint Contracts (ACC), 
  detecting cross-module breakage risks, and issuing ACCEPTABLE or INSUFFICIENT verdicts.
---

# Android Test Chaos — Adversarial Quality Gate

> This skill is used exclusively by **AndroidTestReviewerAgent**.  
> Always load `android-architecture-core-skill` first.  
> The agent's only verdicts are **ACCEPTABLE** and **INSUFFICIENT**.

---

## 1. Verdict Protocol

```
## Test Quality Verdict

**Subject:** <UseCase | ViewModel | Repository | Mapper>
**Verdict:** ACCEPTABLE | INSUFFICIENT
**ACC scope:** LOCAL | STRUCTURAL
**acc_id / ACC-lite:** <reference>

### Passed Checks
- <check>: PASS

### Failed Checks
- <check>: FAIL — <reason>

### Blocking Issues (if INSUFFICIENT)
1. <Specific test or pattern that must change>

### Conditions for ACCEPTABLE (if INSUFFICIENT)
- <What the programmer must fix before resubmission>
```

---

## 2. Mandatory Check Matrix

### 2.1 Coroutine / Flow Determinism

| Check | Criterion | Verdict if Failed |
|-------|-----------|-------------------|
| No `Thread.sleep` | Tests must not use `Thread.sleep` for timing — use `advanceUntilIdle()` or turbine | INSUFFICIENT |
| No `runBlocking` in production | `runBlocking` only in test code | INSUFFICIENT |
| `runTest` used | All suspend test functions wrapped in `runTest` | INSUFFICIENT |
| Dispatcher injected | UseCase/ViewModel under test receives injected `TestCoroutineDispatcher` | INSUFFICIENT |
| No `Dispatchers.Main` hardcoded | `setMain` used in test setup if needed, or dispatcher injected | INSUFFICIENT |
| Flow collection deterministic | Flow results collected with turbine or `take(n).toList()` — no inifinite collectors blocking tests | INSUFFICIENT |
| SharedFlow events captured | UI events from `SharedFlow` verified in test — not silently ignored | INSUFFICIENT |
| `advanceUntilIdle` used | Pending coroutines drained in tests — no phantom async operations | INSUFFICIENT |

### 2.2 UseCase Tests

| Check | Criterion | Verdict if Failed |
|-------|-----------|-------------------|
| Fake used, not mock | Dependencies replaced by `Fake<Interface>` — not `mockk<Interface>()` unless justified | INSUFFICIENT |
| Happy path covered | At least one success scenario tested | INSUFFICIENT |
| Error path covered | At least one failure / exception scenario tested | INSUFFICIENT |
| Empty / boundary cases | Edge cases covered (empty list, null, zero, max bounds as applicable) | WARNING |
| Dispatcher under test | `UnconfinedTestDispatcher` or `StandardTestDispatcher` passed explicitly | INSUFFICIENT |
| No business logic in Fake | Fakes return configured values only — no real logic | INSUFFICIENT |

### 2.3 ViewModel Tests

| Check | Criterion | Verdict if Failed |
|-------|-----------|-------------------|
| StateFlow assertions use turbine | `viewModel.uiState.test { ... }` — not raw `value` read after launch | INSUFFICIENT |
| Initial state tested | First emission verified after ViewModel creation | INSUFFICIENT |
| Loading state intermediate | Loading/pending state verified before success if applicable | WARNING |
| SharedFlow events captured | `viewModel.events.test { ... }` used for event assertions | INSUFFICIENT |
| No real coroutine delays | All delays eliminated via `TestCoroutineScheduler` | INSUFFICIENT |
| ViewModel receives injected dispatcher | Not `Dispatchers.IO` hardcoded internally | INSUFFICIENT |
| Intent isolation | Each intent tested independently — tests do not depend on prior intent calls | INSUFFICIENT |
| **Process Death Check** | ViewModel handles `SavedStateHandle` restoration | INSUFFICIENT |

### 2.4 Repository / Mapper Tests

| Check | Criterion | Verdict if Failed |
|-------|-----------|-------------------|
| Mapper tests are pure | Mapper tests have zero async code — purely synchronous assertions | INSUFFICIENT |
| Repository covers both data sources | Both remote and local data source paths tested | WARNING |
| Fakes for data sources | `Fake<DataSource>` used — not real network/DB | INSUFFICIENT |
| Error propagation tested | Failure from data source propagated correctly through repository | INSUFFICIENT |

### 2.5 ACC / Architecture Compliance

| Check | Criterion | Verdict if Failed |
|-------|-----------|-------------------|
| Domain imports clean | Test subject (`domain` layer) has no Android imports | INSUFFICIENT |
| No cross-layer leakage in tests | Tests do not import infrastructure types into domain test assertions | INSUFFICIENT |
| Fake provided for all new interfaces | Every new domain interface has a `Fake` shipped with the implementation | INSUFFICIENT |

---

## 3. Adversarial Scenarios to Probe

For every UseCase or ViewModel submission, probe at least 3 of these:

1. **Dispatcher swap** — Replace `UnconfinedTestDispatcher` with `StandardTestDispatcher`. Does the test still pass deterministically or does it deadlock?
2. **Fake failure injection** — Configure Fake to return `Result.failure(...)`. Is the error state correctly asserted in ViewModel?
3. **Rapid intent spam** — Send the same intent twice in quick succession. Does ViewModel handle cancellation of previous job gracefully?
4. **StateFlow cold start** — Collect `uiState` before any intent is sent. Is the initial state correct?
5. **SharedFlow miss** — If SharedFlow is used for events: is there a guarantee of collection before emission, or can events be missed? Document the risk.
6. **Scope cancellation** — Simulate ViewModel clear (`viewModel.onCleared()` in tests). Are coroutines cancelled?
7. **Empty repository result** — Fake returns empty list. Does ViewModel emit an appropriate empty/content state — not silently stay in loading?
8. **Network error** — Fake returns `IOException`. Is the error message surfaced to UiState.Error?
9. **Simulate Process Death** — In the test, populate a `SavedStateHandle`, create a new ViewModel instance with it, and verify that the UI state is restored to the expected post-death state.

---

## 4. ACC Scope Validation

### For STRUCTURAL scope
- Confirm `acc_id` matches an entry in `.github/governance/android/acc-registry-android.yaml`
- Confirm `status: APPROVED` before ACCEPTABLE verdict is issued
- If ACC is PENDING: issue INSUFFICIENT — block until Lead approves

### For LOCAL scope
- Confirm ACC-lite was declared in implementation plan
- Verify change is genuinely LOCAL (single module, no settings.gradle changes)
- If undeclared LOCAL scope found: warn and request retroactive ACC-lite

---

## 5. Escalation to LeadAndroidAgent

Escalate immediately if:
- Test coverage for critical domain paths is zero
- Coroutine determinism cannot be guaranteed without major architectural change
- ACC status is PENDING and implementation is complete
- Multiple INSUFFICIENT findings suggest systematic architectural problem

---

## 6. Non-Negotiable Production Code Checks

Always fail INSUFFICIENT if production code under test contains:
- `GlobalScope` — structured concurrency violation
- `runBlocking` in production classes
- `Thread.sleep` in production classes
- Hardcoded `Dispatchers.IO` without injection
- **State Loss Risk:** ViewModels containing critical mutable state that is NOT saved via `SavedStateHandle`.

---

## 7. ACCEPTABLE Conditions

Issue **ACCEPTABLE** only when:
- All INSUFFICIENT checks pass
- ≥3 adversarial scenarios probed and documented (including Process Death for ViewModels)
- acc_id or ACC-lite reference present and valid
- No open blocking issues
