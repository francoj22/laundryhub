#!/usr/bin/env bash
set -euo pipefail

if [[ ! -f .env.aws ]]; then
  echo "Missing .env.aws in repo root. Copy deploy/aws/.env.aws.example to .env.aws first."
  exit 1
fi

source .env.aws

AWS_REGION="${AWS_REGION:-us-east-1}"
CF_STACK_NAME="${CF_STACK_NAME:-laundry-dynamodb}"
SUBMISSIONS_TABLE="${DYNAMODB_SUBMISSIONS_TABLE:-submissions}"
PAYMENTS_TABLE="${DYNAMODB_PAYMENTS_TABLE:-payments}"

aws cloudformation deploy \
  --region "$AWS_REGION" \
  --stack-name "$CF_STACK_NAME" \
  --template-file deploy/aws/dynamodb-stack.yaml \
  --capabilities CAPABILITY_NAMED_IAM \
  --parameter-overrides \
    SubmissionsTableName="$SUBMISSIONS_TABLE" \
    PaymentsTableName="$PAYMENTS_TABLE"

echo "CloudFormation stack deployed: $CF_STACK_NAME"
aws cloudformation describe-stacks \
  --region "$AWS_REGION" \
  --stack-name "$CF_STACK_NAME" \
  --query 'Stacks[0].Outputs' \
  --output table
