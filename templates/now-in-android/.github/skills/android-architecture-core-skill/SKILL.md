---
name: android-architecture-core-skill
description: >
  Index of the Android architecture knowledge base. Prefer loading the specific
  atomic skill your role requires (see table below).
---

# Android Architecture Core — Index

This skill indexes the focused atomic skills that minimize context window usage.
Load only the atomic skills your role requires.

---

## Skill Map

| Skill | Contents | Who loads it |
|-------|----------|-------------|
| `android-arch-layers-skill` | Layer architecture, dependency direction, DI rules, Coroutines/Flow contracts, Non-negotiable invariants | AndroidArchitectAgent, AndroidTestReviewer, LeadAndroidAgent |
| `android-arch-conventions-skill` | Workspace layout, tech stack, module patterns, Day-2 CLI generators, test conventions | AndroidProgrammer |
| `android-arch-build-skill` | Build commands, test execution commands, Gradle task reference | AndroidProgrammer |
| `android-architect-validation-skill` | Scope classification (LOCAL Lite Lane vs STRUCTURAL), verdict protocol, ACC/ACC-lite templates | AndroidArchitectAgent |
| `android-implementation-skill` | Implementation patterns, Repository/ViewModel/Compose scaffolding | AndroidProgrammer |
| `android-test-chaos-skill` | Adversarial test checks, check matrices, ACCEPTABLE/INSUFFICIENT protocol | AndroidTestReviewer |
| `android-lead-orchestration-skill` | Lite Lane vs Structural Lane orchestration, gate sequencing, ACC registration | LeadAndroidAgent |

---

## Workspace Module Groups

| Module Group | Purpose |
|--------------|---------|
| `:app` | Main Android application entry point, `MainActivity`, top-level `NavHost` (Compose + Hilt) |
| `:feature:*` | Self-contained feature screens, ViewModels, and UI state (e.g. `:feature:home`) |
| `:core:*` | Shared foundation modules (`:core:model`, `:core:data`, `:core:database`, `:core:network`, `:core:datastore`, `:core:designsystem`, `:core:ui`, `:core:common`, `:core:connectivity`, `:core:permissions`, `:core:analytics`, `:core:testing`) |
| `:sdk-*` | Redistributable Android SDK modules (`:sdk-core`, `:sdk-ui`, `:sdk-testing`, `:sample`) in SDK projects |

---

## Non-Negotiable Invariants (quick reference)

> Full rules live in `android-arch-layers-skill`.

- **Hilt** is the canonical DI framework for apps (`:sdk-core` maintains a zero-Hilt public surface).
- **StateFlow** is the standard UI state contract. ViewModels expose `StateFlow<UiState>`.
- **GlobalScope** is prohibited. Always use structured concurrency scoped to a lifecycle.
- **runBlocking** is prohibited in production code. Use `suspend` + `CoroutineScope`.
- **Dispatchers** must be injected (`@Dispatcher(AppDispatchers.IO)`) — never hardcode `Dispatchers.IO` or `Dispatchers.Main`.
- **`:core:model`** must be framework-free (no Android, Hilt, or Room imports).
