#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
MAKEFILE="$REPO_ROOT/Makefile"

fail() {
  echo "FAIL: $*" >&2
  exit 1
}

grep -Eq '^DEV_WEB_HOST[[:space:]]*\?=[[:space:]]*127\.0\.0\.1$' "$MAKEFILE" \
  || fail "Makefile must default DEV_WEB_HOST to 127.0.0.1"

grep -Fq 'pnpm exec vite --host $(DEV_WEB_HOST)' "$MAKEFILE" \
  || fail "dev web startup must pass DEV_WEB_HOST to vite"

grep -Fq "cd server && /bin/sh -lc '\$(DEV_SERVER_PREPARE) && exec env \$(DEV_SERVER_SCANNER_ENV) \$(DEV_SERVER_CMD)'" "$MAKEFILE" \
  || fail "dev-server must inject scanner upload environment"

if grep -Fq 'pnpm exec vite --host 0.0.0.0' "$MAKEFILE"; then
  fail "dev web startup must not bind Vite to 0.0.0.0 by default"
fi

echo "dev-web-host-test passed"
