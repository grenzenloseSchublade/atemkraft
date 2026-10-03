#!/usr/bin/env bash
# Stil-Checks aus docs/STYLEGUIDE.md §13.3 (Logik in check_style.py). Argumente werden
# durchgereicht: --self-test, --update-baseline, --ci-range A..B.
set -euo pipefail
export LC_ALL=C.UTF-8
exec python3 "$(dirname "${BASH_SOURCE[0]}")/check_style.py" "$@"
