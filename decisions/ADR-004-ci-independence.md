# ADR-004: Independent CI/CD Pipelines

**Status:** Accepted
**Date:** 2026-03-20
**Deciders:** Team 17

---

## Context

The monolith and `album-service` must be deployable independently. A failure in one pipeline must not block the other. This is a core principle of microservice architecture — teams should be able to ship independently.

## Decision

Three separate GitHub Actions workflows:

| Workflow          | File                                           | Trigger                                                        | What it builds         |
| ----------------- | ---------------------------------------------- | -------------------------------------------------------------- | ---------------------- |
| Monolith CI       | `.github/workflows/monolith.yml`               | Push/PR touching `src/**`, `build.gradle`, `gradle.properties` | Spring-music monolith  |
| Album Service CI  | `.github/workflows/album-service.yml`          | Push/PR touching `album-service/**`                            | album-service          |
| Recommendation CI | `.github/workflows/recommendation-service.yml` | Push/PR touching `recommendation-service/**`                   | recommendation-service |

Each workflow:

1. Runs on `ubuntu-latest`
2. Sets up Java 17
3. Runs `./gradlew clean test` (or equivalent)
4. Uploads test results as artifacts

## Path Filtering

Workflows use `paths:` filters so a change to album-service does not trigger the monolith pipeline:

```yaml
on:
  push:
    paths:
      - "album-service/**"
      - ".github/workflows/album-service.yml"
```

## Consequences

**Positive:**

- A broken monolith build does not block album-service deployment
- Developers can reason about which pipeline owns which code
- Path filtering reduces unnecessary CI runs

**Negative:**

- Cross-cutting changes (e.g., shared gradle wrapper update) may need to trigger multiple pipelines
- No integration test pipeline yet — contract tests run within each service's own CI

## Future: Integration Gate

When album-service goes to production, add an integration workflow that runs contract tests against both services together. This gate runs only before deployment, not on every commit.
