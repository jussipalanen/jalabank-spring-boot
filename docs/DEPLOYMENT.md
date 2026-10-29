# Google Cloud Run

Jalabank runs as one public Cloud Run service, using the existing `release/Dockerfile`.
Cloud Build builds the image and runs the Maven tests. The service serves both the web
pages and REST API. All customers, transactions, authenticator codes and sign-in
sessions stay in memory. No database, persistent disk, VPC connector or load balancer
is needed.

This follows JalaOps's source deployment and GitHub Workload Identity Federation
setup, with scale-to-zero defaults for lower costs.

## Configured project

The demo is configured in the same Google Cloud project as JalaOps:

| Resource | Value |
|---|---|
| Project | `client-jussialanen` (`1097769575593`) |
| Region | `europe-north1` |
| Cloud Run service | `jalabank` |
| Public URL | <https://jalabank-demo.jussialanen.com/> |
| Default service URL | <https://jalabank-1097769575593.europe-north1.run.app> |
| Runtime account | `jalabank-runtime@client-jussialanen.iam.gserviceaccount.com` |
| GitHub deployer | `jalabank-deployer@client-jussialanen.iam.gserviceaccount.com` |
| Workload Identity provider | `github/providers/jalabank` |

The runtime account has no project roles. The deployer has the source deployment
roles and permission to use the runtime and build accounts. Its GitHub identity is
restricted to repository ID `665058949`, `jussipalanen/jalabank-spring-boot` on
`refs/heads/main`, and owner actor ID `4532255`. The three
`GCP_*` GitHub repository variables are configured. Automatic deployment becomes
available when the workflows are merged into `main`.

Only the owner can update `main` and trigger production deployment. See
[repository protection](REPOSITORY.md) for the live access rules.

To deploy your working copy manually:

```bash
export GCP_PROJECT_ID=client-jussialanen
./dev deploy --quiet
./dev gcloud:status
```

The setup below documents how to recreate this configuration in another project.

## Service settings and costs

Manual and automatic deploys both use [`scripts/deploy-cloud-run.sh`](../scripts/deploy-cloud-run.sh).
The script reapplies these settings on each deploy:

| Setting | Value | Purpose |
|---|---|---|
| Region | `europe-north1` (Finland) | Same region as JalaOps |
| Minimum instances | `0` at service and revision level | Scale to zero when idle |
| Maximum instances | `1` at service and revision level | Limit spending and separate copies of the in-memory data |
| Billing | Request based (`--cpu-throttling`) | No always-allocated CPU |
| CPU / memory | 1 vCPU / 512 MiB | Small Java runtime |
| Startup CPU boost | Enabled | Faster Java cold starts |
| Concurrent requests | 40 | Share the instance across visitors |
| Session affinity | Enabled | Prefer the same instance for a visitor |
| Request timeout | 60 seconds | Bound the lifetime of a request |
| Startup probe | `/actuator/health`, up to 120 seconds | Wait for Spring Boot to become healthy |
| Spring profile | `cloud-run` | Honor forwarded HTTPS headers and use secure session cookies |

With zero minimum instances, request-based billing avoids idle instance charges.
Light usage may fit the free allowance, shared with other services in the billing
account. Cloud Build, Artifact Registry, outbound traffic and logging can still cost
money. See [Cloud Run pricing](https://cloud.google.com/run/pricing),
[Cloud Build pricing](https://cloud.google.com/build/pricing) and
[Artifact Registry pricing](https://cloud.google.com/artifact-registry/pricing).
Set a budget alert in **Billing → Budgets & alerts**; alerts do not stop spending.

The first visit after an idle period waits for a cold start. Every restart or deploy
resets the demo data and logs visitors out. Instance limits and affinity do not make
in-memory state durable: old and new revisions can overlap during deployment, and
Cloud Run can briefly exceed instance limits. A visitor may need to sign in again.
Use the generated `run.app` HTTPS address; a custom domain is optional.

## Custom domain

`jalabank-demo.jussialanen.com` is mapped to the `jalabank` service in Cloud Run.
GoDaddy DNS has the required record:

| Type | Name | Target |
|---|---|---|
| CNAME | `jalabank-demo` | `ghs.googlehosted.com` |

Cloud Run provisions and renews the HTTPS certificate automatically. After changing
DNS, certificate issuance usually takes about 15 minutes and can take up to 24 hours;
the `run.app` address works during that time. See
[Google's custom-domain guide](https://cloud.google.com/run/docs/mapping-custom-domains).
This demo uses Cloud Run's preview domain mapping rather than adding a separately
billed load balancer.

Check the mapping and certificate status:

```bash
gcloud beta run domain-mappings describe --domain jalabank-demo.jussialanen.com \
  --project client-jussialanen --region europe-north1 \
  --format='yaml(status.conditions,status.resourceRecords)'
```

When both `Ready` and `CertificateProvisioned` are `True`, open
<https://jalabank-demo.jussialanen.com/>. The secure session cookie and forwarded
HTTPS configuration work with either hostname. Use a direct DNS record for this
mapping; masked URL forwarding can interfere with sign-in cookies.

## Search and AI crawlers

The hosted demo sends `X-Robots-Tag: noindex, nofollow, noarchive` on application
responses, including API documentation, JSON, static assets and sign-in redirects.
The shared HTML header also contains equivalent `robots` metadata. This applies
to the custom domain and the default Cloud Run address.

`/robots.txt` is publicly available. Googlebot and Bingbot can fetch pages to read
`noindex`; other compliant crawlers, including AI search and training bots, are
disallowed from all paths. Blocking Googlebot entirely could prevent it from seeing
`noindex`, leaving an already discovered URL in search results. See
[Google's noindex guidance](https://developers.google.com/search/docs/crawling-indexing/block-indexing)
and [OpenAI's crawler controls](https://developers.openai.com/api/docs/bots).

These are crawler instructions, not access control. They cannot guarantee removal
of existing copies, block a bot that ignores them, or prevent a person from giving
a public URL to an AI tool. The public GitHub repository is separate and can still
be indexed; the hosted application's headers do not control GitHub indexing.

After deployment, verify both hostnames:

```bash
curl -I https://jalabank-demo.jussialanen.com/login
curl https://jalabank-demo.jussialanen.com/robots.txt
curl -I https://jalabank-1097769575593.europe-north1.run.app/login
```

## One-time Google Cloud setup

Install the [Google Cloud CLI](https://cloud.google.com/sdk/docs/install) and sign in
with `gcloud auth login`. Choose a project with billing enabled. You can use the
same project as JalaOps; this service has its own name and runtime identity.
Run the following from the repository root as a project administrator:

```bash
export GCP_PROJECT_ID=my-project-id
export CLOUD_RUN_REGION=europe-north1
PROJECT_NUMBER=$(gcloud projects describe "$GCP_PROJECT_ID" --format='value(projectNumber)')
RUNTIME_SA=jalabank-runtime@$GCP_PROJECT_ID.iam.gserviceaccount.com

gcloud services enable run.googleapis.com cloudbuild.googleapis.com \
  artifactregistry.googleapis.com iam.googleapis.com \
  --project "$GCP_PROJECT_ID"

# A separate runtime identity; the app needs no project IAM roles.
gcloud iam service-accounts create jalabank-runtime \
  --display-name="Jalabank demo runtime" --project "$GCP_PROJECT_ID"

# Identify the actual default build account, including in older projects.
BUILD_SA=$(gcloud builds get-default-service-account --project "$GCP_PROJECT_ID")
BUILD_SA=${BUILD_SA##*/}
gcloud projects add-iam-policy-binding "$GCP_PROJECT_ID" \
  --member="serviceAccount:$BUILD_SA" --role=roles/run.builder
```

If the APIs or build role are already configured for JalaOps, reuse them. Allow a few
minutes for new IAM permissions to propagate. The human deploying also needs
`roles/run.sourceDeveloper`, `roles/serviceusage.serviceUsageConsumer` and permission
to act as the runtime service account. The first public deploy additionally needs
permission to set the service's IAM policy (for example `roles/run.admin`). See
[source deployment permissions](https://cloud.google.com/run/docs/deploying-source-code).

## First deployment and verification

```bash
# Only release/ is uploaded; .gcloudignore excludes build output and local files.
./dev deploy --allow-unauthenticated --quiet

URL=$(gcloud run services describe jalabank \
  --project "$GCP_PROJECT_ID" --region "$CLOUD_RUN_REGION" \
  --format='value(status.url)')
curl --fail --show-error "$URL/actuator/health"
curl --fail --show-error "$URL/api/health"
```

Open `$URL/login`, choose a demo customer, complete the authenticator step, and check
the dashboard and `$URL/docs`. Cloud Run's public invocation setting lets the browser
reach the app; the app still requires its existing sign-in for private pages and API
operations. The `cloud-run` Spring profile is for HTTPS hosting; local Docker runs
continue using the default profile.

For subsequent deploys, run `./dev deploy`. Extra arguments are passed to
`gcloud run deploy`, for example `./dev deploy --quiet`. These commands always specify
the project explicitly and do not change gcloud's active project:

| Command / environment variable | Purpose / default |
|---|---|
| `./dev gcloud:status` | Print service URL and ready revision |
| `./dev gcloud:logs [n]` | Read latest log entries, default 50 |
| `GCP_PROJECT_ID` | Required project ID |
| `CLOUD_RUN_REGION` | Optional, `europe-north1` |
| `CLOUD_RUN_SERVICE` | Optional, `jalabank` |
| `CLOUD_RUN_SERVICE_ACCOUNT` | Optional, `jalabank-runtime@PROJECT_ID.iam.gserviceaccount.com` |

Keep only the three most recent Jalabank images, deleting others older than seven
days. The policy is restricted to packages starting with `jalabank`, so it does not
delete JalaOps images from a shared repository:

```bash
gcloud artifacts repositories set-cleanup-policies cloud-run-source-deploy \
  --project "$GCP_PROJECT_ID" --location "$CLOUD_RUN_REGION" \
  --policy deploy/artifact-cleanup-policy.json --no-dry-run
```

This command replaces the repository's cleanup policies. If the repository already
has policies for JalaOps, combine those with this JSON before applying it. Cleanup
runs asynchronously and does not guarantee staying within the storage free tier.

## Automatic deployment from GitHub

[`deploy.yml`](../.github/workflows/deploy.yml) deploys changes to `release/` or the
deployment configuration on `main`, after the reusable CI workflow passes. It can
also be dispatched manually on `main`. It skips deployment until all three GitHub
repository variables below are configured. CI still runs on pull requests.

Authentication uses Workload Identity Federation, with no service account key in
GitHub. Use the same identity pool as JalaOps if it already exists, but create a
separate provider restricted to this repository's `main` branch and a separate
deployment account. Run after the initial public deployment:

```bash
REPO=jussipalanen/jalabank-spring-boot
DEPLOYER=jalabank-deployer@$GCP_PROJECT_ID.iam.gserviceaccount.com

gcloud services enable iamcredentials.googleapis.com sts.googleapis.com \
  --project "$GCP_PROJECT_ID"
gcloud iam service-accounts create jalabank-deployer \
  --display-name="Jalabank GitHub deployer" --project "$GCP_PROJECT_ID"
for role in roles/run.sourceDeveloper roles/serviceusage.serviceUsageConsumer; do
  gcloud projects add-iam-policy-binding "$GCP_PROJECT_ID" \
    --member="serviceAccount:$DEPLOYER" --role="$role"
done
gcloud iam service-accounts add-iam-policy-binding "$RUNTIME_SA" \
  --project "$GCP_PROJECT_ID" --member="serviceAccount:$DEPLOYER" \
  --role=roles/iam.serviceAccountUser
gcloud iam service-accounts add-iam-policy-binding "$BUILD_SA" \
  --project "$GCP_PROJECT_ID" --member="serviceAccount:$DEPLOYER" \
  --role=roles/iam.serviceAccountUser

# Skip this command if the github pool already exists for JalaOps.
gcloud iam workload-identity-pools create github --location=global \
  --display-name="GitHub Actions" --project "$GCP_PROJECT_ID"
gcloud iam workload-identity-pools providers create-oidc jalabank \
  --project "$GCP_PROJECT_ID" --location=global --workload-identity-pool=github \
  --issuer-uri=https://token.actions.githubusercontent.com \
  --attribute-mapping=google.subject=assertion.sub,attribute.repository=assertion.repository,attribute.ref=assertion.ref \
  --attribute-condition="assertion.repository == '$REPO' && assertion.repository_id == '665058949' && assertion.ref == 'refs/heads/main' && assertion.actor_id == '4532255'"
gcloud iam service-accounts add-iam-policy-binding "$DEPLOYER" \
  --project "$GCP_PROJECT_ID" --role=roles/iam.workloadIdentityUser \
  --member="principalSet://iam.googleapis.com/projects/$PROJECT_NUMBER/locations/global/workloadIdentityPools/github/attribute.repository/$REPO"

# Repository variables, not secrets.
gh variable set GCP_PROJECT_ID --repo "$REPO" --body "$GCP_PROJECT_ID"
gh variable set GCP_SERVICE_ACCOUNT --repo "$REPO" --body "$DEPLOYER"
gh variable set GCP_WORKLOAD_IDENTITY_PROVIDER --repo "$REPO" \
  --body "projects/$PROJECT_NUMBER/locations/global/workloadIdentityPools/github/providers/jalabank"
```

The workflow keeps the service public without changing its IAM policy on every
deploy. A failed build or test prevents deployment. To use a different region,
service name or runtime identity in automation, set the matching `CLOUD_RUN_*`
environment variables in the deploy job as well as in your local shell.
For another repository or owner, replace the repository and actor IDs in both the
provider condition and workflow. Use `gh api repos/OWNER/REPO --jq .id` and
`gh api users/OWNER --jq .id` to look up those immutable IDs.

## Troubleshooting

- **Build permissions:** check `gcloud builds get-default-service-account`, grant
  `roles/run.builder` to that account, and wait for IAM propagation.
- **GitHub sign-in fails:** check the three repository variables, provider repository
  and branch conditions, and the deployer's Workload Identity User binding.
- **Cloud Run returns 403 before the login page:** perform the first deployment with
  `--allow-unauthenticated` as an administrator allowed to update service IAM.
- **Redirects or sign-in fail:** use HTTPS and verify `SPRING_PROFILES_ACTIVE=cloud-run`.
- **Slow first visit or expired session:** scale-to-zero and restarts are expected;
  wait for startup and sign in again.
