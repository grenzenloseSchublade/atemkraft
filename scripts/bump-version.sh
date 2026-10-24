#!/usr/bin/env bash
# Versionsbump an einer Stelle (docs/PROCESS.md PROZ-05): setzt versionName und versionCode (+1) in
# app/build.gradle.kts, legt docs/release-notes/vX.Y.Z.md und
# fastlane/metadata/android/de-DE/changelogs/<versionCode>.txt als Vorlagen an und ersetzt die alte
# Version in den Stand-Zeilen von STYLEGUIDE, SECURITY, PRIVACY, THIRD_PARTY_LICENSES und STORE.
# Danach prüft `scripts/check-style.sh` (version-sync), dass alles übereinstimmt.
#
#   scripts/bump-version.sh 2.8.0          # bump; Notes und Changelog danach von Hand füllen
#   scripts/bump-version.sh 2.8.0 --dry    # nur zeigen, was sich ändern würde
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${ROOT}"

NEW="${1:-}"
DRY=false
[[ "${2:-}" == "--dry" ]] && DRY=true
if [[ ! "${NEW}" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
  echo "Aufruf: scripts/bump-version.sh X.Y.Z [--dry]" >&2
  exit 2
fi

GRADLE="app/build.gradle.kts"
OLD="$(sed -nE 's/^\s*versionName\s*=\s*"([^"]+)".*/\1/p' "${GRADLE}" | head -1)"
OLD_CODE="$(sed -nE 's/^\s*versionCode\s*=\s*([0-9]+).*/\1/p' "${GRADLE}" | head -1)"
NEW_CODE=$((OLD_CODE + 1))
TODAY="$(date +%F)"
NOTES="docs/release-notes/v${NEW}.md"
CHANGELOG="fastlane/metadata/android/de-DE/changelogs/${NEW_CODE}.txt"
STAND_FILES=(docs/STYLEGUIDE.md docs/SECURITY.md docs/PRIVACY.md THIRD_PARTY_LICENSES.md docs/STORE.md)

if [[ -z "${OLD}" || -z "${OLD_CODE}" ]]; then
  echo "versionName/versionCode in ${GRADLE} nicht gefunden" >&2
  exit 1
fi
if [[ "${OLD}" == "${NEW}" ]]; then
  echo "Version ${NEW} ist schon gesetzt" >&2
  exit 1
fi
echo "Version ${OLD} (${OLD_CODE}) → ${NEW} (${NEW_CODE})"

if $DRY; then
  echo "würde ändern: ${GRADLE}, ${NOTES} (neu), ${CHANGELOG} (neu), Stand-Zeilen in: ${STAND_FILES[*]}"
  exit 0
fi

# 1. Gradle
sed -i -E "s/^(\s*versionName\s*=\s*)\"${OLD//./\\.}\"/\1\"${NEW}\"/" "${GRADLE}"
sed -i -E "s/^(\s*versionCode\s*=\s*)${OLD_CODE}\b/\1${NEW_CODE}/" "${GRADLE}"

# 2. Stand-Zeilen: nur in den ersten Kopfzeilen jeder Datei die alte Version durch die neue ersetzen
#    (dort steht „Stand: vX.Y.Z“, „gegen Version X.Y.Z“, „bis X.Y.Z“, „App-Version X.Y.Z“).
for f in "${STAND_FILES[@]}"; do
  [[ -f "${f}" ]] || continue
  sed -i -E "1,6 s/(^|[^0-9.])${OLD//./\\.}([^0-9.]|$)/\1${NEW}\2/g" "${f}"
  # Stand-Datum der Hauptdokumente auf heute setzen, wo die Zeile ein Datum trägt.
  sed -i -E "1,6 s/(Stand:\*{0,2} )[0-9]{4}-[0-9]{2}-[0-9]{2}/\1${TODAY}/" "${f}"
done

# 3. Release-Notes (nur die Änderungen; SHA-256 und Fingerprint kommen in Schritt 7 der Checkliste dazu)
if [[ ! -f "${NOTES}" ]]; then
  cat > "${NOTES}" <<EOF
# Atemkraft ${NEW}

<!-- Was sich für Nutzer ändert, in Laiensprache (TEXT-11, SEC-LEGAL-01): eine Zeile je Änderung. -->
-
EOF
fi

# 4. Changelog für die Stores (≤ 500 Zeichen, Nutzen in Laiensprache, keine internen IDs)
if [[ ! -f "${CHANGELOG}" ]]; then
  cat > "${CHANGELOG}" <<EOF
•
EOF
fi

echo "geändert: ${GRADLE}, ${STAND_FILES[*]}"
echo "angelegt: ${NOTES}, ${CHANGELOG} – jetzt füllen, dann scripts/check-style.sh (version-sync) und scripts/check.sh"
echo "Commit: vX.Y.Z-Betreff „v${NEW}: …“ (PROZ-01, PROZ-05); Tag, Push und Release nur nach Bestätigung (PROZ-04)"
