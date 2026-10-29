# Changelog

Versions use `MAJOR.MINOR.PATCH`: breaking changes increment the major version
(for example `1.0.0`), compatible features increment the minor version (`0.1.0`),
and fixes increment the patch version (`0.0.1`). Tags use a `v` prefix.

The current generation is the substantial 2026 rebuild. The 2023 application is
legacy history. Earlier version milestones below were reconstructed from the Git
history and Maven versions; they were not previously published GitHub releases.

## [Unreleased]

## [2.2.0] - 2026-10-30

First published release of the 2026 Jalabank generation. This includes the major
rebuild introduced in 2.0.0: Java 21, Spring Boot 4, database-free demo data,
decimal money calculations, validated forms and a tested REST API. The 2023
PostgreSQL-backed application is retained only in Git history.

### Added

- Two-step customer sign-in with hourly six-digit authenticator codes, a code
  pop-up, a two-minute grace period and a five-attempt limit.
- Customer-scoped sessions and API access, including `/api/v1/customers/me`.
- Low-cost Cloud Run hosting in Finland with zero minimum instances, one maximum
  instance, 512 MiB memory and request-based billing; no database service.
- Keyless GitHub deployment from `main` after CI passes, separate runtime and
  deployment identities, and scoped image cleanup.
- Manual deployment and inspection commands: `./dev deploy`,
  `./dev gcloud:status` and `./dev gcloud:logs`.
- The `jalabank-demo.jussialanen.com` custom domain with Google-managed HTTPS.
- Shared project instructions in `AGENTS.md`, imported by `CLAUDE.md`, and an
  upcoming-feature roadmap in `ROADMAP.md`.
- A public `robots.txt` that blocks compliant AI search/training crawlers and
  other general crawlers while allowing Google and Bing to read `noindex`.

### Changed

- Responsive navigation, transaction pages and authenticator flow for phones.
- HTTPS-aware redirects and secure session cookies on Cloud Run.
- Only the repository owner can update `main`; PRs and successful CI are
  mandatory. Production federation is restricted to this repository, `main`
  and the owner's GitHub identity.

### Fixed

- All application responses now send `X-Robots-Tag: noindex, nofollow, noarchive`,
  including API documentation, redirects, authentication errors and static files.
- Shared HTML page metadata also marks the demo as `noindex`.

### Removed

- Obsolete Render hosting configuration and instructions.

## Historical 2026 milestones

### 2.1.0 - 2026-10-08

- Dark blue Bootstrap UI with balances, monthly totals, scheduled transactions
  and recent transactions.
- Single and bulk transaction deletion with confirmation, and paginated API
  responses and transaction lists.
- Generated OpenAPI documentation and Swagger UI at `/docs`, plus application
  and actuator health endpoints.
- Customer-specific cumulative balances and correct treatment of future-dated
  transactions.

### 2.0.0 - 2026-10-08 — major rebuild

- Rebuilt the legacy application with Java 21 and Spring Boot 4, and renamed it
  from Boobank to Jalabank.
- Replaced PostgreSQL/JPA and SQL dumps with in-memory customer and transaction
  repositories seeded with approximately six months of demo data.
- Reworked money calculations with `BigDecimal`, input validation and API errors.
- Bundled frontend assets locally and added unit and web tests.
- Modernized Docker builds with a non-root Java runtime and container memory
  settings, and added the `./dev` Docker helper.

This was a breaking generation change: the persistent database setup and old
application packages were removed. Demo changes now reset on restart.

## 2023 legacy — 0.0.1-SNAPSHOT

The July 2023 Boobank prototype used Java 17, Spring Boot 3.1, PostgreSQL and JPA.
It provided transaction forms, monthly and cumulative balances, pagination,
filtering, a REST API, and a Docker Compose database setup. It was a development
snapshot, not a published stable `0.0.1` release.

Legacy source: [last 2023 commit](https://github.com/jussipalanen/jalabank-spring-boot/tree/217ae99).

[Unreleased]: https://github.com/jussipalanen/jalabank-spring-boot/compare/v2.2.0...HEAD
[2.2.0]: https://github.com/jussipalanen/jalabank-spring-boot/releases/tag/v2.2.0
