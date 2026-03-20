# Challenge 3 — The Map: Decomposition Plan

## Strategy: Strangler Fig

Extract services incrementally behind the existing REST interface. The monolith remains fully operational throughout. New services are proven in parallel; the monolith is "strangled" by redirecting traffic over time.

---

## Identified Seams

| Seam                      | What it Does                                    | Coupling                               | Extraction Candidate                          |
| ------------------------- | ----------------------------------------------- | -------------------------------------- | --------------------------------------------- |
| **Album CRUD**            | Create/read/update/delete albums via REST       | Low — pure data, no side effects       | `album-service`                               |
| **CF Service Discovery**  | Detects bound CF services, sets Spring profiles | High — cross-cutting, lifecycle hook   | Leave in monolith                             |
| **Observability / Info**  | Exposes active profiles and CF service info     | Medium — reads env only, no DB         | `platform-info-service` (later)               |
| **Data Seeding**          | Loads albums.json on first startup              | Low — one-time init, no business logic | Move with album-service                       |
| **Multi-backend routing** | Profile-driven repository selection             | High — baked into Spring wiring        | Phase out with album-service (single backend) |

---

## Service Extraction Order

### Cut 1: `album-service` (Implemented — Challenge 5)

**Why first:** Highest value, lowest risk. The Album domain is completely self-contained — no outbound dependencies, no shared tables (in default H2 mode), clean REST boundary already exists. This is the "seam" that most closely resembles a microservice.

**What moves:**

- `Album` entity → `AlbumEntity` (service-owned, no monolith import)
- `JpaAlbumRepository`, `MongoAlbumRepository`, `RedisAlbumRepository` → service starts with JPA/H2 only
- `AlbumController` → replicated in service, monolith kept intact (strangler: both exist)
- `AlbumRepositoryPopulator` → moves with the service

**Extraction Risk: LOW**

- No circular dependencies
- No shared state between Album and other domains
- Existing MockMvc tests act as characterization net

---

### Cut 2: `recommendation-service` (Implemented — Challenge 9)

**Why second:** Demonstrates event-driven communication. The recommendation service is net-new capability (not extracted from the monolith), but it consumes `AlbumCreated` events from album-service, establishing the event bus pattern.

**What it adds:**

- `AlbumCreatedEvent` published by album-service on every PUT
- `RecommendationService` consumes events, maintains a "recently added" list
- Dual-write problem documented; Outbox pattern prescribed

**Extraction Risk: LOW** (new capability, not an extraction)

---

### Cut 3: `platform-info-service` (Future — not implemented)

**Why third:** The `InfoController` + `SpringApplicationContextInitializer` are CF-specific. Abstracting them removes the last platform dependency from the album domain.

**What moves:**

- `/appinfo` and `/service` endpoints
- CF environment detection logic
- Dependency on `java-cfenv-boot`

**Extraction Risk: MEDIUM**

- Requires the other services to register themselves (service registry or sidecar pattern)
- CF-specific logic is not portable to non-CF deployments without adaptation

---

## Service Extraction Risk Ranking

| Rank        | Service                                     | Risk   | Reason                                                                   |
| ----------- | ------------------------------------------- | ------ | ------------------------------------------------------------------------ |
| 1 (lowest)  | `album-service`                             | Low    | Self-contained domain, clean REST boundary, no shared state              |
| 2           | `recommendation-service`                    | Low    | Net-new; no existing behaviour to preserve                               |
| 3           | `platform-info-service`                     | Medium | Cross-cutting; other services must report to it                          |
| 4           | `SpringApplicationContextInitializer` logic | High   | Lifecycle hook; hardcoded profile detection; all services depend on it   |
| 5 (highest) | Multi-backend routing                       | High   | Deep Spring wiring; removing it changes the app's core value proposition |

---

## Anti-Corruption Layer (ACL) Design

The monolith's `Album` JPA entity must not leak into extracted services. See `decisions/ADR-002-acl-pattern.md` for the full decision.

```
Monolith                    ACL                    album-service
Album (JPA entity)  <-->  AlbumPayload (DTO)  <-->  AlbumEntity (service JPA)
  @Entity                  {id, title, artist,        @Entity
  @GeneratedValue           releaseYear, genre,        (service-owned schema)
  jakarta.persistence       trackCount}
```

---

## "Done" Definition (per the CLAUDE.md hint)

Challenge 5 is done when:

1. `album-service` starts independently (`./gradlew :album-service:bootRun` on port 8081)
2. The monolith starts independently on port 8080 unchanged
3. `AlbumControllerTest` still passes against the monolith
4. `AlbumContractTest` passes against album-service
5. Both API surfaces satisfy the same contract (`AlbumPayload` schema)
