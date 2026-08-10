# Workspace — Spring LMS Review & Migration

## Current workspace

- **Repo**: `spring-lms` (Java 21, Spring Boot 4.1.0)
- **Graph**: 330 nodes, 1731 edges, risk 0.00, built on `develop` branch
- **Review completed**: `docs/reviews/2026-08-review.md` (full report with file:line citations)

## Key dependencies

| Artifact | Version | Purpose |
|----------|---------|---------|
| Spring Boot | 4.1.0 | Boot 4 / Boot 3 migration |
| Jackson | 3 (via Spring Boot) | JSON serialization |
| JPA | 3.2 | Jakarta Persistence |
| Redis | 6 | Caching/sessions |
| Liquibase | current | DB migrations |
| Resilience4j | current | Circuit breaker + rate limiter |
| Springdoc | 3.0.2 | OpenAPI docs |
| jjwt | 0.13.0 | JWT tokens |

## Architecture style

**Layered architecture** (controller → service → repository → domain). The project has not yet adopted DDD or the Tomato pattern. The graph confirms **high coupling** between controller-controller and service-class (26 edges) — keep service layer as the single choke-point.

## Review methodology

**Pass A → F**: Build/config → API correctness → Architecture → Data access → Security → Performance/resilience.

**References used** (only when relevant):
- `spring-boot-4-patterns.md` (Jackson 3 migration, Test annotations, retry)
- `java-25-features.md` (virtual threads, patterns — not applicable to Java 21)
- `security-checklist.md` (OWASP, JWT, injection, secrets)
- `performance-patterns.md` (N+1, pagination, projection, caching)
- `architecture-patterns.md` (layered vs package-by-module vs DDD)
- `jspecify-null-safety.md` (null-safety baseline)

## Worklist (pending review)

- [ ] LF line-ending normalization (`.gitattributes`, IntelliJ config)
- [ ] Consumer service repo build and docs
- [ ] Spring Boot 4 migration checklist (all 25 items from the skill)
- [ ] Unit tests added per review finding (high-priority: audit, security, data access)
- [ ] Full end-to-end demo run (after each pass is closed)

## Notes

- **Working tree is dirty** — many uncommitted files (WIP, formatting, docs). Use `git add --renormalize .` before committing.
- **Graph is stale** — built on `HEAD~1` (3904f1d6b694569c344b376e22b2c6598305bfa9), not `HEAD` (3acc0c181c71543d83812ca66d35cb21614d9de6). Findings from the graph may be slightly stale; actual file reads are authoritative.