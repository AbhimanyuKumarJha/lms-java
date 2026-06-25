#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
RUN_ID="${RUN_ID:-$(date +%s)}"

USER_ID="${USER_ID:-$((1000000000 + RUN_ID % 8999999999))}"
SECOND_USER_ID="${SECOND_USER_ID:-$((USER_ID + 1))}"
BOOK_COPY_ID="${BOOK_COPY_ID:-$((200000 + RUN_ID % 700000))}"
SECOND_BOOK_COPY_ID="${SECOND_BOOK_COPY_ID:-$((BOOK_COPY_ID + 1))}"
THIRD_BOOK_COPY_ID="${THIRD_BOOK_COPY_ID:-$((BOOK_COPY_ID + 2))}"
TRANSACTION_ID="${TRANSACTION_ID:-$((500000 + RUN_ID % 400000))}"
SECOND_TRANSACTION_ID="${SECOND_TRANSACTION_ID:-$((TRANSACTION_ID + 1))}"
THIRD_TRANSACTION_ID="${THIRD_TRANSACTION_ID:-$((TRANSACTION_ID + 2))}"

EMAIL="${EMAIL:-asha.${RUN_ID}@example.com}"
SIGNUP_EMAIL="${SIGNUP_EMAIL:-signup.${RUN_ID}@example.com}"
PASSWORD="${PASSWORD:-password123}"
OTP="${OTP:-}"

post_json() {
  local label="$1"
  local path="$2"
  local payload="$3"

  echo
  echo "==> ${label}"
  curl --fail-with-body -sS \
    -X POST "${BASE_URL}${path}" \
    -H "Content-Type: application/json" \
    -d "${payload}"
  echo
}

echo "Seeding LMS dummy data into ${BASE_URL}"
echo "Run id: ${RUN_ID}"

post_json "Signup user with generated id and OTP event" "/api/auth/signup" "{
  \"name\": \"Signup Student ${RUN_ID}\",
  \"email\": \"${SIGNUP_EMAIL}\",
  \"password\": \"${PASSWORD}\"
}"

if [[ -n "${OTP}" ]]; then
  post_json "Verify signup OTP" "/api/otp/verify" "{
    \"email\": \"${SIGNUP_EMAIL}\",
    \"otp\": \"${OTP}\"
  }"
else
  echo
  echo "==> Skipping OTP verify"
  echo "Set OTP=123456 before running this script if you have the generated signup OTP."
fi

post_json "Create library user" "/api/users" "{
  \"userId\": ${USER_ID},
  \"name\": \"Asha Sharma ${RUN_ID}\",
  \"email\": \"${EMAIL}\",
  \"password\": \"${PASSWORD}\"
}"

post_json "Create second library user" "/api/users" "{
  \"userId\": ${SECOND_USER_ID},
  \"name\": \"Rohan Mehta ${RUN_ID}\",
  \"email\": \"rohan.${RUN_ID}@example.com\",
  \"password\": \"${PASSWORD}\"
}"

post_json "Create Clean Architecture copy" "/api/book-copies" "{
  \"bookCopyId\": ${BOOK_COPY_ID},
  \"title\": \"Clean Architecture\",
  \"author\": \"Robert C. Martin\",
  \"description\": \"A practical guide to software architecture principles.\",
  \"copyCode\": \"BK-CA-${RUN_ID}-1\"
}"

post_json "Create second Clean Architecture copy" "/api/book-copies" "{
  \"bookCopyId\": ${SECOND_BOOK_COPY_ID},
  \"title\": \"Clean Architecture\",
  \"author\": \"Robert C. Martin\",
  \"description\": \"A practical guide to software architecture principles.\",
  \"copyCode\": \"BK-CA-${RUN_ID}-2\"
}"

post_json "Create Domain-Driven Design copy" "/api/book-copies" "{
  \"bookCopyId\": ${THIRD_BOOK_COPY_ID},
  \"title\": \"Domain-Driven Design\",
  \"author\": \"Eric Evans\",
  \"description\": \"Tackling complexity in the heart of software.\",
  \"copyCode\": \"BK-DDD-${RUN_ID}-1\"
}"

post_json "Borrow one book copy" "/api/issues" "{
  \"transactionId\": ${TRANSACTION_ID},
  \"userId\": ${USER_ID},
  \"bookCopyId\": ${BOOK_COPY_ID}
}"

post_json "Borrow multiple book copies in bulk" "/api/issues/bulk" "[
  {
    \"transactionId\": ${SECOND_TRANSACTION_ID},
    \"userId\": ${USER_ID},
    \"bookCopyId\": ${SECOND_BOOK_COPY_ID}
  },
  {
    \"transactionId\": ${THIRD_TRANSACTION_ID},
    \"userId\": ${SECOND_USER_ID},
    \"bookCopyId\": ${THIRD_BOOK_COPY_ID}
  }
]"

post_json "Return the first borrowed book" "/api/issues/${TRANSACTION_ID}/return" "{}"

echo
echo "Seed complete."
echo "Created user ids: ${USER_ID}, ${SECOND_USER_ID}"
echo "Created book copy ids: ${BOOK_COPY_ID}, ${SECOND_BOOK_COPY_ID}, ${THIRD_BOOK_COPY_ID}"
echo "Created transaction ids: ${TRANSACTION_ID}, ${SECOND_TRANSACTION_ID}, ${THIRD_TRANSACTION_ID}"
