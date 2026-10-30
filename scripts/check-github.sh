#!/usr/bin/env bash
# Prüft die GitHub-Einstellungen des Repos gegen docs/SECURITY.md (Check check-github,
# SEC-REPO-02, SEC-SUP-03). Nur lesend über `gh api` (GET), ändert nichts.
#
#   scripts/check-github.sh                       # Repo aus dem Remote origin
#   scripts/check-github.sh --repo owner/name
#
# Soll-Zustand: 2FA am angemeldeten Konto, Private Vulnerability Reporting, Secret Scanning,
# Push Protection und Dependabot-Alerts an; ein aktives Ruleset verbietet auf `main`
# Force-Push und Löschen, eines macht Tags `v*` unveränderlich (kein Update, kein Löschen).
# Exit 0 = alles ok, 1 = Abweichung, 2 = nicht prüfbar (kein gh, kein Login, kein Zugriff).
set -euo pipefail
export LC_ALL=C.UTF-8

REPO=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --repo) REPO="${2:?--repo braucht owner/name}"; shift 2 ;;
    -h|--help) sed -n '2,11p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'; exit 0 ;;
    *) echo "check-github: unbekanntes Argument $1" >&2; exit 2 ;;
  esac
done

if ! command -v gh >/dev/null 2>&1; then
  echo "check-github: GitHub CLI \`gh\` fehlt – installieren und \`gh auth login\` ausführen." >&2
  exit 2
fi
if ! gh auth status >/dev/null 2>&1; then
  echo "check-github: \`gh\` ist nicht angemeldet – \`gh auth login\` (oder GH_TOKEN setzen) und erneut starten." >&2
  exit 2
fi
if [[ -z "${REPO}" ]]; then
  url="$(git -C "$(dirname "${BASH_SOURCE[0]}")/.." remote get-url origin 2>/dev/null || true)"
  REPO="$(sed -E 's#^(https://github\.com/|git@github\.com:)##; s#\.git$##' <<<"${url}")"
  if [[ ! "${REPO}" =~ ^[^/]+/[^/]+$ ]]; then
    echo "check-github: Repo nicht aus origin ableitbar (${url:-kein Remote}); --repo owner/name angeben." >&2
    exit 2
  fi
fi
API="repos/${REPO}"

# Lesender Aufruf; bricht bei fehlendem Zugriff mit Exit 2 ab statt eine Abweichung zu melden.
get() {
  local out
  if ! out="$(gh api --method GET "$@" 2>&1)"; then
    echo "check-github: gh api $* nicht lesbar: ${out}" >&2
    exit 2
  fi
  printf '%s' "${out}"
}

FAILED=0
report() { # report <ok|fehlt> <Text>
  printf '  %-5s %s\n' "$1" "$2"
  [[ "$1" == ok ]] || FAILED=1
}
expect() { # expect <Ist> <Soll> <Text>
  if [[ "$1" == "$2" ]]; then report ok "$3"; else report fehlt "$3 (ist: ${1:-leer})"; fi
}

echo "check-github: ${REPO}"

# Jeder Abruf erst in eine Variable: So beendet ein Lesefehler (Exit 2 in get) das Skript über set -e.
user="$(get user --jq '[.login, .two_factor_authentication] | @tsv')"
IFS=$'\t' read -r login twofa <<<"${user}"
expect "${twofa}" true "2FA am angemeldeten Konto ${login} (SEC-REPO-02)"
pvr="$(get "${API}/private-vulnerability-reporting" --jq '.enabled')"
expect "${pvr}" true "Private Vulnerability Reporting (SEC-REPO-02, „Sicherheitslücke melden“)"

sa="$(get "${API}" --jq '.security_and_analysis // {} | [.secret_scanning.status, .secret_scanning_push_protection.status] | @tsv')"
IFS=$'\t' read -r scanning push <<<"${sa}"
expect "${scanning}" enabled "Secret Scanning (SEC-REPO-02)"
expect "${push}" enabled "Push Protection (SEC-REPO-02)"

# 204 = an, 404 = aus; beides ist eine gültige Antwort, deshalb ohne get().
if gh api --method GET "${API}/vulnerability-alerts" >/dev/null 2>&1; then
  report ok "Dependabot-Alerts (SEC-SUP-03)"
else
  report fehlt "Dependabot-Alerts (SEC-SUP-03)"
fi

# main: alle Regeln, die auf den Branch wirken (auch aus Org-Rulesets), nur aktive.
main_rules="$(get "${API}/rules/branches/main" --jq '[.[].type] | join(" ")')"
for rule in non_fast_forward deletion; do
  if [[ " ${main_rules} " == *" ${rule} "* ]]; then
    report ok "Ruleset main: ${rule} (SEC-REPO-02)"
  else
    report fehlt "Ruleset main: ${rule} (SEC-REPO-02)"
  fi
done

# Tags v*: aktive Tag-Rulesets, deren Bedingung refs/tags/v* (oder alle Tags) umfasst.
tag_rules=""
ids="$(get "${API}/rulesets?includes_parents=true" --jq '.[] | select(.target == "tag" and .enforcement == "active") | .id')"
for id in ${ids}; do
  one="$(get "${API}/rulesets/${id}" --jq '
    select(any(.conditions.ref_name.include[]?; . == "refs/tags/v*" or . == "~ALL"))
    | [.rules[].type] | join(" ")')"
  tag_rules+=" ${one}"
done
for rule in update deletion; do
  if [[ " ${tag_rules} " == *" ${rule} "* ]]; then
    report ok "Ruleset Tags v*: ${rule} (SEC-REPO-02)"
  else
    report fehlt "Ruleset Tags v*: ${rule} (SEC-REPO-02)"
  fi
done

if [[ "${FAILED}" -eq 0 ]]; then
  echo "check-github: alles wie in SECURITY.md"
else
  echo "check-github: Abweichungen von SECURITY.md (Einstellungen → Code security bzw. Rules → Rulesets)"
fi
exit "${FAILED}"
