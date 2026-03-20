# ADR-003: Event-Driven Communication and the Dual-Write Problem

**Status:** Accepted
**Date:** 2026-03-20
**Deciders:** Team 17

---

## Context

`recommendation-service` must be notified when albums are created or updated in `album-service`. The challenge requirement is "no HTTP" — the services must communicate via events.

This introduces the **dual-write problem**: `album-service` must atomically write to its own database AND publish an event. If these are two separate operations, one can succeed while the other fails:

```
album-service:
  1. INSERT album into H2          ← succeeds
  2. POST event to recommendations ← fails (network timeout)

Result: album saved, recommendation-service never notified.
```

## Options Considered

| Option                              | Description                                                                     | Dual-Write Safety                                                    |
| ----------------------------------- | ------------------------------------------------------------------------------- | -------------------------------------------------------------------- |
| **Direct HTTP call**                | album-service POSTs to recommendation-service after saving                      | Not safe — violates "no HTTP" requirement, also dual-write risk      |
| **Spring ApplicationEvents**        | In-process events via ApplicationEventPublisher                                 | No dual-write risk (same transaction), but only works within one JVM |
| **Outbox pattern + polling**        | Write event to DB table in same transaction; scheduler relays to message broker | Safe — event and album share one ACID transaction                    |
| **Message broker (Kafka/RabbitMQ)** | Publish to broker after save                                                    | Still has dual-write risk unless using transactional outbox          |

## Decision

**Implement the Outbox pattern** for correctness, demonstrated with Spring `ApplicationEventPublisher` for the in-process prototype:

### Phase 1 (Implemented): In-process event relay

```
album-service:
  AlbumController.add()
    → repository.save(album)
    → applicationEventPublisher.publishEvent(AlbumCreatedEvent)
    → @TransactionalEventListener in EventRelayService
      → HTTP POST /events to recommendation-service
```

The `@TransactionalEventListener` fires AFTER the transaction commits, meaning the album is saved before the event is relayed. If the relay fails, the album remains saved and an error is logged. The event is not retried.

### Phase 2 (Prescribed, not implemented): Outbox pattern

```
album-service (same DB transaction):
  INSERT INTO albums ...
  INSERT INTO outbox_events (type='AlbumCreated', payload=..., sent=false)

Outbox poller (every 5 seconds):
  SELECT * FROM outbox_events WHERE sent=false
  POST event to recommendation-service
  UPDATE outbox_events SET sent=true
```

This guarantees at-least-once delivery. The recommendation-service must handle idempotent events (dedup by event_id).

## Consequences

**Phase 1:**

- Simple to implement and demo
- At-most-once delivery (event may be lost if relay fails)
- Acceptable for the hackathon; not acceptable for production

**Phase 2 (Outbox):**

- At-least-once delivery
- Adds complexity: outbox table, polling scheduler, idempotency in recommendation-service
- No external message broker required — uses the same DB already in place

## Why not Kafka?

Kafka would require a running broker (external dependency). For a hackathon demo, the Outbox pattern achieves the same delivery guarantees using only the H2 database already present in album-service.
