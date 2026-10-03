#!/usr/bin/env bash
# Einziger Einstieg für alle Prüfungen, lokal und im CI (docs/STYLEGUIDE.md §13.1).
#
#   scripts/check.sh          # Stil-Checks, Sicherheits-Checks, ktlint, Unit-Tests, Android Lint
#   scripts/check.sh --fast   # ohne Android Lint (z. B. als pre-push-Hook)
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${ROOT}"

FAST=false
[[ "${1:-}" == "--fast" ]] && FAST=true

step() { printf '\n== %s ==\n' "$1"; }

step "Stil-Checks (Selbsttest)"
scripts/check-style.sh --self-test
step "Stil-Checks"
scripts/check-style.sh

# Sicherheits-Checks laut docs/SECURITY.md, sobald das Skript existiert.
if [[ -x scripts/check-security.sh ]]; then
  step "Sicherheits-Checks"
  scripts/check-security.sh
fi

TASKS=(ktlintCheck testDebugUnitTest)
$FAST || TASKS+=(lintDebug)
step "Gradle: ${TASKS[*]}"
scripts/build.sh "${TASKS[@]}"

printf '\ncheck.sh: alles grün\n'
