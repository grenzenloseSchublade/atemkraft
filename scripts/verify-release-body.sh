#!/usr/bin/env bash
# Prüft Release-Commit, Notes, Changelog und Release-Body (docs/SECURITY.md, Check release-verify;
# Logik in verify_release_body.py): versionCode = Vorgänger + 1, Notes und Changelog vorhanden,
# keine Heilversprechen, im Body SHA-256 je APK und der Signatur-Fingerprint.
#
#   scripts/verify-release-body.sh --tag v2.9.0                                  # Checkliste Schritt 2
#   scripts/verify-release-body.sh --tag v2.9.0 --body <datei> atemkraft-v2.9.0*.apk   # Schritt 7
set -euo pipefail
export LC_ALL=C.UTF-8
# -I: keine Module aus PYTHONPATH, Arbeitsordner oder Nutzer-site; verify_apk holt das Skript selbst
# aus seinem Ordner (sys.path.insert).
exec python3 -I "$(dirname "${BASH_SOURCE[0]}")/verify_release_body.py" "$@"
