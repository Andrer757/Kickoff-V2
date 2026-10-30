#!/usr/bin/env python3
"""
android-skill-coverage-check.py
Checks Android skills for canonical content alignment.

Checks:
  - android-architecture-core-skill: must list module groups (:app, :feature, :core), coroutine rules, DI rules, invariants
  - android-implementation-skill: must reference Fakes, runTest, StateFlow
  - android-test-chaos-skill: must reference turbine, runTest, ACCEPTABLE/INSUFFICIENT
  - android-architect-validation-skill: must reference LOCAL/STRUCTURAL, ACC template
  - android-lead-orchestration-skill: must reference state machine, ACC policy

Usage:
    python3 .github/policies/android/android-skill-coverage-check.py [--strict]

Exit codes:
  0 = passed or warnings
  1 = failures (strict mode)
"""

import sys
import json
import argparse
from pathlib import Path

WORKSPACE_ROOT = Path(__file__).parent.parent.parent.parent
CANDIDATE_SKILL_DIRS = [
    WORKSPACE_ROOT / ".agents/skills",
    WORKSPACE_ROOT / ".claude/skills",
    WORKSPACE_ROOT / ".github/skills",
]
SKILLS_DIR = next((d for d in CANDIDATE_SKILL_DIRS if d.exists()), CANDIDATE_SKILL_DIRS[0])
REPORT_PATH = WORKSPACE_ROOT / ".github/governance/android/reports/android-skill-coverage-report.json"

SKILL_CHECKS = {
    "android-architecture-core-skill": [
        (":app", "Must list :app module"),
        (":feature", "Must list :feature modules"),
        (":core", "Must list :core modules"),
        ("GlobalScope", "Must mention GlobalScope prohibition"),
        ("Hilt", "Must mention Hilt/DI rules"),
        ("StateFlow", "Must mention StateFlow contract"),
        ("runBlocking", "Must mention runBlocking prohibition"),
        ("Non-Negotiable", "Must have Non-Negotiable Invariants section"),
    ],
    "android-implementation-skill": [
        ("Fake", "Must reference Fake pattern"),
        ("runTest", "Must reference runTest"),
        ("StateFlow", "Must reference StateFlow"),
        ("@HiltViewModel", "Must reference @HiltViewModel"),
        ("turbine", "Must reference turbine for Flow testing"),
        ("ACC Input Contract", "Must reference ACC Input Contract"),
    ],
    "android-test-chaos-skill": [
        ("ACCEPTABLE", "Must define ACCEPTABLE verdict"),
        ("INSUFFICIENT", "Must define INSUFFICIENT verdict"),
        ("turbine", "Must reference turbine"),
        ("runTest", "Must reference runTest"),
        ("Thread.sleep", "Must flag Thread.sleep as INSUFFICIENT"),
        ("GlobalScope", "Must flag GlobalScope"),
        ("Adversarial", "Must have adversarial scenarios section"),
    ],
    "android-architect-validation-skill": [
        ("LOCAL", "Must define LOCAL scope"),
        ("STRUCTURAL", "Must define STRUCTURAL scope"),
        ("acc_id", "Must reference acc_id in ACC template"),
        ("AND-ACC-", "Must use AND-ACC- ID format"),
        ("Hilt", "Must reference Hilt/DI review"),
    ],
    "android-lead-orchestration-skill": [
        ("IntentDefined", "Must reference state machine IntentDefined state"),
        ("LeadFinalized", "Must reference LeadFinalized state"),
        ("APPROVED", "Must reference APPROVED verdict"),
        ("BLOCKED", "Must reference BLOCKED verdict"),
        ("ACC Reference Contract", "Must have ACC Reference Contract or equivalent"),
    ],
    "android-coroutines-flows-skill": [
        ("StateFlow", "Must reference StateFlow"),
        ("Flow", "Must reference Flow"),
        ("runTest", "Must reference runTest"),
        ("Turbine", "Must reference Turbine"),
        ("Dispatcher", "Must reference Dispatcher injection"),
    ],
    "android-compose-ui-skill": [
        ("Composable", "Must reference @Composable"),
        ("State Hoisting", "Must reference state hoisting pattern"),
        ("performance", "Must reference performance optimizations"),
    ],
    "android-compose-performance-audit-skill": [
        ("recomposition", "Must reference recomposition"),
        ("performance", "Must reference performance audit workflow"),
        ("Audit", "Must define audit criteria"),
    ],
    "android-testing-strategies-skill": [
        ("JUnit", "Must reference JUnit"),
        ("Hilt", "Must reference Hilt test injection"),
        ("Unit Test", "Must reference unit tests"),
    ],
}


def check_skill(skill_name: str, checks: list[tuple]) -> list[dict]:
    findings = []
    skill_path = SKILLS_DIR / skill_name / "SKILL.md"

    if not skill_path.exists():
        findings.append({"severity": "error", "skill": skill_name, "check": "file_exists", "message": f"SKILL.md not found at {skill_path.relative_to(WORKSPACE_ROOT)}"})
        return findings

    content = skill_path.read_text()
    for keyword, message in checks:
        if keyword not in content:
            findings.append({"severity": "error", "skill": skill_name, "check": keyword, "message": message})

    return findings


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--strict", action="store_true", help="Exit 1 on any finding")
    args = parser.parse_args()

    print("=== Android Skill Coverage Check ===")

    all_findings = []
    for skill_name, checks in SKILL_CHECKS.items():
        findings = check_skill(skill_name, checks)
        all_findings.extend(findings)

    errors = [f for f in all_findings if f["severity"] == "error"]
    status = "passed" if not errors else "failed"

    result = {
        "platform": "android",
        "skills_checked": list(SKILL_CHECKS.keys()),
        "status": status,
        "findings": all_findings,
    }

    REPORT_PATH.parent.mkdir(parents=True, exist_ok=True)
    REPORT_PATH.write_text(json.dumps(result, indent=2))

    if all_findings:
        for f in all_findings:
            level = f["severity"].upper()
            print(f"  [{level}] {f['skill']}: {f['message']}")
    else:
        print("  No findings.")

    print(f"  Status: {status.upper()} | Errors: {len(errors)} | Report: {REPORT_PATH.relative_to(WORKSPACE_ROOT)}")

    if args.strict and errors:
        sys.exit(1)
    sys.exit(0)


if __name__ == "__main__":
    main()
