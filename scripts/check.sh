#!/usr/bin/env bash
# Einziger Einstieg für alle Prüfungen, lokal und im CI (docs/STYLEGUIDE.md §13.1).
#
#   scripts/check.sh          # Stil-Checks, Sicherheits-Checks, ktlint, Unit-Tests, Android Lint,
#                             # Release-Build (unsigniert)
#   scripts/check.sh --fast   # ohne Android Lint und Release-Build
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

# Der Release-Build prüft, was nur dort läuft: R8 samt Keep-Regeln, Ressourcen-Shrinking,
# lintVitalRelease und das Paketieren. Ob die neuronale Stimme über JNI zur Laufzeit geht, prüft
# er nicht (das bleibt die Gerätesprobe der Release-Checkliste). Er bleibt immer unsigniert,
# auch wenn lokal ein Release-Keystore liegt.
TASKS=(ktlintCheck testDebugUnitTest)
$FAST || TASKS+=(lintDebug assembleRelease -PunsignedRelease=true)
step "Gradle: ${TASKS[*]}"
scripts/build.sh "${TASKS[@]}"

printf '\ncheck.sh: alles grün\n'
