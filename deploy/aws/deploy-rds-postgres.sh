#!/usr/bin/env bash
set -euo pipefail

if [[ ! -f .env.aws ]]; then
  echo "Missing .env.aws in repo root. Copy deploy/aws/.env.aws.example to .env.aws first."
  exit 1
fi

source .env.aws

AWS_REGION="${AWS_REGION:-us-east-1}"
RDS_STACK_NAME="${RDS_STACK_NAME:-laundry-rds}"
RDS_DB_INSTANCE_IDENTIFIER="${RDS_DB_INSTANCE_IDENTIFIER:-laundry-payments}"
RDS_DB_NAME="${RDS_DB_NAME:-payments}"
RDS_DB_USERNAME="${RDS_DB_USERNAME:-payments}"
RDS_DB_PASSWORD="${RDS_DB_PASSWORD:-}"
RDS_DB_CLASS="${RDS_DB_CLASS:-db.t3.micro}"
RDS_DB_STORAGE_GB="${RDS_DB_STORAGE_GB:-20}"
RDS_VPC_ID="${RDS_VPC_ID:-}"
RDS_SUBNET_IDS="${RDS_SUBNET_IDS:-}"
RDS_ALLOWED_CIDR="${RDS_ALLOWED_CIDR:-10.0.0.0/16}"
RDS_PUBLICLY_ACCESSIBLE="${RDS_PUBLICLY_ACCESSIBLE:-false}"
RDS_BACKUP_RETENTION_DAYS="${RDS_BACKUP_RETENTION_DAYS:-7}"
RDS_MULTI_AZ="${RDS_MULTI_AZ:-false}"

if [[ -z "$RDS_DB_PASSWORD" ]]; then
  echo "RDS_DB_PASSWORD is required in .env.aws"
  exit 1
fi

if [[ -z "$RDS_VPC_ID" || -z "$RDS_SUBNET_IDS" ]]; then
  echo "RDS_VPC_ID and RDS_SUBNET_IDS are required in .env.aws"
  exit 1
fi

aws cloudformation deploy \
  --region "$AWS_REGION" \
  --stack-name "$RDS_STACK_NAME" \
  --template-file deploy/aws/rds-postgres-stack.yaml \
  --capabilities CAPABILITY_NAMED_IAM \
  --parameter-overrides \
    ProjectName="laundry" \
    DbInstanceIdentifier="$RDS_DB_INSTANCE_IDENTIFIER" \
    DbName="$RDS_DB_NAME" \
    DbUsername="$RDS_DB_USERNAME" \
    DbPassword="$RDS_DB_PASSWORD" \
    DbClass="$RDS_DB_CLASS" \
    DbStorageGB="$RDS_DB_STORAGE_GB" \
    VpcId="$RDS_VPC_ID" \
    SubnetIds="$RDS_SUBNET_IDS" \
    AllowedCidr="$RDS_ALLOWED_CIDR" \
    PubliclyAccessible="$RDS_PUBLICLY_ACCESSIBLE" \
    BackupRetentionDays="$RDS_BACKUP_RETENTION_DAYS" \
    MultiAz="$RDS_MULTI_AZ"

echo "CloudFormation stack deployed: $RDS_STACK_NAME"
aws cloudformation describe-stacks \
  --region "$AWS_REGION" \
  --stack-name "$RDS_STACK_NAME" \
  --query 'Stacks[0].Outputs' \
  --output table
