---
name: android-resource-stewardship-skill
description: >
  Guidance for lean resource management in Android. Focuses on Hilt DI scope hygiene,
  memory leak prevention, and avoiding "God Object" bloat.
---

# Android Resource Stewardship

This skill ensures that the application remains lean, performant, and memory-efficient as it scales.

## 1. DI Scope Hygiene (Hilt)

The most common architectural debt in large projects is the overuse of @Singleton.

| Scope | Recommendation | Use Case |
|-------|----------------|----------|
| **@Singleton** | **AVOID** | Only for truly global state (e.g., Token Storage, Database). |
| **@ViewModelScoped** | **PREFER** | For data that only needs to live as long as a screen or flow. |
| **@ActivityScoped** | **SELECTIVE** | For UI-related logic shared across fragments in one activity. |
| **Unscoped** | **DEFAULT** | Most UseCases and Mappers should be unscoped (re-created per injection). |

### The "God Object" Veto
Any class injected into more than 15 other classes must be reviewed for "God Object" status. Suggest breaking it down into smaller, focused UseCases or Repositories.

## 2. Memory Management

- **Object Retention:** Ensure that long-lived objects (@Singleton) do not hold references to short-lived objects (Activities, Fragments, Views).
- **Large Data Sets:** Avoid keeping large lists or bitmaps in memory. Use paging or LRU caches with defined bounds.
- **Context Leaks:** Never pass a Context (especially an Activity Context) into a Repository or UseCase. If needed, inject the ApplicationContext.

## 3. Resource Checklist for Architects

- [ ] Is this new component @Singleton? If yes, why can't it be @ViewModelScoped or unscoped?
- [ ] Does this component hold a reference to a Context?
- [ ] Will this component's memory footprint grow over time (e.g., unbounded list)?
- [ ] Can this logic be moved from a long-lived Service to a short-lived Worker (WorkManager)?
