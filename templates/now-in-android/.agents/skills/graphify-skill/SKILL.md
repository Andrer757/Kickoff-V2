---
name: graphify-skill
description: >
  Graph-assisted codebase navigation for the Android multi-module project.
  Use when exploring unknown modules, tracing dependencies between modules, validating
  architectural boundaries, or mapping change impact — especially when graphify-out/
  exists in the target workspace. Avoids exhaustive file searches by querying the
  prebuilt knowledge graph instead.
argument-hint: >
  Describe what you need to find or trace: module name, class, dependency path,
  or architectural boundary to validate.
---

# Graphify — Graph-Assisted Navigation Skill

## When to Load This Skill

Load this skill when:
- Exploring an unfamiliar module before writing code
- Tracing how a change in one module could affect another
- Validating Clean Architecture boundaries or dependency direction
- Assessing change impact before issuing an ACC verdict
- Navigating the multi-module dependency graph (`:app`, `:feature:*`, `:core:*`, `:sdk-*`)

Do NOT load this skill when:
- `graphify-out/` does not exist in the current workspace (fall back to standard file search)
- Modifying a specific file whose location is already known
- Running tests or builds

---

## Step 0 — Detect Graph Availability

Before using any graph commands, check if the graph exists by attempting to read the report file:

```
read_file: graphify-out/GRAPH_REPORT.md
```

If the file does not exist: **stop here**. Do not use any graph commands from this skill.
Fall back to normal navigation: use `grep_search`, `file_search`, and `read_file` directly on source files as you would without this skill.

If the file exists: proceed with graph-first navigation below.

---

## Step 1 — Read the Report (or Wiki)

**Check for wiki first** — if the wiki was built, it provides deeper structured navigation:

```
read_file: graphify-out/wiki/index.md   ← prefer this if it exists
read_file: graphify-out/GRAPH_REPORT.md ← fallback if wiki does not exist
```

The report/wiki gives you:
- **God nodes** — most-connected classes/modules. Changes here have broad impact.
- **Surprising connections** — unexpected links between modules in different layers.
- **Suggested questions** — 4–5 questions the graph is uniquely positioned to answer.

---

## Step 2 — Graph Navigation Commands

### Find dependencies between two components

```
/graphify path ":feature:home" ":app"
/graphify path "UserRepository" "ApiService"
```

### Understand a specific component

```
/graphify explain "OfflineFirstUserRepository"
/graphify explain "HomeViewModel"
```

### Broad dependency queries

```
/graphify query "what connects feature modules to the database?"
/graphify query "which modules depend on :core:model?"
/graphify query "show the full dependency chain for :feature:home"
```

### BFS traversal for impact analysis

```
/graphify query "what would be affected if I change UserRepository?" --dfs --budget 1500
```

---

## Step 3 — Update the Graph After Changes

If `graphify-out/` is stale (files changed since last build), ask the user to run the update command in an assistant that has the `/graphify` command installed:

```
/graphify . --update
```

Agents do not trigger graph updates themselves — always delegate this to the user.

---

## Step 4 — Commit the Graph

If the team tracks `graphify-out/`, commit `GRAPH_REPORT.md`, `graph.json`, and `graph.html` so all team members share the same map.
