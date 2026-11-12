#!/usr/bin/env python3
"""
android-acc-validator.py
Validates the Android ACC registry (acc-registry-android.yaml).
Checks: unique IDs, required fields, valid status values, AND-ACC ID format.

Usage:
    python3 .github/policies/android/android-acc-validator.py [--strict]

Exit codes:
  0 = passed (or warnings in non-strict mode)
  1 = failures found (strict mode)
"""

import sys
import json
import re
import argparse
from pathlib import Path

WORKSPACE_ROOT = Path(__file__).parent.parent.parent.parent
REGISTRY_PATH = WORKSPACE_ROOT / ".github/governance/android/acc-registry-android.yaml"
REPORT_PATH = WORKSPACE_ROOT / ".github/governance/android/reports/android-acc-validation-report.json"

REQUIRED_FIELDS = ["acc_id", "title", "scope", "date", "status", "author", "lead_approval", "summary"]
VALID_STATUSES = {"APPROVED", "PENDING", "REJECTED"}
VALID_SCOPES = {"STRUCTURAL", "LOCAL"}
ACC_ID_PATTERN = re.compile(r"^AND-ACC-\d{4}-\d{3}$")


def parse_yaml_entries(registry_path: Path) -> list[dict] | None:
    """Parse YAML entries for the ACC registry; return None if PyYAML is unavailable."""
    try:
        import yaml
        with open(registry_path) as f:
            data = yaml.safe_load(f) or {}
        return data.get("entries") or []
    except ImportError:
        print("  [WARN] pyyaml not available — cannot validate registry (install pyyaml for full validation)")
        return None


def validate_registry(entries: list[dict]) -> list[dict]:
    findings = []

    seen_ids = set()
    for i, entry in enumerate(entries):
        prefix = f"Entry[{i}]"

        acc_id = entry.get("acc_id", f"<missing at index {i}>")

        # Unique ID
        if acc_id in seen_ids:
            findings.append({"severity": "error", "acc_id": acc_id, "message": "Duplicate acc_id"})
        seen_ids.add(acc_id)

        # ID format
        if not ACC_ID_PATTERN.match(str(acc_id)):
            findings.append({"severity": "error", "acc_id": acc_id, "message": f"acc_id does not match AND-ACC-YYYY-NNN format: {acc_id}"})

        # Required fields
        for field in REQUIRED_FIELDS:
            if field not in entry or entry[field] is None:
                findings.append({"severity": "error", "acc_id": acc_id, "message": f"Missing required field: {field}"})

        # Valid status
        status = entry.get("status")
        if status and status not in VALID_STATUSES:
            findings.append({"severity": "error", "acc_id": acc_id, "message": f"Invalid status '{status}'. Must be one of {VALID_STATUSES}"})

        # Valid scope
        scope = entry.get("scope")
        if scope and scope not in VALID_SCOPES:
            findings.append({"severity": "error", "acc_id": acc_id, "message": f"Invalid scope '{scope}'. Must be STRUCTURAL or LOCAL"})

        # lead_approval must be APPROVED if status is APPROVED
        if status == "APPROVED" and entry.get("lead_approval") != "APPROVED":
            findings.append({"severity": "error", "acc_id": acc_id, "message": "status is APPROVED but lead_approval is not APPROVED"})

    return findings


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--strict", action="store_true", help="Exit 1 on any finding")
    args = parser.parse_args()

    print("=== Android ACC Validator ===")

    if not REGISTRY_PATH.exists():
        print(f"  [ERROR] Registry not found: {REGISTRY_PATH}")
        result = {"status": "error", "findings": [{"severity": "error", "message": f"Registry file not found: {REGISTRY_PATH}"}]}
        REPORT_PATH.parent.mkdir(parents=True, exist_ok=True)
        REPORT_PATH.write_text(json.dumps(result, indent=2))
        sys.exit(1 if args.strict else 0)

    entries = parse_yaml_entries(REGISTRY_PATH)
    if entries is None:
        result = {
            "platform": "android",
            "registry": str(REGISTRY_PATH.relative_to(WORKSPACE_ROOT)),
            "entries_checked": 0,
            "status": "not_run",
            "findings": [{"severity": "warning", "message": "PyYAML is not installed; ACC registry validation did not run"}]
        }
        REPORT_PATH.parent.mkdir(parents=True, exist_ok=True)
        REPORT_PATH.write_text(json.dumps(result, indent=2))
        print(f"  Status: NOT RUN | Errors: 0 | Report: {REPORT_PATH.relative_to(WORKSPACE_ROOT)}")
        sys.exit(1 if args.strict else 0)

    print(f"  Loaded {len(entries)} ACC entries")

    findings = validate_registry(entries)
    errors = [f for f in findings if f["severity"] == "error"]

    status = "passed" if not errors else "failed"
    result = {
        "platform": "android",
        "registry": str(REGISTRY_PATH.relative_to(WORKSPACE_ROOT)),
        "entries_checked": len(entries),
        "status": status,
        "findings": findings
    }

    REPORT_PATH.parent.mkdir(parents=True, exist_ok=True)
    REPORT_PATH.write_text(json.dumps(result, indent=2))

    if findings:
        for f in findings:
            level = f["severity"].upper()
            print(f"  [{level}] {f.get('acc_id', 'N/A')}: {f['message']}")
    else:
        print("  No findings.")

    print(f"  Status: {status.upper()} | Errors: {len(errors)} | Report: {REPORT_PATH.relative_to(WORKSPACE_ROOT)}")

    if args.strict and errors:
        sys.exit(1)
    sys.exit(0)


if __name__ == "__main__":
    main()
