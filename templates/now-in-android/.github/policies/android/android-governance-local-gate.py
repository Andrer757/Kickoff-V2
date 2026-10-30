#!/usr/bin/env python3
"""
android-governance-local-gate.py
Developer pre-commit gate for Android governance.
Runs all 3 Android validators and exits 0 (safe) or 1 (block).

Usage:
    python3 .github/policies/android/android-governance-local-gate.py

Exit 0 = safe to commit
Exit 1 = block commit, fix findings first
"""

import subprocess
import sys
from pathlib import Path

WORKSPACE_ROOT = Path(__file__).parent.parent.parent.parent
PYTHON = sys.executable

VALIDATORS = [
    (".github/policies/android/android-acc-validator.py", "Android ACC Validator"),
    (".github/policies/android/android-agent-consistency-lint.py", "Android Agent Consistency Lint"),
    (".github/policies/android/android-skill-coverage-check.py", "Android Skill Coverage Check"),
]

OBSERVABILITY_SCRIPT = ".github/policies/android/android-governance-observability.py"


def run_validator(script: str, name: str) -> bool:
    """Returns True if passed, False if failed."""
    result = subprocess.run(
        [PYTHON, str(WORKSPACE_ROOT / script), "--strict"],
        capture_output=True,
        text=True,
        cwd=WORKSPACE_ROOT,
    )
    status = "PASS" if result.returncode == 0 else "FAIL"
    print(f"  [{status}] {name}")
    if result.stdout.strip():
        for line in result.stdout.strip().splitlines():
            print(f"         {line}")
    if result.returncode != 0 and result.stderr.strip():
        for line in result.stderr.strip().splitlines():
            print(f"         {line}")
    return result.returncode == 0


def main():
    print("=" * 50)
    print("  Android Governance Local Gate")
    print("=" * 50)

    all_passed = True
    for script, name in VALIDATORS:
        passed = run_validator(script, name)
        if not passed:
            all_passed = False

    observability = subprocess.run(
        [PYTHON, str(WORKSPACE_ROOT / OBSERVABILITY_SCRIPT)],
        capture_output=True,
        text=True,
        cwd=WORKSPACE_ROOT,
    )
    print("  [INFO] Android Governance Observability")
    if observability.stdout.strip():
        for line in observability.stdout.strip().splitlines():
            print(f"         {line}")
    if observability.returncode != 0 and observability.stderr.strip():
        for line in observability.stderr.strip().splitlines():
            print(f"         {line}")

    print("=" * 50)
    if all_passed:
        print("  ✓ All checks passed — safe to commit")
        sys.exit(0)
    else:
        print("  ✗ Findings detected — fix before committing")
        print("    Run individual validators with --strict for details")
        sys.exit(1)


if __name__ == "__main__":
    main()
