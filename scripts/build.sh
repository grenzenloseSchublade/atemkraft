#!/usr/bin/env bash
# Baut Atemkraft auf diesem Host (Pop!_OS), der nur ein JRE im PATH hat.
#
# Hintergrund: `java` zeigt hier auf ein JRE ohne `javac`, deshalb scheitert
# Gradles Toolchain-Auswahl ohne Hilfe. Dieses Skript sucht ein vollständiges
# JDK (mit javac), setzt JAVA_HOME und reicht alle Argumente an ./gradlew durch.
# Bevorzugt JDK 17 wie CI und Release-Builds (SEC-BUILD-06). Mit einem anderen JDK
# bricht es bei Release-Aufgaben ab (Reproduzierbarkeit), sonst baut es mit Warnung weiter.
#
# Beispiele:
#   scripts/build.sh                  # = assembleDebug
#   scripts/build.sh installDebug     # auf Gerät/Emulator installieren
#   scripts/build.sh clean assembleRelease
#   scripts/build.sh bundleRelease    # App Bundle für Play: eigener Aufruf (ohne ABI-Splits)
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

# JDK-Major wie in CI und Release-Checkliste (ci.yml, docs/SECURITY.md).
WANT_MAJOR=17

# 1) Vollständiges JDK (mit javac) finden – Reihenfolge: explizit, bekannte 17er-Pfade, Suche
#    (17 zuerst, dann irgendeins).
find_jdk() {
  if [[ -n "${JAVA_HOME:-}" && -x "${JAVA_HOME}/bin/javac" ]]; then
    echo "${JAVA_HOME}"; return 0
  fi
  local candidates=(
    "${HOME}/.gradle/jdks"/*-${WANT_MAJOR}-*
    "${HOME}/.local/jdks"/jdk-${WANT_MAJOR}*
    /usr/lib/jvm/java-${WANT_MAJOR}-openjdk-amd64
  )
  for c in "${candidates[@]}"; do
    [[ -x "${c}/bin/javac" ]] && { echo "${c}"; return 0; }
  done
  # Letzter Versuch: ein javac unter den üblichen JDK-Wurzeln, das gewünschte Major zuerst.
  local found any=""
  while IFS= read -r found; do
    local home
    home="$(dirname "$(dirname "${found}")")"
    [[ "$(jdk_major "${home}")" == "${WANT_MAJOR}" ]] && { echo "${home}"; return 0; }
    [[ -z "${any}" ]] && any="${home}"
  done < <(find "${HOME}/.local/jdks" "${HOME}/.gradle/jdks" /usr/lib/jvm \
    -maxdepth 3 -name javac -type f 2>/dev/null || true)
  [[ -n "${any}" ]] && { echo "${any}"; return 0; }
  return 1
}

# Major-Version eines JDK aus `javac -version` („javac 17.0.19“ → 17).
jdk_major() {
  "$1/bin/javac" -version 2>&1 | sed -n 's/^javac \([0-9]*\).*/\1/p'
}

JDK_HOME="$(find_jdk)" || {
  echo "FEHLER: Kein JDK mit javac gefunden. Bitte JDK ${WANT_MAJOR} installieren oder JAVA_HOME setzen." >&2
  exit 1
}
JDK_MAJOR="$(jdk_major "${JDK_HOME}")"
if [[ "${JDK_MAJOR}" != "${WANT_MAJOR}" ]]; then
  # Release-Artefakte müssen bitgleich nachbaubar sein (F-Droid, CI release-verify): kein
  # Release-Build mit fremdem JDK. Erkannt an Aufgaben wie assembleRelease, :app:bundleRelease.
  for task in "$@"; do
    if [[ "${task}" =~ ^(:?[A-Za-z0-9_-]+:)*[a-z][A-Za-z]*Release$ ]]; then
      echo "FEHLER: ${task} braucht JDK ${WANT_MAJOR} (gefunden: ${JDK_MAJOR:-?} in ${JDK_HOME})." \
        "JAVA_HOME auf ein JDK ${WANT_MAJOR} setzen (SEC-BUILD-06)." >&2
      exit 1
    fi
  done
  echo "WARNUNG: JDK ${JDK_MAJOR:-?} statt ${WANT_MAJOR} – CI und Release bauen mit ${WANT_MAJOR}; Screenshots" \
    "können abweichen." >&2
fi

echo "JDK:            ${JDK_HOME}"
echo "Aufgaben:       ${*:-assembleDebug}"
echo

cd "${PROJECT_DIR}"
exec env JAVA_HOME="${JDK_HOME}" \
  ./gradlew "${@:-assembleDebug}" --console=plain
