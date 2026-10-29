# Roadmap

Jalabank stays a small, database-free demo bank. Upcoming features should work
with seeded in-memory data, keep customer data isolated and fit the existing
low-cost Cloud Run service. This list describes planned work, not promises of
delivery dates. Completed changes belong in [CHANGELOG.md](CHANGELOG.md).

## Current 2026 generation

- Java 21 and Spring Boot 4 rebuild with decimal money calculations and seeded
  in-memory customers and transactions.
- Responsive web UI, customer balances, scheduled transactions, pagination,
  transaction creation and single/bulk deletion.
- Customer sign-in with the demo hourly authenticator and session-scoped REST API.
- OpenAPI documentation, public health checks, unit and web tests, and Docker tools.
- Low-cost Cloud Run hosting, keyless owner-restricted deployment and a custom domain.

## Release preparation

- [x] Establish the changelog and semantic versioning policy.
- [x] Add search-engine `noindex` headers and HTML metadata, with public crawler rules.
- [x] Add shared project instructions for coding tools.
- [x] Document annotated tags and changelog-based GitHub releases.

## Upcoming features

- [ ] Add multiple UI languages through message bundles, including translated
  navigation, forms, validation messages and demo sign-in screens.
- [ ] Add a language switcher to the navigation, remember the visitor's choice
  across page loads, and format dates and amounts for the selected language.
- [ ] Search transactions by message and filter by date range, amount and type.
- [ ] Add optional transaction categories with seeded examples and monthly summaries.
- [ ] Export the signed-in customer's monthly statement as CSV.
- [ ] Confirm new transactions with the existing demo authenticator flow.
- [ ] Show the in-memory data reset behavior clearly in the web UI.

## Quality and usability

- [ ] Add browser coverage for sign-in, transaction creation/deletion and sign-out.
- [ ] Review keyboard navigation, focus handling, form errors and screen-reader labels.
- [ ] Expand mobile coverage for transaction tables and the authenticator pop-up/tab.
- [ ] Add a documented display-time-zone setting for dates and authenticator times.
- [ ] Measure cold-start time and memory use before changing Cloud Run resource limits.

## Maintenance

- [ ] Keep dependencies and pinned GitHub Actions current through reviewed PRs.
- [ ] Keep release notes and the application version aligned for each release.
- [ ] Recheck live indexing directives and production access restrictions after hosting changes.

The 2023 PostgreSQL prototype is legacy history. It is not the baseline for new
features, and restoring a database is outside the current demo scope.
