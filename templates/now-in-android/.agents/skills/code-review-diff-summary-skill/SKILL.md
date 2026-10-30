---
name: code-review-diff-summary-skill
description: 'Diff summarization workflow for code review. Use when: a reviewer needs fast orientation on what really changed, which behavior is new versus modified, what remained untouched, and whether the diff has a few large clusters or many small edits.'
argument-hint: 'Describe the diff, PR, branch, or file set that should be summarized for review orientation.'
user-invocable: false
---

# Code Review Diff Summary Skill

## When to Use

- A reviewer asks what changed in plain language
- The diff is large enough that orientation cost matters
- The review needs separation of new versus modified behavior
- The review needs identification of large and small edit clusters

## Boundaries

- Do not review correctness, bugs, or design quality
- Do not speculate about behavior not supported by the diff and nearby context
- Do not recommend approval or rejection

## Procedure

### Step 1 - Identify the Change Surface

Prefer deterministic extraction first with the helper script:

```bash
python3 .github/policies/codereview/code-review-diff-summary.py --path core/services
python3 .github/policies/codereview/code-review-diff-summary.py --base-ref main --head-ref feature/review-pass --path core/services
python3 .github/policies/codereview/code-review-diff-summary.py --path core/common --json
```

Use the helper to extract:

- changed files and line counts
- new, deleted, renamed, and modified file buckets
- path clusters and isolated edits
- nearby untouched siblings under the selected paths when determinable

If the helper output is insufficient, augment it with one or two focused Git or file reads rather than broad exploration.

### Step 2 - Identify the Change Surface

Collect:

- changed files and modules
- main edit clusters
- proportion of structural versus behavioral edits

### Step 3 - Reframe the Diff

Describe in reviewer language:

- new behavior introduced
- existing behavior modified
- structural, naming, or non-behavioral edits

### Step 4 - Bound the Summary

Call out adjacent areas that are not touched only when the diff gives enough confidence.

### Step 5 - Report Shape, Not Judgment

Make the summary faster to read than the raw diff while staying neutral.

## Output Pattern

Use this structure:

```md
### Scope Summary
### What Changed
### What Did Not Change
### Diff Shape
```
