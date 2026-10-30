#!/usr/bin/env python3
"""
android-governance-observability.py
Generates a weekly Markdown + JSON summary of Android governance health.

Reads:
  .github/governance/android/reports/*.json
  .github/governance/android/ops-metrics-android.json

Writes:
    .github/governance/android/reports/android-weekly-governance-summary.json
    .github/governance/android/reports/android-weekly-governance-summary.md

Usage:
    python3 .github/policies/android/android-governance-observability.py
"""

import json
from pathlib import Path
from datetime import date

WORKSPACE_ROOT = Path(__file__).parent.parent.parent.parent
REPORTS_DIR = WORKSPACE_ROOT / ".github/governance/android/reports"
OPS_METRICS_PATH = WORKSPACE_ROOT / ".github/governance/android/ops-metrics-android.json"
SUMMARY_JSON = REPORTS_DIR / "android-weekly-governance-summary.json"
SUMMARY_MD = REPORTS_DIR / "android-weekly-governance-summary.md"

THRESHOLDS = {
    "blocked_pr_rate": {"warning": 0.10, "rollback": 0.15},
    "false_positive_rate": {"warning": 0.05, "rollback": 0.10},
    "validator_crash_rate": {"warning": 0.02, "rollback": 0.05},
}


def load_json(path: Path) -> dict:
    if not path.exists():
        return {}
    try:
        return json.loads(path.read_text())
    except Exception:
        return {}


def load_ops_metrics() -> dict:
    metrics = load_json(OPS_METRICS_PATH)
    total = metrics.get("total_prs_last_7d", 0)
    blocked = metrics.get("prs_blocked_by_governance", 0)
    false_pos = metrics.get("false_positives_reported", 0)
    crashes = metrics.get("validator_crashes", 0)

    if total == 0:
        blocked_rate = 0.0
        fp_rate = 0.0
    else:
        blocked_rate = round(blocked / total, 4)
        fp_rate = round(false_pos / total, 4)

    crash_rate = round(crashes / max(total, 1), 4)

    return {
        "total_prs": total,
        "blocked_prs": blocked,
        "blocked_pr_rate": blocked_rate,
        "false_positives": false_pos,
        "false_positive_rate": fp_rate,
        "validator_crashes": crashes,
        "validator_crash_rate": crash_rate,
    }


def evaluate_threshold(value: float, metric: str) -> str:
    t = THRESHOLDS.get(metric, {})
    if value >= t.get("rollback", float("inf")):
        return "ROLLBACK"
    if value >= t.get("warning", float("inf")):
        return "WARNING"
    return "OK"


def collect_validator_results() -> dict:
    reports = {
        "acc_validator": load_json(REPORTS_DIR / "android-acc-validation-report.json"),
        "agent_consistency": load_json(REPORTS_DIR / "android-agent-consistency-report.json"),
        "skill_coverage": load_json(REPORTS_DIR / "android-skill-coverage-report.json"),
    }
    return {k: {"status": v.get("status", "unknown"), "findings": len(v.get("findings", []))} for k, v in reports.items()}


def main():
    today = date.today().isoformat()
    ops = load_ops_metrics()
    validators = collect_validator_results()

    threshold_results = {
        "blocked_pr_rate": evaluate_threshold(ops["blocked_pr_rate"], "blocked_pr_rate"),
        "false_positive_rate": evaluate_threshold(ops["false_positive_rate"], "false_positive_rate"),
        "validator_crash_rate": evaluate_threshold(ops["validator_crash_rate"], "validator_crash_rate"),
    }

    recommendation = "Keep Stage 1 (warning) active"
    if any(v == "ROLLBACK" for v in threshold_results.values()):
        recommendation = "ROLLBACK recommended — thresholds exceeded"
    elif any(v == "WARNING" for v in threshold_results.values()):
        recommendation = "Warning threshold approaching — monitor closely"

    summary = {
        "platform": "android",
        "week_of": today,
        "ops_metrics": ops,
        "threshold_evaluation": threshold_results,
        "validator_results": validators,
        "recommendation": recommendation,
    }

    REPORTS_DIR.mkdir(parents=True, exist_ok=True)
    SUMMARY_JSON.write_text(json.dumps(summary, indent=2))

    # Markdown summary
    md_lines = [
        f"# Android Weekly Governance Summary — {today}",
        "",
        "## Ops Metrics",
        f"| Metric | Value | Evaluation |",
        f"|--------|-------|-----------|",
        f"| Total PRs (7d) | {ops['total_prs']} | — |",
        f"| Blocked PRs | {ops['blocked_prs']} | — |",
        f"| **blocked_pr_rate** | {ops['blocked_pr_rate']} | {threshold_results['blocked_pr_rate']} |",
        f"| **false_positive_rate** | {ops['false_positive_rate']} | {threshold_results['false_positive_rate']} |",
        f"| **validator_crash_rate** | {ops['validator_crash_rate']} | {threshold_results['validator_crash_rate']} |",
        "",
        "## Validator Results",
        "| Validator | Status | Findings |",
        "|-----------|--------|---------|",
    ]
    for name, data in validators.items():
        md_lines.append(f"| {name} | {data['status'].upper()} | {data['findings']} |")

    md_lines += [
        "",
        f"## Recommendation",
        f"> {recommendation}",
        "",
        f"_Generated by android-governance-observability.py on {today}_",
    ]

    SUMMARY_MD.write_text("\n".join(md_lines))

    print(f"=== Android Weekly Governance Summary ===")
    print(f"  Week of: {today}")
    print(f"  Recommendation: {recommendation}")
    print(f"  Reports written to {REPORTS_DIR.relative_to(WORKSPACE_ROOT)}/")


if __name__ == "__main__":
    main()
