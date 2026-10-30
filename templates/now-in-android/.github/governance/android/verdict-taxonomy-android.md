# Android Governance Verdict Taxonomy

## Purpose
Define a single decision vocabulary for Android governance artifacts so verdict mapping remains deterministic and auditable.

## Canonical Verdict Layers

### Lead Final Decision Layer
- APPROVED: All required gates completed and acceptable.
- REJECTED: At least one required gate returned INSUFFICIENT.
- BLOCKED: Mandatory input or decision context is missing/ambiguous.

### Gate Verdict Layer (AndroidArchitectAgent and AndroidTestReviewer)
- ACCEPTABLE: Gate criteria are satisfied.
- INSUFFICIENT: Gate criteria are not satisfied.

## Mandatory Mapping Rules
- Gate mapping rule:
  - Any gate INSUFFICIENT → Lead decision REJECTED.
  - All required gates ACCEPTABLE → Lead decision APPROVED.
- Missing ACC or unresolved ambiguity → Lead decision BLOCKED.

## ACC Scope Vocabulary
- STRUCTURAL: Change affects ≥2 modules, adds cross-repo dependencies, modifies Hilt graph or Gradle topology, or toggles local-vs-remote dependency.
- LOCAL: Change confined to a single Gradle module with no cross-module or settings.gradle impact.

## Usage Requirements
- All files under `.github/backlog/Android/` must use canonical terms for new entries.
- Any agent or skill updates must preserve this taxonomy.
- If both gate and lead outcomes are recorded in the same entry, include both explicitly.

## Example
- Gate Verdict (Architecture): ACCEPTABLE
- Gate Verdict (Test Quality): ACCEPTABLE
- Lead Final Decision: APPROVED
