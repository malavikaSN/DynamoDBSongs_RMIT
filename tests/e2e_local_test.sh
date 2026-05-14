#!/usr/bin/env bash
set -euo pipefail

OUT=/tmp/e2e_local_run.log
echo "E2E Local Test run at $(date -u)" > "$OUT"
BASE="http://127.0.0.1:4567"

# random suffix to avoid collisions
SUFFIX=$(date +%s)
EMAIL="testuser+${SUFFIX}@example.com"
PASSWORD="Passw0rd-${SUFFIX}"
USERNAME="testuser-${SUFFIX}"
ARTIST="Test Artist ${SUFFIX}"
SONGKEY="song-${SUFFIX}"
TITLE="Test Song ${SUFFIX}"
ALBUM="Test Album"
YEAR="2026"
IMAGE_URL="https://example.com/image-${SUFFIX}.jpg"

echo "Using: EMAIL=$EMAIL, USERNAME=$USERNAME, SONGKEY=$SONGKEY" | tee -a "$OUT"

# Register
echo "\n=== REGISTER ===" | tee -a "$OUT"
REG_RESP=$(curl -s -w "\nHTTP_CODE:%{http_code}\n" -X POST "$BASE/api/register" -H "Content-Type: application/json" -d "{\"email\": \"$EMAIL\", \"user_name\": \"$USERNAME\", \"password\": \"$PASSWORD\"}")
echo "$REG_RESP" | tee -a "$OUT"

# Login
echo "\n=== LOGIN ===" | tee -a "$OUT"
LOGIN_RESP=$(curl -s -X POST "$BASE/api/login" -H "Content-Type: application/json" -d "{\"email\": \"$EMAIL\", \"password\": \"$PASSWORD\"}")
echo "$LOGIN_RESP" | tee -a "$OUT"
TOKEN=$(echo "$LOGIN_RESP" | python3 -c 'import sys, json
try:
    print(json.load(sys.stdin).get("token", ""))
except Exception:
    print("")')

if [ -z "$TOKEN" ]; then
  echo "No token returned; aborting." | tee -a "$OUT"
  exit 2
fi

echo "Token present (len=${#TOKEN})" | tee -a "$OUT"

# GET songs (before)
echo "\n=== GET /api/songs (before) ===" | tee -a "$OUT"
curl -s -X GET "$BASE/api/songs" -H "Content-Type: application/json" | tee -a "$OUT"

# POST /api/songs (subscribe/create)
echo "\n=== POST /api/songs (create) ===" | tee -a "$OUT"
CREATE_RESP=$(curl -s -w "\nHTTP_CODE:%{http_code}\n" -X POST "$BASE/api/songs" -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" -d "{\"artist\": \"$ARTIST\", \"songKey\": \"$SONGKEY\", \"title\": \"$TITLE\", \"album\": \"$ALBUM\", \"year\": \"$YEAR\", \"image_url\": \"$IMAGE_URL\"}")

echo "$CREATE_RESP" | tee -a "$OUT"

# GET songs (after)
echo "\n=== GET /api/songs (after) ===" | tee -a "$OUT"
AFTER=$(curl -s -X GET "$BASE/api/songs" -H "Content-Type: application/json")
echo "$AFTER" | tee -a "$OUT"

echo "\nSearching for created songKey=$SONGKEY" | tee -a "$OUT"
echo "$AFTER" | grep -F "$SONGKEY" && echo "Found created item" | tee -a "$OUT" || (echo "Created item not found" | tee -a "$OUT" )

# Delete item using AWS CLI (requires aws configured and permissions)
echo "\n=== DELETE item via aws cli ===" | tee -a "$OUT"
if command -v aws >/dev/null 2>&1; then
  echo "Deleting item from DynamoDB table 'music' with artist='$ARTIST' and songKey='$SONGKEY'" | tee -a "$OUT"
  aws dynamodb delete-item --table-name music --key "{\"artist\": {\"S\": \"$ARTIST\"}, \"songKey\": {\"S\": \"$SONGKEY\"}}" >> "$OUT" 2>&1 || echo "aws cli delete failed" | tee -a "$OUT"
else
  echo "aws CLI not found; cannot delete item. Skipping delete." | tee -a "$OUT"
fi

# GET songs (final)
echo "\n=== GET /api/songs (final) ===" | tee -a "$OUT"
FINAL=$(curl -s -X GET "$BASE/api/songs" -H "Content-Type: application/json")
echo "$FINAL" | tee -a "$OUT"

echo "\nE2E Test finished at $(date -u)" | tee -a "$OUT"

# Exit success
exit 0
