package org.cloudfoundry.samples.recommendations.event;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Challenge 9 — The Second Cut.
 *
 * The event payload that recommendation-service expects from album-service.
 * This is the service's own representation of the incoming event.
 * It is NOT imported from album-service — it is independently defined here.
 *
 * This is the fence applied to events: recommendation-service has no compile-time
 * dependency on album-service. They are coupled only by the event's JSON shape.
 */
@Data
@NoArgsConstructor
public class AlbumCreatedPayload {
    private String id;
    private String title;
    private String artist;
    private String releaseYear;
    private String genre;
    private int trackCount;
}
