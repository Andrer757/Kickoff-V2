#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TMP_DIR="$(mktemp -d "${TMPDIR:-/tmp}/kickoff-verify-XXXXXX")"
trap 'rm -rf "$TMP_DIR"' EXIT

cd "$ROOT_DIR"

echo "=== Building Quality Verifier & Kickoff CLI ==="
./gradlew :quality-verifier:publishToMavenLocal :kickoff-cli:installDist --no-daemon

APP_DIR="$TMP_DIR/verify-app"
SDK_DIR="$TMP_DIR/verify-sdk"

echo "=== Scaffolding & Verifying Android App (50+ char package + Day-2) ==="
./kickoff new \
  --name "Verify App" \
  --package "com.example.verylongpackagenamefortesting.kickoffverify" \
  --output "$APP_DIR" \
  --type app \
  --ai all \
  --no-git \
  --json

./kickoff add feature --name settings -d "$APP_DIR" --json
./kickoff add entity --name AuditNote --fields "id:String,count:Int,active:Boolean" -d "$APP_DIR" --json
./kickoff arch-check -d "$APP_DIR"

(
  cd "$APP_DIR"
  ./gradlew check --no-daemon --max-workers=2
)

echo "=== Scaffolding & Verifying Android SDK (--type sdk + Day-2) ==="
./kickoff new \
  --name "Verify SDK" \
  --package "com.example.verifysdk" \
  --output "$SDK_DIR" \
  --type sdk \
  --ai all \
  --no-git \
  --json

./kickoff add module --name telemetry --type sdk -d "$SDK_DIR" --json
./kickoff arch-check -d "$SDK_DIR"

(
  cd "$SDK_DIR"
  ./gradlew check --no-daemon --max-workers=2
)

echo "=== All scaffold checks passed! ==="
