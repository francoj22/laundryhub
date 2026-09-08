#!/bin/bash
# Quick deployment script for all services (gateway, submissions, payments)

set -e

echo "🚀 Deploying all services to production..."

# Load environment variables
source .env.aws

# Get EC2 IP
echo "📡 Finding EC2 instance..."
EC2_IP=$(aws ec2 describe-instances \
  --region "$AWS_REGION" \
  --filters "Name=instance-state-name,Values=running" \
  --query 'Reservations[0].Instances[0].PublicIpAddress' \
  --output text)

if [ "$EC2_IP" == "None" ]; then
  echo "❌ No running EC2 instance found"
  exit 1
fi

echo "✅ Found EC2 instance: $EC2_IP"
echo ""

# Deploy
echo "🔄 Pulling latest code and restarting all services..."
ssh -o StrictHostKeyChecking=no -i deploy/aws/keys/${EC2_KEY_PAIR_NAME}.pem ec2-user@$EC2_IP << 'ENDSSH'
cd laundry
git pull origin main
docker-compose --env-file .env.aws up -d --build
echo ""
echo "📊 Services status:"
docker-compose --env-file .env.aws ps
ENDSSH

echo ""
echo "✅ Deployment complete!"
echo ""
echo "🧪 Test with:"
echo "curl https://api.laundrywithme.com/auth/token?userId=test&role=user"
