# Android Governance State Machine

## Objective
Define mandatory orchestration states and transition guards for governed Android delivery.

## States
1. IntentDefined
2. ArchitectValidated
3. ACCLocked
4. Implemented
5. ChaosValidated
6. LeadFinalized

## Transition Guards

### IntentDefined → ArchitectValidated
Required:
- Objective and scope are explicit.
- Affected repos, Gradle modules, and layers are listed.
- Suspected scope classification (LOCAL/STRUCTURAL) provided to AndroidArchitectAgent.

### ArchitectValidated → ACCLocked
Required:
- Architecture verdict is ACCEPTABLE.
- For STRUCTURAL scope: ACC entry (`AND-ACC-YYYY-NNN`) exists in `.github/governance/android/acc-registry-android.yaml` with `status: APPROVED`.
- For LOCAL scope: ACC-lite is declared in implementation plan.

### ACCLocked → Implemented
Required:
- acc_id (or ACC-lite) attached to AndroidProgrammerAgent input.
- **Bug Fix Protocol:** If the task is a bug fix, the `AndroidTestWriter` must first deliver a failing test (RED) that reproduces the bug. Implementation is only allowed after this reproduction test is validated.
- Allowed and forbidden changes are explicit (layer boundaries, DI scope, Gradle changes).

### Implemented → ChaosValidated
Required:
- Implementation files delivered.
- Fakes provided for all new domain interfaces.
- Test files provided with `runTest`-based coroutine tests and turbine Flow assertions.
- **Privacy Review:** AndroidPrivacyGuardian has issued an ACCEPTABLE verdict (no PII leaks or hardcoded secrets).

### ChaosValidated → LeadFinalized
Required:
- AndroidTestReviewer verdict is ACCEPTABLE.
- ≥3 adversarial scenarios probed and documented (including Process Death validation).
- No `Thread.sleep`, `GlobalScope`, or `runBlocking` in production code.

## Halt Conditions
- BLOCKED: Mandatory ACC missing, PENDING not resolved, or external dependency unavailable.
- REJECTED: Any required gate returns INSUFFICIENT after revision opportunity.

## Finalization Rule
Lead final decision must be exactly one of:
- APPROVED
- REJECTED
- BLOCKED

- **Documentation Sync:** After an APPROVED verdict, the LeadAndroidAgent must ensure the architectural decision (ADR) is mirrored in the corresponding `/documentation/` folder, keeping human documentation synchronized with the agentic system's state.
