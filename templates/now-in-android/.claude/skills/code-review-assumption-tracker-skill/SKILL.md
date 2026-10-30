---
name: code-review-assumption-tracker-skill
description: 'Implicit assumption extraction workflow for code review. Use when: a reviewer wants hidden assumptions listed neutrally, such as non-empty data, guaranteed order, prior initialization, synchronization expectations, environmental preconditions, or lifecycle timing dependencies.'
argument-hint: 'Describe the diff, code path, or files where hidden assumptions should be extracted.'
user-invocable: false
---

# Code Review Assumption Tracker Skill

## When to Use

- A reviewer asks what the code is assuming silently
- The changed path depends on ordering, prior setup, timing, or environment state
- The code has implicit contracts that are not stated explicitly

## Boundaries

- Do not judge assumptions as correct or incorrect
- Do not convert assumptions into mandatory fixes
- Do not infer assumptions that the code path does not support

## Procedure

### Step 1 - Read the Smallest Relevant Control Path

Prefer deterministic extraction first with the helper script:

```bash
python3 .github/policies/codereview/code-review-assumption-tracker.py --path core/common
python3 .github/policies/codereview/code-review-assumption-tracker.py --base-ref main --head-ref feature/review-pass --path core/services --context-lines 3
python3 .github/policies/codereview/code-review-assumption-tracker.py --path core/services --json
```

Use the helper for a first-pass assumption inventory around changed lines and nearby context.

Explicit heuristic limits:

- inspect at most `12` changed files
- capture at most `6` matches per assumption category
- capture at most `24` total heuristic matches
- inspect `0` to `5` nearby context lines around changed lines

The runner is intentionally bounded and heuristic. If the result is thin or ambiguous, follow up with targeted file reads on the strongest evidence anchors rather than broad exploration.

### Step 2 - Read the Smallest Relevant Control Path

Inspect only the diff and the minimum nearby context required to understand the path.

### Step 3 - Extract Assumption Categories

Look for assumptions about:

- data shape and emptiness
- ordering or sequencing
- initialization and lifecycle state
- timing, concurrency, or synchronization
- environment, configuration, or side effects

### Step 4 - Distinguish Explicit Contracts from Hidden Assumptions

Only list assumptions that are relied on but not stated as explicit guards or contracts.

### Step 5 - Phrase Neutrally

Use clear, non-judgmental language and state uncertainty where context is incomplete.

## Output Pattern

Use this structure:

```md
### Assumption Inventory
### Evidence Anchors
### Uncertainties
```
