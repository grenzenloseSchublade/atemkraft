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

# Stil- und statische Sicherheits-Checks (sec-*, docs/SECURITY.md) laufen im selben Skript.
step "Stil- und Sicherheits-Checks (Selbsttest)"
scripts/check-style.sh --self-test
step "Stil- und Sicherheits-Checks"
scripts/check-style.sh

TASKS=(ktlintCheck testDebugUnitTest)
$FAST || TASKS+=(lintDebug)
step "Gradle: ${TASKS[*]}"
scripts/build.sh "${TASKS[@]}"

printf '\ncheck.sh: alles grün\n'
