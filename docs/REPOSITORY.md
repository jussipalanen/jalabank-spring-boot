# Public repository and production access

This repository is public. Visitors can read, fork and propose pull requests.
Only `@jussipalanen` (GitHub user ID `4532255`) can update or merge into `main`.
Existing collaborators may still push feature branches; their access does not
permit updating `main`.

## Active main protections

Two independent protections are configured in GitHub:

- The owner-only branch ruleset restricts updates to `refs/heads/main`. Its only
  bypass actor is the specific user `@jussipalanen`; collaborators, GitHub Actions
  and other apps are not bypass actors.
- Classic branch protection requires a pull request, an up-to-date branch, a
  successful `Build and test` check from GitHub Actions (app ID `15368`), and
  resolved review conversations. This also applies to the owner. Force pushes
  and deletion of `main` are disabled.

The required approval count is zero, so the sole maintainer can merge their own
passing PR. `CODEOWNERS` records ownership without requiring self-approval.
Automatic PR merging is disabled. GitHub Actions uses a read-only token by default
and cannot approve PRs.

The API configurations are checked in as
[main-owner-ruleset.json](../.github/main-owner-ruleset.json) and
[main-branch-protection.json](../.github/main-branch-protection.json).
Changing these files does not itself change the live GitHub configuration.

To recreate the protections on this repository as the owner:

```bash
# Create once. To update an existing ruleset, use PUT with its ID instead.
gh api --method POST repos/jussipalanen/jalabank-spring-boot/rulesets \
  -H 'X-GitHub-Api-Version: 2026-03-10' \
  --input .github/main-owner-ruleset.json
gh api --method PUT repos/jussipalanen/jalabank-spring-boot/branches/main/protection \
  --input .github/main-branch-protection.json
```

## Production deployment

The deploy workflow runs only on `main` for actor ID `4532255`, including manual
dispatches. It also requires `github.triggering_actor` to be `jussipalanen` in both
the CI and deployment jobs, so another collaborator cannot redeploy by rerunning
an owner's previous workflow run. Pull requests run CI with read-only repository permissions and never
receive Google credentials. The production deployment job is the only job that
requests an OIDC token.

Google's Workload Identity provider independently requires this repository's name
and immutable ID (`665058949`), `refs/heads/main`, and actor ID `4532255`. A fork,
feature branch or another collaborator's manual run cannot assume the deployer
identity. Credentials use short-lived federation tokens, with no service account
key in GitHub. The account owner can still deploy locally with `./dev deploy`
using their own Google Cloud login. Other project administrators could change
Google Cloud permissions independently of this repository.

## Dependency updates

[Dependabot](../.github/dependabot.yml) checks Maven dependencies and Docker base
images in `release/`, and GitHub Actions in `.github/workflows/`, every Monday at
09:00 Europe/Helsinki time. Minor and patch updates are grouped per ecosystem;
major updates open separate PRs. Open version-update PRs are limited to five for
Maven and three each for Docker and Actions.

Dependabot alerts and security-update PRs are enabled separately in GitHub.
Security fixes are not held until the weekly version-update schedule. Dependabot
only proposes changes: the existing CI requirements and owner-only `main` rule
still apply. No automatic merging or extra bypass permissions are granted.

## Verify live settings

```bash
gh api repos/jussipalanen/jalabank-spring-boot --jq .visibility
gh api repos/jussipalanen/jalabank-spring-boot/rulesets
gh api repos/jussipalanen/jalabank-spring-boot/rules/branches/main
gh api repos/jussipalanen/jalabank-spring-boot/branches/main/protection
gh api repos/jussipalanen/jalabank-spring-boot/collaborators
gh api repos/jussipalanen/jalabank-spring-boot/invitations
gh api repos/jussipalanen/jalabank-spring-boot/keys
gcloud iam workload-identity-pools providers describe jalabank \
  --project client-jussialanen --location global --workload-identity-pool github
```

See GitHub's [ruleset API](https://docs.github.com/en/rest/repos/rules) and
[available branch rules](https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-rulesets/available-rules-for-rulesets).
