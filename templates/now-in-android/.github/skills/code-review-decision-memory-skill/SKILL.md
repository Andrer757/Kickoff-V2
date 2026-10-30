---
name: code-review-decision-memory-skill
description: 'Historical decision-memory workflow for code review context. Use when: a reviewer wants prior rationale reconstructed from backlog notes, ACCs, architecture artifacts, PR-related files, or repository history, and needs a concise summary of what appears to have been decided before and why.'
argument-hint: 'Describe the area or decision context whose prior rationale should be reconstructed.'
user-invocable: false
---

# Code Review Decision Memory Skill

## When to Use

- A reviewer asks why a similar decision was made before
- Local backlog notes, ACCs, or architecture documents may contain rationale
- Prior decision context is relevant to understanding the current change

## Boundaries

- Do not force the current change to follow the historical decision
- Do not invent rationale when records do not exist
- Do not treat weak hints as confirmed history

## Procedure

### Step 1 - Search for Decision Artifacts

Prefer deterministic extraction first with the helper script:

```bash
python3 .github/policies/codereview/code-review-decision-memory.py --path core/services
python3 .github/policies/codereview/code-review-decision-memory.py --path core/services --query architecture
python3 .github/policies/codereview/code-review-decision-memory.py --path core/common --query review --json
```

Use the helper to search:

- backlog notes
- governance and ACC artifacts
- local prompts, agents, and skills
- confluence markdown stored in the repo
- focused Git subject hints for the selected paths

If deterministic extraction returns weak or ambiguous evidence, follow up with targeted reads of the top-scoring artifacts instead of broad repo exploration.

### Step 2 - Search for Decision Artifacts

Check the smallest relevant set of artifacts first:

- backlog notes
- ACC files or governance records
- architecture or runbook documents
- PR-related or review-related files
- focused Git history when the artifact trail is incomplete

### Step 3 - Separate Records from Hints

- Direct records: explicit notes, ACC entries, decision documents
- Indirect hints: commit messages, naming, repeated patterns, code comments

### Step 4 - Summarize Prior Rationale Carefully

State:

- what appears to have been decided
- why it appears to have been decided that way
- the confidence level based on evidence quality

### Step 5 - State Gaps Explicitly

If no reliable decision memory exists, say so clearly.

## Output Pattern

Use this structure:

```md
### Decision Artifacts Found
### Prior Rationale Summary
### Constraints or Tradeoffs Recorded
### Gaps
```
