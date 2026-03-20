# Challenge 10 — The Weekend: Cutover Runbook

> The one ops will actually follow at 3am.

---

## Pre-Cutover Checklist (T-48h)

- [ ] album-service contract tests (`AlbumServiceContractTest`) green on latest main
- [ ] Monolith contract tests (`MonolithAlbumContractTest`) green on latest main
- [ ] album-service health check returns 200: `GET http://album-service:8081/actuator/health`
- [ ] recommendation-service health check returns 200: `GET http://recommendation-service:8082/health`
- [ ] album-service has ingested all existing album data from monolith (data migration verified)
- [ ] Load test completed: album-service handles peak TPS (observed from monolith metrics)
- [ ] Rollback plan reviewed with on-call engineer
- [ ] PagerDuty alert routing confirmed: album-service errors → on-call

---

## Data Migration (T-24h)

The monolith and album-service have separate databases. Before cutover, migrate data:

```bash
# 1. Export all albums from monolith
curl http://monolith:8080/albums > albums-export.json

# 2. Verify export count matches expected
EXPECTED=29  # or current production count
ACTUAL=$(jq length albums-export.json)
[ "$ACTUAL" -ge "$EXPECTED" ] || { echo "ABORT: export count mismatch ($ACTUAL vs $EXPECTED)"; exit 1; }

# 3. Import into album-service
jq -c '.[]' albums-export.json | while read album; do
  curl -s -X PUT http://album-service:8081/albums \
    -H "Content-Type: application/json" \
    -d "$album" || echo "WARN: import failed for $album"
done

# 4. Verify import
curl http://album-service:8081/albums | jq length
```

---

## Cutover Steps (Strangler Fig — Traffic Shift)

### Step 1: Enable dark launch (T-0, 09:00 AEST)

Route 5% of `/albums` traffic to album-service. Monitor for 30 minutes.

```nginx
# nginx / API gateway rule — shadow routing
upstream albums {
  server monolith:8080   weight=95;
  server album-service:8081  weight=5;
}
```

**Watch for 30 minutes:**

- album-service error rate < 0.5% (baseline: monolith error rate)
- album-service p99 latency < monolith p99 + 50ms
- No 5xx in album-service logs

**Rollback trigger (Step 1):** If any condition above is violated → revert weight to monolith=100, album-service=0. Incident report required before retry.

---

### Step 2: Ramp to 50% (T+30min, if Step 1 healthy)

```nginx
upstream albums {
  server monolith:8080   weight=50;
  server album-service:8081  weight=50;
}
```

**Watch for 15 minutes.** Same thresholds as Step 1.

**Rollback trigger (Step 2):** Same as Step 1.

---

### Step 3: Full cutover (T+45min, if Step 2 healthy)

```nginx
upstream albums {
  server album-service:8081  weight=100;
}
# monolith still running — not removed yet
```

**Watch for 60 minutes.** Extended observation window.

**Rollback trigger (Step 3):**

- Error rate > 1% over any 5-minute window
- Any data loss reported by application team
- Customer-facing impact confirmed

---

### Step 4: Monolith /albums deprecation (T+7 days, if Step 3 stable)

After 7 days of clean production on album-service:

1. Add deprecation header to monolith `/albums` endpoints: `Deprecation: true`
2. Add `X-Service: monolith-deprecated` response header
3. Update all internal consumers to call album-service directly

---

### Step 5: Monolith /albums removal (T+30 days)

1. Remove `AlbumController`, `JpaAlbumRepository`, `MongoAlbumRepository`, `RedisAlbumRepository` from monolith
2. Keep `InfoController` and `SpringApplicationContextInitializer` (not yet extracted)
3. Deploy monolith without album endpoints
4. Decommission monolith database if no other endpoints use it

---

## 3am Decision Tree

```
Is album-service down?
├── YES → Step 1: Is rollback in place (traffic → monolith)?
│         ├── YES → Monitor. Page album-service owner. Do not page CTO.
│         └── NO  → IMMEDIATE: revert nginx to monolith=100. Then investigate.
└── NO  → Is error rate > 1%?
          ├── YES → Check album-service logs. Is it a data issue?
          │         ├── YES (data corruption) → ROLLBACK. Page DBA. Do not continue.
          │         └── NO (transient) → Wait 5 minutes. If still > 1%, rollback.
          └── NO  → Is recommendation-service down?
                    ├── YES → Non-blocking. Album CRUD still works.
                    │         Log incident. Events will be lost (dual-write risk).
                    │         Fix recommendation-service during business hours.
                    └── NO  → False alarm. Check monitoring dashboard. Go back to sleep.
```

---

## Rollback Command (any step)

```bash
# Immediate rollback — 30-second SLA
kubectl set env deployment/api-gateway ALBUMS_BACKEND=http://monolith:8080

# Or nginx:
nginx -s reload  # after updating weights to monolith=100
```

---

## Post-Cutover Verification

```bash
# Verify album-service is serving production traffic
curl -s http://album-service:8081/actuator/health | jq .status
# Expected: "UP"

# Verify album count is consistent
MONO_COUNT=$(curl -s http://monolith:8080/albums | jq length)
SVC_COUNT=$(curl -s http://album-service:8081/albums | jq length)
echo "Monolith: $MONO_COUNT | Service: $SVC_COUNT"
# Expected: equal counts (or service > monolith if new albums added post-migration)

# Verify recommendation-service receiving events
curl http://recommendation-service:8082/recommendations | jq length
# Expected: > 0 if any PUT /albums occurred since cutover
```

---

## Contacts

| Role                | Contact          | When to page                                 |
| ------------------- | ---------------- | -------------------------------------------- |
| album-service owner | On-call rotation | Any Step 3+ rollback                         |
| DBA                 | db-on-call@      | Data corruption / migration failure          |
| Platform / infra    | infra-on-call@   | nginx / routing failures                     |
| CTO                 | —                | Only if data loss is confirmed in production |
