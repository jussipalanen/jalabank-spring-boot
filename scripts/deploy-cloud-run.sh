#!/usr/bin/env bash
# Shared by ./dev deploy and GitHub Actions so cost settings stay consistent.
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
: "${GCP_PROJECT_ID:?Set GCP_PROJECT_ID to the Google Cloud project ID}"
command -v gcloud >/dev/null 2>&1 || { echo "Install the Google Cloud CLI first." >&2; exit 1; }

exec gcloud run deploy "${CLOUD_RUN_SERVICE:-jalabank}" \
    --source "$ROOT_DIR/release" \
    --project "$GCP_PROJECT_ID" \
    --region "${CLOUD_RUN_REGION:-europe-north1}" \
    --service-account "${CLOUD_RUN_SERVICE_ACCOUNT:-jalabank-runtime@$GCP_PROJECT_ID.iam.gserviceaccount.com}" \
    --min 0 \
    --max 1 \
    --min-instances 0 \
    --max-instances 1 \
    --cpu 1 \
    --memory 512Mi \
    --cpu-throttling \
    --cpu-boost \
    --concurrency 40 \
    --session-affinity \
    --port 8080 \
    --timeout 60 \
    --startup-probe 'httpGet.path=/actuator/health,httpGet.port=8080,periodSeconds=5,timeoutSeconds=3,failureThreshold=24' \
    --update-env-vars SPRING_PROFILES_ACTIVE=cloud-run \
    "$@"
