# ADR-002: Anti-Corruption Layer (ACL) Between Monolith and Extracted Services

**Status:** Accepted
**Date:** 2026-03-20
**Deciders:** Team 17

---

## Context

When extracting `album-service` from the monolith, there is a temptation to share the `Album` class between the two. This would introduce a hard coupling: any change to the monolith's `Album` entity (a JPA entity with `@Entity`, `@GeneratedValue`, Hibernate-specific annotations) would break the extracted service. Jakarta EE annotations, Lombok annotations, and Hibernate-specific details would leak across the service boundary.

## Decision

Each service owns its own domain model. No shared libraries contain domain objects.

The API contract is expressed as a plain DTO — `AlbumPayload` — that both sides agree on:

```java
// AlbumPayload.java — the API contract. Neither JPA nor monolith-specific annotations.
public class AlbumPayload {
    private String id;
    private String title;
    private String artist;
    private String releaseYear;
    private String genre;
    private int trackCount;
}
```

An `AlbumTranslator` component inside `album-service` converts between `AlbumPayload` (API contract) and `AlbumEntity` (service's internal JPA entity):

```
HTTP Client → AlbumPayload → AlbumTranslator → AlbumEntity → H2 DB
HTTP Client ← AlbumPayload ← AlbumTranslator ← AlbumEntity ← H2 DB
```

## Consequences

**Positive:**

- Monolith's `Album` class can evolve without breaking album-service
- album-service's schema can evolve without breaking the monolith
- The contract (AlbumPayload) is explicit and testable independently
- New services consuming the album API only need to know about `AlbumPayload`

**Negative:**

- Two `Album`-like classes exist simultaneously during transition (accepted duplication)
- `AlbumTranslator` must be kept in sync when contract fields change — mitigated by contract tests (Challenge 7)

## What "leaking" looks like (anti-pattern to avoid)

```java
// DO NOT do this in album-service — this imports the monolith's JPA entity
import org.cloudfoundry.samples.music.domain.Album; // WRONG

// DO this instead — use the contract DTO
import org.cloudfoundry.samples.albumservice.acl.AlbumPayload; // CORRECT
```
