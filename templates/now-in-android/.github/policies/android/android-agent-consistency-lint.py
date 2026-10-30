#!/usr/bin/env python3
"""
android-agent-consistency-lint.py
Lints Android agent markdown files for required sections and contracts.

Checks for:
  - Mandatory Skill Loading section referencing android-architecture-core-skill
  - Core Identity section
  - ACC Input Contract (AndroidProgrammer)
  - Verdict protocol (ACCEPTABLE/INSUFFICIENT for gate agents)
  - Non-delegable authority section (LeadAndroidAgent)

Usage:
    python3 .github/policies/android/android-agent-consistency-lint.py [--strict] [--all-agents]

Exit codes:
  0 = passed or warnings
  1 = failures found (strict mode)
"""

import sys
import json
import argparse
from pathlib import Path

WORKSPACE_ROOT = Path(__file__).parent.parent.parent.parent
CANDIDATE_AGENT_DIRS = [
    WORKSPACE_ROOT / ".agents/agents",
    WORKSPACE_ROOT / ".claude/agents",
    WORKSPACE_ROOT / ".github/agents",
]
AGENTS_DIR = next((d for d in CANDIDATE_AGENT_DIRS if d.exists()), CANDIDATE_AGENT_DIRS[0])
REPORT_PATH = WORKSPACE_ROOT / ".github/governance/android/reports/android-agent-consistency-report.json"

ANDROID_AGENT_NAMES = {
    "LeadAndroidAgent",
    "AndroidArchitectAgent",
    "AndroidProgrammer",
    "AndroidTestReviewer",
}

UNIVERSAL_CHECKS = {
    "android-architecture-core-skill": "Must reference android-architecture-core-skill in skill loading",
    "Core Identity": "Must have a Core Identity section",
}

AGENT_SPECIFIC_CHECKS = {
    "AndroidProgrammer": {
        "ACC Input Contract": "Must have ACC Input Contract section",
        "android-implementation-skill": "Must reference android-implementation-skill",
    },
    "LeadAndroidAgent": {
        "Non-Delegable": "Must have Non-Delegable Authorities section",
        "ACC Reference Contract": "Must have ACC Reference Contract section",
    },
    "AndroidArchitectAgent": {
        "ACCEPTABLE": "Must mention ACCEPTABLE verdict",
        "INSUFFICIENT": "Must mention INSUFFICIENT verdict",
        "Authority": "Must have Authority section",
    },
    "AndroidTestReviewer": {
        "ACCEPTABLE": "Must mention ACCEPTABLE verdict",
        "INSUFFICIENT": "Must mention INSUFFICIENT verdict",
        "android-test-chaos-skill": "Must reference android-test-chaos-skill",
    },
}


def agent_base_name(path: Path) -> str:
    name = path.name
    if name.endswith(".agent.md"):
        return name[:-len(".agent.md")]
    if name.endswith(".md"):
        return name[:-len(".md")]
    return path.stem


def lint_agent(agent_path: Path) -> list[dict]:
    findings = []
    name = agent_base_name(agent_path)
    content = agent_path.read_text()

    for keyword, message in UNIVERSAL_CHECKS.items():
        if keyword not in content:
            findings.append({"severity": "error", "agent": name, "check": keyword, "message": message})

    for check_keyword, check_message in AGENT_SPECIFIC_CHECKS.get(name, {}).items():
        if check_keyword not in content:
            findings.append({"severity": "error", "agent": name, "check": check_keyword, "message": check_message})

    return findings


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--strict", action="store_true", help="Exit 1 on any finding")
    parser.add_argument("--all-agents", action="store_true", help="Lint all agent files, not just Android ones")
    args = parser.parse_args()

    print("=== Android Agent Consistency Lint ===")

    agent_files = list(AGENTS_DIR.glob("*.md"))
    if not args.all_agents:
        agent_files = [f for f in agent_files if agent_base_name(f) in ANDROID_AGENT_NAMES]

    print(f"  Linting {len(agent_files)} agent(s) — scope={'all' if args.all_agents else 'android-only'}")

    all_findings = []
    for agent_file in sorted(agent_files):
        findings = lint_agent(agent_file)
        all_findings.extend(findings)

    errors = [f for f in all_findings if f["severity"] == "error"]
    status = "passed" if not errors else "failed"

    result = {
        "platform": "android",
        "lint_scope": "all" if args.all_agents else "android-only",
        "agents_checked": [agent_base_name(f) for f in sorted(agent_files)],
        "status": status,
        "findings": all_findings,
    }

    REPORT_PATH.parent.mkdir(parents=True, exist_ok=True)
    REPORT_PATH.write_text(json.dumps(result, indent=2))

    if all_findings:
        for f in all_findings:
            level = f["severity"].upper()
            print(f"  [{level}] {f['agent']}: {f['message']}")
    else:
        print("  No findings.")

    print(f"  Status: {status.upper()} | Errors: {len(errors)} | Report: {REPORT_PATH.relative_to(WORKSPACE_ROOT)}")

    if args.strict and errors:
        sys.exit(1)
    sys.exit(0)


if __name__ == "__main__":
    main()
