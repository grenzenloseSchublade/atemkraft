#!/usr/bin/env bash
# Prüft Release-APKs gegen docs/SECURITY.md (Werkzeug verify-apk; Logik in verify_apk.py):
# Zertifikat, Berechtigungen, exportierte Komponenten, Härtung, Native Libs, 16-KB-Ausrichtung,
# Lizenztexte, verbotene Bibliotheken und mit --tag die Herkunft (Tag-Commit, versionName).
#
#   scripts/verify-apk.sh atemkraft-v2.5.0.apk atemkraft-v2.5.0-arm64-v8a.apk --tag v2.5.0
#   scripts/verify-apk.sh app/build/outputs/apk/release/*.apk --allow-unsigned   # Prüf-Build
set -euo pipefail
export LC_ALL=C.UTF-8
exec python3 "$(dirname "${BASH_SOURCE[0]}")/verify_apk.py" "$@"
