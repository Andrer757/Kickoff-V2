---
name: code-review-lead-orchestration-skill
description: 'Evidence-based orchestration workflow for multi-agent code review. Use when: normalizing review requests, routing review questions to specialist agents, assembling a neutral review package, reducing reviewer cognitive load, and keeping human approval decisions outside the agent boundary.'
argument-hint: 'Describe the review scope, the repositories or files involved, and the signals needed such as diff summary, impact surface, assumptions, decision memory, architecture, testing, or history.'
user-invocable: false
---

# Code Review Lead Orchestration Skill

## When to Use

- A user asks for a code review, PR review, or review of current changes
- A review request needs decomposition into multiple specialist analyses
- The reviewer needs a neutral evidence bundle rather than a final decision
- The code review spans multiple repositories, modules, or platforms

## Boundaries

- Do not approve, reject, or sign off changes
- Do not perform implementation work or edit files as part of the review process
- Do not replace specialist findings with personal preference
- Do not collapse uncertainty into false confidence

## Procedure

### Step 1 - Normalize Intent

Translate the user request into:

- objective: structured code review support
- scope: diff, branch, PR, file set, or module slice
- outcome: review-ready evidence bundle
- explicit boundary: no final human approval decision

If scope is incomplete, ask only the smallest question needed to route the work.

### Step 2 - Select the Smallest Useful Specialist Set

Pick specialists by question type:

- `CodeReviewDiffSummaryAgent` for reviewer orientation
- `CodeReviewGitHistoryAgent` for what changed, when, and repeated touch patterns
- `CodeReviewImpactSurfaceAgent` for dependency and adjacency mapping
- `CodeReviewAssumptionTrackerAgent` for implicit assumptions
- `CodeReviewDecisionMemoryAgent` for prior rationale from artifacts
- `AndroidArchitectAgent` for architecture-specific risk
- `AndroidTestReviewer` for test robustness and validation quality
- `GitForensicsAgent` only when deeper historical reconstruction is needed beyond review-time context

Avoid over-delegation. Use only the analyses that answer the request.

### Step 3 - Delegate with Tight Contracts

For each sub-agent, include:

- exact scope under review
- explicit question to answer
- type of evidence expected
- reminder to remain neutral and identify uncertainty

Prefer parallel delegation when the requested analyses are independent.

### Step 4 - Assemble the Evidence Bundle

Keep these streams separate:

- diff summary
- impact surface
- assumption inventory
- decision memory
- architecture findings
- testing findings
- Git and history signals

Do not merge them into a single undifferentiated narrative.

### Step 5 - Produce Reviewer Handoff

Return a package that lets a human reviewer act quickly:

- what changed
- what could be affected
- what is being assumed
- what prior context exists
- what remains uncertain

End with an explicit statement that final judgment remains with the human reviewer.

## Output Pattern

Use this structure:

```md
## Intent Normalization

## Review Plan

## Evidence Bundle

## Reviewer Handoff
```
