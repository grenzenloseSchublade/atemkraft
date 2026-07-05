#!/usr/bin/env bash
# Baut Atemkraft auf diesem Host (Pop!_OS), der nur ein JRE im PATH hat.
#
# Hintergrund: `java` zeigt hier auf ein JRE ohne `javac`, deshalb scheitert
# Gradles Toolchain-Auswahl ohne Hilfe. Dieses Skript sucht ein vollständiges
# JDK (mit javac), setzt JAVA_HOME und reicht alle Argumente an ./gradlew durch.
#
# Beispiele:
#   scripts/build.sh                  # = assembleDebug
#   scripts/build.sh installDebug     # auf Gerät/Emulator installieren
#   scripts/build.sh clean assembleRelease
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

# 1) Vollständiges JDK (mit javac) finden – Reihenfolge: explizit, bekannte Pfade, Suche.
find_jdk() {
  if [[ -n "${JAVA_HOME:-}" && -x "${JAVA_HOME}/bin/javac" ]]; then
    echo "${JAVA_HOME}"; return 0
  fi
  local candidates=(
    "${HOME}/.local/jdks/jdk-21.0.11+10"
    "${HOME}/.local/jdks"/jdk-21*
    "${HOME}/.gradle/jdks"/*-21-*
    /usr/lib/jvm/java-21-openjdk-amd64
  )
  for c in "${candidates[@]}"; do
    [[ -x "${c}/bin/javac" ]] && { echo "${c}"; return 0; }
  done
  # Letzter Versuch: irgendein javac unter den üblichen JDK-Wurzeln.
  local found
  found="$(find "${HOME}/.local/jdks" "${HOME}/.gradle/jdks" /usr/lib/jvm \
    -maxdepth 3 -name javac -type f 2>/dev/null | head -n1 || true)"
  [[ -n "${found}" ]] && { echo "$(dirname "$(dirname "${found}")")"; return 0; }
  return 1
}

JDK_HOME="$(find_jdk)" || {
  echo "FEHLER: Kein JDK mit javac gefunden. Bitte ein JDK 17+ installieren oder JAVA_HOME setzen." >&2
  exit 1
}

# 2) Gradle-Cache wiederverwenden, falls vorhanden (spart den Gradle-Download).
GRADLE_HOME_CACHE="${HOME}/Entwicklung/audiotomy/.gradle-home"
GRADLE_USER_HOME_ARG=()
if [[ -d "${GRADLE_HOME_CACHE}/wrapper/dists" ]]; then
  GRADLE_USER_HOME_ARG=(GRADLE_USER_HOME="${GRADLE_HOME_CACHE}")
fi

echo "JDK:            ${JDK_HOME}"
echo "Gradle-Cache:   ${GRADLE_HOME_CACHE} (wenn vorhanden)"
echo "Aufgaben:       ${*:-assembleDebug}"
echo

cd "${PROJECT_DIR}"
exec env JAVA_HOME="${JDK_HOME}" "${GRADLE_USER_HOME_ARG[@]}" \
  ./gradlew "${@:-assembleDebug}" --console=plain
