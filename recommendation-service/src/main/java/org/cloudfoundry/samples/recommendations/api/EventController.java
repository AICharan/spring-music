package org.cloudfoundry.samples.recommendations.api;

import org.cloudfoundry.samples.recommendations.domain.Recommendation;
import org.cloudfoundry.samples.recommendations.event.AlbumCreatedPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Challenge 9 — The Second Cut.
 *
 * Receives AlbumCreatedEvents from album-service via HTTP POST.
 * Maintains a sliding window of the 20 most recently added albums as recommendations.
 *
 * DUAL-WRITE HANDLING: Events may arrive at-most-once (see ADR-003).
 * Idempotency: events with a duplicate albumId are silently ignored.
 *
 * Note: In-memory store — not persistent across restarts. For production,
 * persist to a database and read from there.
 */
@RestController
public class EventController {

    private static final Logger logger = LoggerFactory.getLogger(EventController.class);
    private static final int MAX_RECOMMENDATIONS = 20;

    private final Deque<Recommendation> recentlyAdded = new ArrayDeque<>();
    private final Set<String> seenIds = ConcurrentHashMap.newKeySet();

    @PostMapping("/events/album-created")
    public ResponseEntity<Void> onAlbumCreated(@RequestBody AlbumCreatedPayload payload) {
        // Idempotency: skip duplicate events
        if (!seenIds.add(payload.getId())) {
            logger.info("Duplicate event for albumId={} — ignored", payload.getId());
            return ResponseEntity.ok().build();
        }

        Recommendation rec = new Recommendation(
                payload.getId(),
                payload.getTitle(),
                payload.getArtist(),
                payload.getGenre(),
                Instant.now()
        );

        synchronized (recentlyAdded) {
            if (recentlyAdded.size() >= MAX_RECOMMENDATIONS) {
                recentlyAdded.pollFirst(); // evict oldest
            }
            recentlyAdded.addLast(rec);
        }

        logger.info("Received AlbumCreatedEvent: albumId={} title={}", payload.getId(), payload.getTitle());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/recommendations")
    public List<Recommendation> getRecommendations() {
        synchronized (recentlyAdded) {
            return List.copyOf(recentlyAdded).reversed();
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("recommendation-service UP");
    }
}
