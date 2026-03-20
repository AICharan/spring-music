package org.cloudfoundry.samples.albumservice.acl;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Challenge 6 — The Fence: Anti-Corruption Layer contract DTO.
 *
 * AlbumPayload is the API contract shared by the monolith and album-service.
 * It contains NO JPA annotations, NO Hibernate-specific types, and NO
 * references to either service's internal domain.
 *
 * Both the monolith's /albums endpoint and album-service's /albums endpoint
 * produce and consume JSON that maps to this shape.
 *
 * Contract (pinned by AlbumContractTest):
 *   { "id": string, "title": string, "artist": string,
 *     "releaseYear": string, "genre": string, "trackCount": int }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlbumPayload {
    private String id;
    private String title;
    private String artist;
    private String releaseYear;
    private String genre;
    private int trackCount;
}
