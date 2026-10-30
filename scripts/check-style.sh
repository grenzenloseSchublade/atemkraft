#!/usr/bin/env bash
# Stil-Checks zu docs/STYLEGUIDE.md, Tabelle in docs/CHECKS.md (Logik in check_style.py). Argumente werden
# durchgereicht: --self-test, --update-baseline, --ci-range A..B, --commit-msg DATEI.
set -euo pipefail
export LC_ALL=C.UTF-8
exec python3 "$(dirname "${BASH_SOURCE[0]}")/check_style.py" "$@"
