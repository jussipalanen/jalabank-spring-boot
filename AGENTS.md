# Project instructions

## Scope and architecture

Jalabank is a public demo bank built with Java 21, Spring Boot 4, Thymeleaf,
Spring Security and a REST API. The Maven application lives in `release/`.
Customers, transactions, authenticator codes and sessions are in memory and reset
on restart. Keep this demo database-free unless the user explicitly changes that
scope. The hourly authenticator is a demo, not an external MFA integration.

The hosted app runs as one low-cost Cloud Run service in `europe-north1` with zero
minimum instances, a maximum of one instance and request-based billing. Preserve
these defaults. See `docs/DEPLOYMENT.md` for the configured project and identities.
Render is no longer used.

## Commands and validation

From the repository root:

```bash
cd release
./mvnw --batch-mode --no-transfer-progress verify
```

Java 21 is required; the Maven wrapper is checked in. Use `./dev up`, `./dev down`,
`./dev logs` and `./dev test` for the local Docker workflow. The production image
is built with `docker build release`; its build stage also runs the tests.

For shell changes, run `bash -n dev` and `bash -n scripts/deploy-cloud-run.sh`.
For workflow changes, run `actionlint` when available. Run `git diff --check`
before committing. Match validation to the change; documentation-only edits do
not require an application rebuild.

`GCP_PROJECT_ID=client-jussialanen ./dev deploy` deploys the working copy.
`./dev gcloud:status` and `./dev gcloud:logs` also require `GCP_PROJECT_ID`.
Production changes must be within the user's authorized task.

## Code and behavior

- Follow the existing Java package structure and formatting.
- Use `BigDecimal` for money and validate transaction inputs.
- Keep balances, transaction access and mutations scoped to the signed-in
  customer. Preserve the existing CSRF and session protections.
- Keep API and HTML errors consistent with the existing application.
- Keep the health endpoints public for Cloud Run startup checks.
- Preserve global `X-Robots-Tag: noindex` and the shared HTML robots metadata.
  Serve `/robots.txt` without sign-in. Google and Bing must be able to read
  `noindex`; other compliant crawlers, including AI bots, are disallowed.
- Do not describe crawler directives as a guarantee against public access or
  indexing of the separate public GitHub repository.
- Use English for documentation and code comments. The current UI is English;
  upcoming localization and its language switcher are tracked in `ROADMAP.md`.

## Branches and repository access

Start work from current `main` on a `feature/`, `fix/`, `docs/` or `chore/` branch.
Inspect local changes first and preserve work outside the current task. Stage
only the files changed for the task.

The repository is public. Only `@jussipalanen` may update `main`; existing
collaborators may work on feature branches. Main updates require a PR, passing
`Build and test` CI, an up-to-date branch and resolved review conversations.
Do not bypass these requirements, enable unattended auto-merge, grant new access,
or loosen the production federation conditions. Follow `docs/REPOSITORY.md`.

The deployment workflow is restricted to the owner's original actor ID and rerun
actor. The Google provider also restricts the repository, immutable repository
ID, `main` and owner actor ID. Keep GitHub tokens read-only except for the
deployment job's OIDC permission. Never use `pull_request_target` to execute
untrusted PR code with credentials.

## Versioning, changelog and releases

Use `MAJOR.MINOR.PATCH`: a breaking change increments major, a compatible feature
increments minor, and a fix increments patch. Tags use `vMAJOR.MINOR.PATCH`.
Keep the application version in `release/pom.xml`, the release heading in
`CHANGELOG.md`, and the published tag consistent.

Record user-visible changes in `CHANGELOG.md` and maintain `ROADMAP.md` for
upcoming work. The substantial 2026 rebuild is the current generation; keep the
2023 prototype grouped as legacy snapshot history rather than inventing stable
historical releases or tags.

When a release is requested, use an annotated tag on the exact reviewed release
commit and use the corresponding `CHANGELOG.md` section for GitHub release notes.
Check existing tags and releases first. Do not move published tags or invent a
new version unrelated to the application version.

## Documentation

`CLAUDE.md` imports this file; maintain instructions here instead of duplicating
them. Update `README.md`, `docs/DEPLOYMENT.md` and `docs/REPOSITORY.md` when their
documented behavior changes. Keep hosting examples consistent with Cloud Run.
Never commit credentials, service-account keys, `.env` files or secret values.
