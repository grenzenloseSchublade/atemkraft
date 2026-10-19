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
# auch wenn lokal ein Release-Keystore liegt, und lässt eine signierte APK liegen (siehe unten).
TASKS=(ktlintCheck testDebugUnitTest)
$FAST || TASKS+=(lintDebug assembleRelease -PunsignedRelease=true)

# Das Paketieren räumt das Ausgabeverzeichnis des Release-Builds; eine dort liegende signierte
# app-release.apk (scripts/build.sh assembleRelease mit Keystore) wäre danach weg. Deshalb wird
# das Verzeichnis vorher beiseitegelegt und am Ende – auch bei Fehlern – unverändert
# zurückgestellt; die unsignierte Prüf-APK wird verworfen.
RELEASE_OUT="app/build/outputs/apk/release"
RELEASE_KEEP="app/build/check-release-keep"
restore_release() {
  [[ -d "${RELEASE_KEEP}" ]] || return 0
  rm -rf "${RELEASE_OUT}"
  mv "${RELEASE_KEEP}" "${RELEASE_OUT}"
  echo "check.sh: signierte Release-APK in ${RELEASE_OUT} wiederhergestellt"
}
if ! $FAST && [[ -f "${RELEASE_OUT}/app-release.apk" ]]; then
  rm -rf "${RELEASE_KEEP}"
  mv "${RELEASE_OUT}" "${RELEASE_KEEP}"
  trap restore_release EXIT
fi
step "Gradle: ${TASKS[*]}"
scripts/build.sh "${TASKS[@]}"

printf '\ncheck.sh: alles grün\n'
