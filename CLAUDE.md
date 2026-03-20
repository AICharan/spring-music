# CLAUDE.md — Spring Music (Modernized)

Legacy Spring Boot app modernized from 2.4.0 → 3.2.x as part of the APAC Claude Code Workshop Hackathon (Team 17).

## Project Purpose

Demonstrates two phases of work: (1) Spring Boot 2.4.0 → 3.2.3 modernization (javax→jakarta, Lombok, Gradle 8.5, JUnit 5), and (2) strangler-fig decomposition into microservices. The original monolith stores albums across multiple persistence backends (H2/MySQL/Postgres/MongoDB/Redis). Extracted services run independently on H2.

## Stack

- **Runtime**: Java 17, Spring Boot 3.2.3
- **Build**: Gradle 8.5 (plugins block DSL)
- **Persistence**: JPA/H2 (default), MySQL, Postgres, MongoDB, Redis
- **Frontend**: AngularJS 1.x (legacy — unchanged)
- **Test**: JUnit 5 + Spring MockMvc

## Quick Start

```bash
# Monolith (port 8080, H2 default)
./gradlew clean assemble
java -jar build/libs/spring-music-1.0.jar

# album-service (port 8081)
cd album-service && gradle clean assemble
java -jar build/libs/album-service-1.0.jar

# recommendation-service (port 8082)
cd recommendation-service && gradle clean assemble
java -jar build/libs/recommendation-service-1.0.jar

# Run with a specific DB profile (monolith only)
java -jar -Dspring.profiles.active=mysql build/libs/spring-music-1.0.jar

# Run all monolith tests (characterization + contract + integration)
./gradlew test

# Run album-service contract tests
cd album-service && gradle test

# Run a single test class
./gradlew test --tests "org.cloudfoundry.samples.music.characterization.AlbumApiCharacterizationTest"
```

## Repository Structure

```
spring-music/
├── src/                          ← Monolith (unchanged from modernization)
├── album-service/                ← Challenge 5/6: extracted service (port 8081)
│   └── src/.../albumservice/
│       ├── acl/AlbumPayload.java      ← API contract DTO (the fence)
│       ├── acl/AlbumTranslator.java   ← Only place that maps payload ↔ entity
│       ├── domain/AlbumEntity.java    ← Service-owned JPA entity (NOT Album.java)
│       ├── event/AlbumCreatedEvent.java
│       ├── event/EventRelayService.java  ← @TransactionalEventListener → HTTP relay
│       └── api/AlbumController.java
├── recommendation-service/       ← Challenge 9: event consumer (port 8082)
│   └── src/.../recommendations/
│       ├── event/AlbumCreatedPayload.java  ← Service-local event DTO (not imported from album-service)
│       ├── domain/Recommendation.java
│       └── api/EventController.java        ← POST /events/album-created, GET /recommendations
├── decisions/                    ← ADRs + challenge deliverables
│   ├── 01-user-stories.md        ← Challenge 1
│   ├── 02-decomposition-map.md   ← Challenge 3
│   ├── ADR-001-strangler-fig.md
│   ├── ADR-002-acl-pattern.md
│   ├── ADR-003-event-driven-second-cut.md
│   ├── ADR-004-ci-independence.md
│   └── 10-cutover-runbook.md     ← Challenge 10
├── .github/workflows/
│   ├── monolith.yml              ← Challenge 8: path-filtered, independent
│   ├── album-service.yml
│   └── recommendation-service.yml
├── presentation.html             ← 5-minute HTML deck for judging
└── README.md                     ← Filled-in hackathon template
```

## Key Files (Monolith)

| File                                                              | Role                                                         |
| ----------------------------------------------------------------- | ------------------------------------------------------------ |
| `src/.../domain/Album.java`                                       | JPA entity — Lombok, jakarta.persistence                     |
| `src/.../web/AlbumController.java`                                | REST CRUD — @GetMapping/PostMapping/PutMapping/DeleteMapping |
| `src/.../config/SpringApplicationContextInitializer.java`         | CF profile detection                                         |
| `src/test/.../characterization/AlbumApiCharacterizationTest.java` | Challenge 4 — pins 9 behavioral quirks                       |
| `src/test/.../contract/MonolithAlbumContractTest.java`            | Challenge 7 consumer side                                    |

## Modernization Changes (What Was Fixed)

### Breaking / Correctness

- `javax.persistence.*` → `jakarta.persistence.*` (required for Spring Boot 3.x / Jakarta EE 10)
- `javax.validation.Valid` → `jakarta.validation.Valid`
- `spring.profiles:` → `spring.config.activate.on-profile:` (deprecated since 2.4, removed in 3.x)
- `com.mysql.jdbc.Driver` → `com.mysql.cj.jdbc.Driver` (legacy driver removed from MySQL 8+)
- `MySQL55Dialect` / `ProgressDialect` → `MySQLDialect` / `PostgreSQLDialect` (renamed in Hibernate 6)

### Build

- `buildscript {}` + `apply plugin:` → `plugins {}` block (modern Gradle DSL)
- Removed `jcenter()` repository (shut down 2022)
- Gradle 6.7 → 8.5
- `mysql:mysql-connector-java` → `com.mysql:mysql-connector-j` (artifact renamed)
- Added Lombok (eliminates ~60 lines of boilerplate getters/setters)
- JUnit 4 → JUnit 5 (via `useJUnitPlatform()`)

### Code Quality

- `@RequestMapping(method = RequestMethod.GET)` → `@GetMapping` (all HTTP method variants)
- Logger string concatenation → `{}` placeholder format (avoids unnecessary string allocation)
- Raw `CrudRepository` usages → properly typed `CrudRepository<Album, String>`
- `ArrayList` loop → `stream().toArray()` in InfoController

## Patterns & Conventions

- Use `@GetMapping`/`@PostMapping`/`@PutMapping`/`@DeleteMapping` — never generic `@RequestMapping` with method param
- Logger calls always use `{}` placeholders: `logger.info("Message {}", var)` not concatenation
- Tests use `@BeforeEach` to reset repository state for isolation
- All persistence profiles are activated via `spring.config.activate.on-profile` in application.yml

## Profile Reference

| Profile    | Backend      | Notes                                        |
| ---------- | ------------ | -------------------------------------------- |
| (none)     | H2 in-memory | Default for local dev                        |
| `mysql`    | MySQL        | Requires running MySQL on localhost/music    |
| `postgres` | PostgreSQL   | Requires running Postgres on localhost/music |
| `mongodb`  | MongoDB      | Requires running MongoDB                     |
| `redis`    | Redis        | Requires running Redis                       |

## Service Architecture Invariants

- `album-service` must NEVER import from `org.cloudfoundry.samples.music.*` — that breaks the fence
- `recommendation-service` must NEVER import from `org.cloudfoundry.samples.albumservice.*` — it is independently deployable
- All inter-service communication uses DTOs (`AlbumPayload`, `AlbumCreatedPayload`) — not shared domain objects
- `AlbumTranslator` is the ONLY place that maps `AlbumPayload` ↔ `AlbumEntity` in album-service
- `EventRelayService` fires AFTER transaction commit (`@TransactionalEventListener`) — album is always saved before event relay is attempted
- GET /albums/{id} returns **404** in album-service and **200+empty** in the monolith — this is a documented divergence, not a bug. Pinned by `MonolithAlbumContractTest`.

## "Done" Definition (Challenges 5–9)

- Monolith starts on port 8080, all `AlbumControllerTest` pass
- album-service starts on port 8081, all `AlbumServiceContractTest` pass
- recommendation-service starts on port 8082
- PUT to album-service → event arrives at recommendation-service → `GET /recommendations` returns the album
- `./gradlew test` (monolith) and `gradle test` (album-service) both green independently
