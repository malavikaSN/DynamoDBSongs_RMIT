#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

echo "Starting DynamoDB Local via docker-compose..."
docker-compose up -d dynamodb

# wait for dynamodb
for i in {1..20}; do
  if curl -sS http://localhost:8000 >/dev/null 2>&1; then
    echo "DynamoDB Local is up"
    break
  fi
  echo "waiting for DynamoDB... ($i)"
  sleep 1
done

export DYNAMODB_ENDPOINT="http://host.docker.internal:8000"
export AWS_REGION="us-east-1"

echo "Building project..."
mvn -DskipTests package

echo "Run the ApiServer with DYNAMODB_ENDPOINT=$DYNAMODB_ENDPOINT"
# Run the built classes from target/classes on classpath
java -cp "target/classes:target/dependency/*" com.amazonaws.samples.ApiServer


# Note: to stop DynamoDB Local run: docker-compose down
