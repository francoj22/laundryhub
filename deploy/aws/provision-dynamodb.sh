#!/usr/bin/env bash
set -euo pipefail

AWS_REGION="${AWS_REGION:-us-east-1}"
SUBMISSIONS_TABLE="${DYNAMODB_SUBMISSIONS_TABLE:-submissions}"
PAYMENTS_TABLE="${DYNAMODB_PAYMENTS_TABLE:-payments}"

create_table_if_missing() {
  local table_name="$1"

  if aws dynamodb describe-table --table-name "$table_name" --region "$AWS_REGION" >/dev/null 2>&1; then
    echo "Table $table_name already exists"
    return
  fi

  aws dynamodb create-table \
    --table-name "$table_name" \
    --attribute-definitions AttributeName=id,AttributeType=S \
    --key-schema AttributeName=id,KeyType=HASH \
    --billing-mode PAY_PER_REQUEST \
    --region "$AWS_REGION"

  aws dynamodb wait table-exists --table-name "$table_name" --region "$AWS_REGION"
  echo "Table $table_name created"
}

create_table_if_missing "$SUBMISSIONS_TABLE"
create_table_if_missing "$PAYMENTS_TABLE"

echo "DynamoDB provisioning completed in region $AWS_REGION"
