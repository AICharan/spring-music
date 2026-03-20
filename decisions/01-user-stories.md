# Challenge 1 — The Stories

> Reverse-engineered business capabilities from spring-music source code.

---

## Business Capability 1: Album Catalog Management

**As a** music store administrator
**I want to** create, view, edit, and delete albums in the catalog
**So that** customers can browse an up-to-date music collection

### Acceptance Criteria

| #   | Scenario           | Given                                        | When                                          | Then                                                                                   |
| --- | ------------------ | -------------------------------------------- | --------------------------------------------- | -------------------------------------------------------------------------------------- |
| 1.1 | View all albums    | The catalog has seeded data                  | GET /albums                                   | Returns JSON array of 29 albums with title, artist, releaseYear, genre, trackCount, id |
| 1.2 | Add a new album    | Valid album payload (title, artist required) | PUT /albums with JSON body                    | Returns 200 with the saved album including a server-generated UUID id                  |
| 1.3 | Update an album    | An album with known id exists                | POST /albums with modified album (id present) | Returns 200 with updated fields persisted                                              |
| 1.4 | Delete an album    | An album with known id exists                | DELETE /albums/{id}                           | Returns 200; subsequent GET /albums/{id} returns empty body (200)                      |
| 1.5 | Get a single album | An album with known id exists                | GET /albums/{id}                              | Returns 200 with that album's fields                                                   |
| 1.6 | Get unknown album  | No album with that id exists                 | GET /albums/{id}                              | Returns 200 with empty response body (not 404) **[quirk — see ADR]**                   |

---

## Business Capability 2: Multi-Backend Persistence

**As a** platform engineer
**I want to** run the same application against different database backends (H2, MySQL, Postgres, MongoDB, Redis)
**So that** the app can demonstrate and validate database service options on Cloud Foundry

### Acceptance Criteria

| #   | Scenario            | Given                           | When        | Then                                                                    |
| --- | ------------------- | ------------------------------- | ----------- | ----------------------------------------------------------------------- |
| 2.1 | Default (in-memory) | No active profile               | App starts  | Uses H2 in-memory; album data is seeded from albums.json                |
| 2.2 | MySQL profile       | spring.profiles.active=mysql    | App starts  | Connects to jdbc:mysql://localhost/music via com.mysql.cj.jdbc.Driver   |
| 2.3 | Postgres profile    | spring.profiles.active=postgres | App starts  | Connects to jdbc:postgresql://localhost/music via org.postgresql.Driver |
| 2.4 | MongoDB profile     | spring.profiles.active=mongodb  | App starts  | Uses MongoAlbumRepository; JPA auto-config excluded                     |
| 2.5 | Redis profile       | spring.profiles.active=redis    | App starts  | Uses RedisAlbumRepository; JPA + Mongo auto-config excluded             |
| 2.6 | Multiple profiles   | Two DB profiles active          | App starts  | Throws IllegalStateException before accepting requests                  |
| 2.7 | Profile isolation   | Any single profile              | GET /albums | Returns data from that backend only                                     |

---

## Business Capability 3: Cloud Foundry Service Discovery

**As a** Cloud Foundry platform operator
**I want to** bind a database service to the app and have it auto-configure
**So that** no code changes or manual config are needed when deploying to different CF environments

### Acceptance Criteria

| #   | Scenario                 | Given                                         | When         | Then                                                                   |
| --- | ------------------------ | --------------------------------------------- | ------------ | ---------------------------------------------------------------------- |
| 3.1 | No bound services        | Running locally (no VCAP_SERVICES)            | GET /appinfo | Returns {"profiles":[],"services":[]}                                  |
| 3.2 | CF MySQL service bound   | VCAP_SERVICES contains mysql-tagged service   | App starts   | SpringApplicationContextInitializer adds "mysql" profile automatically |
| 3.3 | CF MongoDB service bound | VCAP_SERVICES contains mongodb-tagged service | App starts   | Adds "mongodb" profile; JPA auto-config excluded                       |
| 3.4 | Two conflicting services | Two DB services bound                         | App starts   | Throws IllegalStateException listing both conflicting service names    |
| 3.5 | Service name exposure    | Any CF service bound                          | GET /service | Returns list of CfService objects with name and tags                   |
| 3.6 | Active profile exposure  | Any profile active                            | GET /appinfo | Returns {"profiles":["<active>"],"services":["<service-name>"]}        |
