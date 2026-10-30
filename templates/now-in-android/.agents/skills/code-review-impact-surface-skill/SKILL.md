---
name: code-review-impact-surface-skill
description: 'Impact surface mapping workflow for code review. Use when: a reviewer wants to know what modules, layers, components, dependencies, or historically sensitive areas could be affected by the change without turning that into solution advice or prioritization.'
argument-hint: 'Describe the files, modules, or diff whose impact surface should be mapped.'
user-invocable: false
---

# Code Review Impact Surface Skill

## When to Use

- A reviewer asks what the change could affect
- A diff crosses module or layer boundaries
- Nearby dependencies or consumers matter to the review
- You need a neutral map of direct and indirect exposure

## Boundaries

- Do not prescribe mitigations or fixes
- Do not assign severity or review verdicts
- Do not claim runtime impact without evidence from dependencies, interfaces, or usage patterns

## Procedure

### Step 1 - Define the Direct Change Surface

Prefer deterministic extraction first with the helper script:

```bash
python3 .github/policies/codereview/code-review-impact-surface.py --path core/services
python3 .github/policies/codereview/code-review-impact-surface.py --base-ref main --head-ref feature/review-pass --path core/services
python3 .github/policies/codereview/code-review-impact-surface.py --path core/common --json
```

Use the helper for:

- direct changed-path clustering
- adjacent sibling areas under the same parent slices
- simple reference-hit discovery across nearby text code files
- lightweight historical sensitivity hints for the changed surface

Treat the helper output as a first-pass map. If the change involves runtime coupling or semantic dependencies that the script cannot infer, expand with focused file reads.

### Step 2 - Define the Direct Change Surface

List the files, modules, layers, and components directly changed.

### Step 3 - Map Direct Connections

Look for:

- immediate consumers and providers
- imports or references crossing layers
- contracts, interfaces, or shared models near the edit

### Step 4 - Expand One Hop Only

Identify plausible secondary surfaces without turning the analysis into open-ended exploration.

### Step 5 - Add Historical Sensitivity Signals

If repository evidence shows nearby hotspots or repeated touches, include them as context only.

## Output Pattern

Use this structure:

```md
### Changed Surface
### Directly Connected Areas
### Potentially Affected Areas
### Historical Sensitivity Signals
```
