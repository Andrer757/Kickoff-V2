---
name: git-forensics-skill
description: 'Historical Git forensics workflow for evidence extraction from commits, branches, diffs, churn, and file movement. Use when: analyzing the last N commits touching a path, reconstructing why changes likely happened, identifying temporal clusters, hotspots, rename signals, or producing neutral historical context for audits, ACC support, or engineering investigation.'
argument-hint: 'Describe the branch, commit range, files or folders, and the evidence you want extracted.'
---

# Git Forensics Skill

## When to Use

- Analyze the last N commits touching a file, folder, or subsystem
- Reconstruct likely rationale behind a series of changes from Git evidence
- Identify churn hotspots, author concentration, rename signals, and time clustering
- Support architectural review, backlog context, audits, or post-incident historical analysis
- Help engineers understand what changed in the past without issuing a verdict

## Boundaries

- Do not issue architecture, quality, or governance verdicts
- Do not propose implementation solutions unless explicitly requested
- Do not assume intent when the evidence is weak or contradictory
- Keep direct observations separate from inferences

## Procedure

### Step 1 - Define the Scope

Establish the smallest useful analysis window first:

- Branch or ref to inspect
- Commit count, date range, or explicit commit window
- Files, folders, or subsystem paths
- Whether the user wants pattern extraction, chronology, rename tracking, or change rationale

If the request is vague, narrow it to a concrete slice before analyzing.

### Step 2 - Prefer Deterministic Extraction First

For requests like "analyze the last 20 commits touching services" or "show churn on this folder", start with the helper script:

```bash
python3 .github/policies/gitforensics/git-forensics-report.py --limit 20 --path core/services
```

Use the helper when the task is primarily:

- commit window summarization
- branch-to-branch comparison for a specific path or subsystem
- top changed files or directories
- author distribution
- rename and delete signals
- temporal clustering
- repeated commit-message patterns

Use `--json` when another agent, script, or governance artifact needs structured output instead of markdown.

### Step 3 - Expand with Native Git Only When Needed

If the helper output is insufficient, add focused Git inspection such as:

- `git log --follow -- <file>` for single-file lineage
- `git diff <ref1>..<ref2> -- <path>` for boundary comparison
- `git show <commit>` for a decisive commit
- `git blame <file>` only when authorship timing matters

Do not broaden scope without a reason.

### Step 4 - Separate Facts from Inference

Report observations first, then infer cautiously:

- Facts: commit counts, files touched, dates, authors, rename markers, repeated messages
- Inference: likely intent, possible migration, probable cleanup, suggests boundary drift

Always qualify inference with likely, possibly, or suggests, and assign confidence as High, Medium, or Low.

### Step 5 - Produce the Standard Report

Always use this structure:

```md
### Historical Context
- Time range analyzed
- Branches or commits involved
- Files or areas affected

### Change Summary
- High-level description of what changed
- Nature of changes: structural, behavioral, cosmetic, configuration, or mixed

### Detected Patterns
- Repeated changes
- Escalating or narrowing scope
- Areas of high churn or volatility
- Temporal clustering or discontinuity

### Inferred Intent (Evidence-Based)
- What the changes likely attempted to achieve
- Confidence: High, Medium, or Low
- Evidence supporting the inference

### Architectural or Governance Signals
- Potential boundaries crossed
- Contract or dependency implications
- Drift indicators without judgment

### Open Questions or Uncertainties
- Missing context
- Ambiguous signals
- Information required for higher-confidence interpretation
```

## Heuristics

- Rename-heavy windows often suggest refactors, reorganizations, or ownership shifts
- High author concentration may indicate specialized ownership or emergency correction work
- Bursts across adjacent dates may indicate a rollout, regression repair, or migration push
- Repeated edits in the same area with changing commit scope may suggest unstable boundaries
- Broad file spread with vague commit messages lowers inference confidence

## Recommended Helper Usage

Common invocations:

```bash
python3 .github/policies/gitforensics/git-forensics-report.py --limit 20 --path core/services
python3 .github/policies/gitforensics/git-forensics-report.py --limit 30 --ref main --path core/services
python3 .github/policies/gitforensics/git-forensics-report.py --limit 15 --since 2026-01-01 --path .github
python3 .github/policies/gitforensics/git-forensics-report.py --base-ref main --head-ref hotfix/3.4.2 --path core/services
python3 .github/policies/gitforensics/git-forensics-report.py --base-ref main --head-ref hotfix/3.4.2 --path core/services --json
```

If the helper produces enough evidence, summarize it directly. If not, augment with one or two precise Git commands rather than broad exploration.