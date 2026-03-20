# ADR-001: Strangler Fig as Decomposition Strategy

**Status:** Accepted
**Date:** 2026-03-20
**Deciders:** Team 17

---

## Context

The spring-music monolith must be decomposed without a big-bang rewrite. The app is running in production (on Cloud Foundry) and must remain operational throughout the migration. Three strategies were considered.

## Options Considered

| Strategy                  | Description                                                 | Risk                                                                 |
| ------------------------- | ----------------------------------------------------------- | -------------------------------------------------------------------- |
| **Big-bang rewrite**      | Rewrite the entire app from scratch                         | High — two codebases, zero trust in feature parity                   |
| **Branch by abstraction** | Introduce interface layer, swap implementations             | Medium — complex wiring, hard to test in isolation                   |
| **Strangler fig**         | Build new service alongside; redirect traffic incrementally | Low — monolith is always the fallback, new service is proven in prod |

## Decision

**Strangler fig.** New services (starting with `album-service`) are built alongside the monolith and expose the same REST contract. The monolith continues to handle all requests. Once the new service is proven, a routing layer (API gateway or load balancer rule) redirects `/albums` traffic to it. The monolith's `/albums` endpoint is then deprecated, not deleted.

## Consequences

**Positive:**

- Monolith is never broken — safe to roll back at any point
- Each service can be independently deployed and tested before traffic is shifted
- Forces clean contract definition (ACL) between old and new

**Negative:**

- Temporary duplication: two services serving the same domain
- Data synchronisation needed during transition (albums.json seed vs. live data)
- Operational complexity increases before it decreases

## Rollback Trigger

If album-service error rate exceeds 1% over 5 minutes in production, revert the routing rule to send all `/albums` traffic back to the monolith. No code change required.
