#!/usr/bin/env bash
# End-to-end auth checks. Run with stack up: docker compose up -d
# Usage: ./scripts/verify-auth.sh [base]
#   base  Base URL for API (default http://127.0.0.1:8080). Use http://127.0.0.1:3000 to test Next proxy.
set -euo pipefail
BASE="${1:-http://127.0.0.1:8080}"
EMAIL="verify$(date +%s)@example.com"
PASS="VerifyPass123"

echo "== Smoke (GET / on BASE — Next proxy may not expose /actuator)"
curl -sf -o /dev/null -w "HTTP %{http_code}\n" "${BASE%/}/" || echo "(ignored)"
echo ""

echo "== Register POST ${BASE%/}/api/v1/auth/register"
REG_JSON=$(curl -sf -X POST "${BASE%/}/api/v1/auth/register" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASS\",\"companyName\":\"VerifyCo\"}") || {
  echo "FAIL: register"
  exit 1
}
TOKEN=$(echo "$REG_JSON" | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")
echo "OK register (token len ${#TOKEN})"

echo "== Login POST"
LOGIN_JSON=$(curl -sf -X POST "${BASE%/}/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASS\"}") || {
  echo "FAIL: login"
  exit 1
}
TOKEN=$(echo "$LOGIN_JSON" | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")

echo "== GET /api/v1/users/me"
ME=$(curl -sf "${BASE%/}/api/v1/users/me" -H "Authorization: Bearer $TOKEN") || {
  echo "FAIL: /users/me"
  exit 1
}
echo "$ME" | python3 -m json.tool 2>/dev/null || echo "$ME"

echo "== GET /api/v1/profile/my"
curl -sf "${BASE%/}/api/v1/profile/my" -H "Authorization: Bearer $TOKEN" | head -c 300
echo ""
echo "ALL OK"
