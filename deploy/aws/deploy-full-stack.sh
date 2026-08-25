#!/usr/bin/env bash
set -euo pipefail

if [[ ! -f .env.aws ]]; then
  echo "Missing .env.aws in repo root. Copy deploy/aws/.env.aws.example to .env.aws first."
  exit 1
fi

source .env.aws

required_vars=(
  AWS_REGION
  CF_FULL_STACK_NAME
  EC2_KEY_PAIR_NAME
  GIT_REPO_URL
  SECURITY_JWT_SECRET
)

for var_name in "${required_vars[@]}"; do
  if [[ -z "${!var_name:-}" ]]; then
    echo "Missing required variable: $var_name"
    exit 1
  fi
done

aws cloudformation deploy \
  --region "$AWS_REGION" \
  --stack-name "$CF_FULL_STACK_NAME" \
  --template-file deploy/aws/full-stack-ec2.yaml \
  --capabilities CAPABILITY_NAMED_IAM \
  --parameter-overrides \
    ProjectName="${PROJECT_NAME:-laundry}" \
    KeyPairName="$EC2_KEY_PAIR_NAME" \
    InstanceType="${EC2_INSTANCE_TYPE:-t3.small}" \
    AllowedSshCidr="${ALLOWED_SSH_CIDR:-0.0.0.0/0}" \
    AllowedHttpCidr="${ALLOWED_HTTP_CIDR:-0.0.0.0/0}" \
    EnableAlbHttps="${ENABLE_ALB_HTTPS:-false}" \
    VpcId="${VPC_ID:-}" \
    PublicSubnetOneId="${PUBLIC_SUBNET_ONE_ID:-}" \
    PublicSubnetTwoId="${PUBLIC_SUBNET_TWO_ID:-}" \
    AcmCertificateArn="${ACM_CERTIFICATE_ARN:-}" \
    DomainName="${CUSTOM_DOMAIN_NAME:-}" \
    HostedZoneId="${HOSTED_ZONE_ID:-}" \
    SubmissionsTableName="${DYNAMODB_SUBMISSIONS_TABLE:-submissions}" \
    PaymentsTableName="${DYNAMODB_PAYMENTS_TABLE:-payments}" \
    GitRepoUrl="$GIT_REPO_URL" \
    GitRepoBranch="${GIT_REPO_BRANCH:-main}" \
    JwtSecret="$SECURITY_JWT_SECRET"

echo "Full stack deployed: $CF_FULL_STACK_NAME"
aws cloudformation describe-stacks \
  --region "$AWS_REGION" \
  --stack-name "$CF_FULL_STACK_NAME" \
  --query 'Stacks[0].Outputs' \
  --output table
