# Team 17 — Spring Music Modernization & Decomposition

## Participants

- Charan Raghupatruni (PM, Architect, Developer, Tester, Infra)

## Scenario

Scenario 1: Code Modernization — "The Monolith"

Used the optional spring-music starter repo. Skipped Challenge 2 (The Patient) as instructed.

---

## What We Built

Starting from a Spring Boot 2.4.0 legacy app, we completed two phases of work.

**Phase 1 — Modernization (pre-existing):** Full migration to Spring Boot 3.2.3 / Java 17. Every breaking change resolved: javax→jakarta, jcenter removal, Gradle 8.5, Lombok adoption, modern Spring annotations, JUnit 5. Six MockMvc integration tests added as a behavioral baseline.

**Phase 2 — Decomposition (this session):** Strangler-fig extraction of the Album domain into a standalone `album-service` (port 8081), a new `recommendation-service` (port 8082) that receives album events, characterization tests that pin the monolith's quirks, contract tests that verify both the monolith and album-service satisfy the same API shape, three independent CI/CD pipelines, and a full cutover runbook with a 3am decision tree.

What runs: monolith (H2), album-service (H2), recommendation-service (in-memory). All three build and start independently.
What's scaffolding: CI/CD workflows written but not pushed to GitHub. Recommendation event relay is at-most-once (outbox pattern documented, not implemented).

---

## Challenges Attempted

| #   | Challenge      | Status  | Notes                                                                                                                                                                                 |
| --- | -------------- | ------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 1   | The Stories    | done    | 3 user stories with executable acceptance criteria in `decisions/01-user-stories.md`                                                                                                  |
| 2   | The Patient    | skipped | Used spring-music starter as instructed                                                                                                                                               |
| 3   | The Map        | done    | Decomposition plan, seam analysis, extraction risk ranking in `decisions/02-decomposition-map.md`                                                                                     |
| 4   | The Pin        | done    | 9 characterization tests pinning monolith quirks (200+empty on unknown id, seeded count, etc.)                                                                                        |
| 5   | The Cut        | done    | `album-service` extracted, runs on port 8081, monolith unchanged on 8080                                                                                                              |
| 6   | The Fence      | done    | `AlbumPayload` ACL DTO, `AlbumTranslator` — monolith's `Album` entity never imported in album-service                                                                                 |
| 7   | The Contract   | done    | Provider-side (`AlbumServiceContractTest`) + consumer-side (`MonolithAlbumContractTest`). Divergence documented.                                                                      |
| 8   | The Pipeline   | done    | 3 independent GitHub Actions workflows with path filtering. Monolith failure does not block service builds.                                                                           |
| 9   | The Second Cut | done    | `recommendation-service` on port 8082 receives `AlbumCreatedEvent` via `@TransactionalEventListener` + HTTP relay. Dual-write problem documented in ADR-003. Idempotency implemented. |
| 10  | The Weekend    | done    | Cutover runbook: 5-step strangler fig traffic shift, rollback triggers, 3am decision tree, data migration script                                                                      |

---

## Key Decisions

**Strangler fig over big-bang rewrite** — Monolith stays live. New services proven in parallel. Traffic shifted incrementally. One routing rule change = instant rollback. See `decisions/ADR-001-strangler-fig.md`.

**ACL at every service boundary** — `AlbumPayload` is the only shared contract. Neither service imports the other's domain objects. The fence is enforced by the package structure: `album-service` has zero imports from `org.cloudfoundry.samples.music.*`. See `decisions/ADR-002-acl-pattern.md`.

**Outbox pattern prescribed for events** — The current event relay is at-most-once (logged and accepted). ADR-003 documents the outbox pattern as the production-ready solution, using only the H2 database already present (no Kafka dependency).

**Three independent CI pipelines** — Path-filtered GitHub Actions. A broken monolith does not block album-service deployment and vice versa. See `decisions/ADR-004-ci-independence.md`.

---

## How to Run It

Requires: Java 17, Gradle 8.5 (or `./gradlew` wrapper for monolith)

```bash
# Terminal 1 — Monolith (port 8080, H2 in-memory)
cd spring-music
./gradlew clean assemble
java -jar build/libs/spring-music-1.0.jar

# Terminal 2 — album-service (port 8081, H2 in-memory)
cd spring-music/album-service
gradle clean assemble
java -jar build/libs/album-service-1.0.jar

# Terminal 3 — recommendation-service (port 8082, in-memory)
cd spring-music/recommendation-service
gradle clean assemble
java -jar build/libs/recommendation-service-1.0.jar

# Verify monolith
curl http://localhost:8080/albums | jq length          # 29 seeded albums

# Verify album-service
curl http://localhost:8081/albums | jq length          # 0 (no seed data; add via PUT)
curl -X PUT http://localhost:8081/albums \
  -H "Content-Type: application/json" \
  -d '{"title":"Dark Side","artist":"Pink Floyd","releaseYear":"1973","genre":"Rock","trackCount":10}'

# Verify recommendation-service received the event
curl http://localhost:8082/recommendations | jq .

# Run all monolith tests (characterization + contract + existing)
cd spring-music && ./gradlew test

# Run album-service contract tests
cd spring-music/album-service && gradle test
```

---

## If We Had Another Day

1. **Outbox pattern** for album events — replace at-most-once relay with guaranteed at-least-once delivery using the outbox table in H2
2. **Docker Compose** — one command to start all three services + seed data migration
3. **API Gateway** — nginx or Spring Cloud Gateway to implement the strangler fig routing rule
4. **`@ResponseStatus(NOT_FOUND)`** on monolith's `getById` — align the 200+empty quirk with album-service's 404 (currently pinned by contract test, not yet fixed)
5. **Platform info service extraction** — remove CF dependency from the album domain entirely

---

## How We Used Claude Code

- **Codebase archaeology first** — scanned all files before writing a line; the characterization tests were written by asking Claude to enumerate every behavioral quirk it could find
- **ADRs as prompts** — wrote the ADR decision question first, let Claude draft the trade-off analysis, then edited for accuracy; architecture thinking scaled to a single-person team
- **Parallel service scaffolding** — Claude wrote album-service + recommendation-service + 3 CI workflows + runbook in a single session; tasks that would normally require 3 engineers for half a day
- **Contract as the forcing function** — asking Claude "how do you prove both sides satisfy the same contract" produced the two-sided test structure (AlbumServiceContractTest + MonolithAlbumContractTest) and surfaced the 404 vs 200 divergence as a documented decision, not a hidden bug
- **CLAUDE.md as living architecture doc** — every structural decision written into CLAUDE.md so the next session has full context without reading git history
