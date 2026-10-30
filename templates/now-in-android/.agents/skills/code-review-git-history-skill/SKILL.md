---
name: code-review-git-history-skill
description: 'Review-time Git history reconstruction workflow. Use when: a reviewer asks when something changed, what the current diff means in Git terms, whether an area has been touched repeatedly, or needs a neutral classification of change shape without giving quality judgments.'
argument-hint: 'Describe the diff, branch, commit range, or files whose recent Git history should be reconstructed.'
user-invocable: false
---

# Code Review Git History Skill

## When to Use

- Reviewers ask when a behavior or shape changed
- The current diff needs Git-grounded context
- You need recent related commits and repeated touch patterns
- You need a neutral change-shape classification such as feature, refactor, workaround, cleanup, or mixed

## Boundaries

- Do not judge code quality or design quality
- Do not recommend fixes or review outcomes
- Do not over-infer intent from a single commit
- Keep review-time scope narrower than a full historical audit

## Procedure

### Step 1 - Define the Review Window

Prefer the narrowest useful slice:

- current diff
- specific files
- branch comparison
- recent commit window touching the changed area

### Step 2 - Prefer Deterministic Extraction First

Start with the review-time helper script:

```bash
python3 .github/policies/codereview/code-review-git-history.py --path core/services --limit 12
python3 .github/policies/codereview/code-review-git-history.py --base-ref main --head-ref hotfix/3.4.2 --path core/services
python3 .github/policies/codereview/code-review-git-history.py --path core/common --json
```

Use this helper for:

- current diff reconstruction in Git terms
- recent related commits for a review slice
- repeated touch patterns in the same area
- neutral change-shape classification from commit evidence

If review-time scope becomes too broad or needs deeper historical analysis, escalate to `git-forensics-skill` instead of stretching this workflow.

### Step 3 - Gather Git Evidence

Focus on:

- current diff shape
- nearby commits touching the same area
- repeated edits in the same files or folders
- rename or move signals when relevant
- commit clustering over time

For deeper or broader history beyond review-time needs, escalate to `git-forensics-skill` rather than expanding scope indefinitely.

### Step 4 - Classify the Change Shape Carefully

Use neutral labels only when the evidence supports them:

- feature
- refactor
- workaround
- cleanup
- configuration
- mixed

If the signal is weak, mark the classification as uncertain.

### Step 5 - Separate Fact from Inference

- Facts: commit counts, files touched, dates, repeated edits, rename markers
- Inference: likely intent or likely change type, with confidence noted

## Output Pattern

Use this structure:

```md
### Git Scope
### Diff Reconstruction
### Related History
### Evidence Notes
```
