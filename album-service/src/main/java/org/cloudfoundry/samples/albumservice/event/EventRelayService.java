package org.cloudfoundry.samples.albumservice.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Challenge 9 — The Second Cut.
 *
 * Relays AlbumCreatedEvent to recommendation-service via HTTP POST after the
 * album-service transaction commits.
 *
 * DUAL-WRITE PROBLEM: if the HTTP relay fails, the event is lost (at-most-once delivery).
 * The album IS saved (transaction committed before this listener fires).
 * Production fix: replace this with the Outbox pattern (see ADR-003).
 *
 * Using @TransactionalEventListener(phase = AFTER_COMMIT) ensures:
 * - Event is NOT sent if the transaction rolls back
 * - Album save and event relay are NOT in the same atomic unit (dual-write risk accepted)
 */
@Component
public class EventRelayService {

    private static final Logger logger = LoggerFactory.getLogger(EventRelayService.class);

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${recommendation.service.url:http://localhost:8082}")
    private String recommendationServiceUrl;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAlbumCreated(AlbumCreatedEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event.album());
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(recommendationServiceUrl + "/events/album-created"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                logger.info("Album event relayed to recommendation-service: albumId={}",
                        event.album().getId());
            } else {
                logger.warn("Recommendation-service returned {} for albumId={}. Event may be lost.",
                        response.statusCode(), event.album().getId());
            }
        } catch (Exception e) {
            // DUAL-WRITE: album is saved but event not delivered. Log and continue.
            // Production: write to outbox table instead of throwing.
            logger.error("Failed to relay AlbumCreatedEvent for albumId={}. " +
                    "Event lost — implement Outbox pattern for at-least-once delivery. Error: {}",
                    event.album().getId(), e.getMessage());
        }
    }
}
