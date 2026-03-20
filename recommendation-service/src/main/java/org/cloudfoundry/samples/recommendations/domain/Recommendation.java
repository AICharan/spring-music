package org.cloudfoundry.samples.recommendations.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

/**
 * Challenge 9 — The Second Cut.
 *
 * A recommendation entry, derived from an AlbumCreatedEvent.
 * The recommendation-service maintains a sliding window of recently-added albums.
 *
 * This is recommendation-service's OWN domain — no dependency on Album or AlbumEntity
 * from other services. The fence holds.
 */
@Data
@AllArgsConstructor
public class Recommendation {
    private String albumId;
    private String title;
    private String artist;
    private String genre;
    private Instant receivedAt;
}
