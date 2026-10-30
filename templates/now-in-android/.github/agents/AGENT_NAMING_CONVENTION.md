# Agent Naming Convention

This convention is generic and intended to survive beyond the code review workflow.

## Goals

- Make agent names predictable in the agent picker and in subagent delegation
- Separate orchestration roles from specialist roles
- Keep names stable enough for reuse across domains

## Naming Rules

### 1. Orchestrators

Use this pattern for coordinating agents with delegation authority:

`Lead<Scope>Agent`

Examples:

- `LeadAndroidAgent`
- `LeadIOSAgent`
- `LeadCodeReviewAgent`

Use `Lead` only for agents whose primary job is orchestration, sequencing, routing, or final synthesis.

### 2. Domain Specialists

Use this pattern for specialists attached to a specific workflow or domain:

`<Scope><Capability>Agent`

Examples:

- `CodeReviewDiffSummaryAgent`
- `CodeReviewGitHistoryAgent`
- `CodeReviewImpactSurfaceAgent`
- `CodeReviewAssumptionTrackerAgent`
- `CodeReviewDecisionMemoryAgent`

Use a domain prefix when the capability is specialized by workflow, vocabulary, or output contract.

### 3. Cross-Domain Generic Specialists

Use this pattern for specialists that are reusable across multiple workflows without a domain prefix:

`<Capability>Agent`

Examples:

- `GitForensicsAgent`
- `ConfluenceAgent`
- `Explore`

Keep the name generic only when the agent can be reused outside a single workflow without ambiguity.

## Selection Guidance

- If the agent coordinates other agents, start with `Lead`.
- If the agent is primarily tied to one workflow, prefix it with that workflow scope.
- If the agent is broadly reusable, keep the name capability-first and generic.

## File Naming

- File name should match the agent name exactly, followed by `.agent.md`.
- The `name:` field in frontmatter should match the file name stem exactly.

Examples:

- `LeadCodeReviewAgent.agent.md` -> `name: LeadCodeReviewAgent`
- `CodeReviewImpactSurfaceAgent.agent.md` -> `name: CodeReviewImpactSurfaceAgent`

## Compatibility Note

Existing older agents do not need to be renamed immediately if they are already in active use.
Apply this convention to new agents first, then rename older agents only when a workflow is being actively cleaned up.
