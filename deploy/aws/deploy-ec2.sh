#!/usr/bin/env bash
set -euo pipefail

if [[ ! -f .env.aws ]]; then
  echo "Missing .env.aws in repo root. Copy deploy/aws/.env.aws.example to .env.aws first."
  exit 1
fi

if [[ ! -f docker-compose.yml ]]; then
  echo "Run this script from the repository root where docker-compose.yml exists."
  exit 1
fi

docker compose --env-file .env.aws build
docker compose --env-file .env.aws up -d

echo "Deployment started. Check status with: docker compose ps"
