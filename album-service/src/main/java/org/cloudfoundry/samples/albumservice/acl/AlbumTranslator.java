package org.cloudfoundry.samples.albumservice.acl;

import org.cloudfoundry.samples.albumservice.domain.AlbumEntity;
import org.springframework.stereotype.Component;

/**
 * Challenge 6 — The Fence: translates between the API contract (AlbumPayload)
 * and the service's internal domain (AlbumEntity).
 *
 * This class is the ONLY place where the two representations meet.
 * No other class in album-service should map between them.
 */
@Component
public class AlbumTranslator {

    public AlbumPayload toPayload(AlbumEntity entity) {
        return AlbumPayload.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .artist(entity.getArtist())
                .releaseYear(entity.getReleaseYear())
                .genre(entity.getGenre())
                .trackCount(entity.getTrackCount())
                .build();
    }

    public AlbumEntity toEntity(AlbumPayload payload) {
        return AlbumEntity.builder()
                .id(payload.getId() != null ? payload.getId() : AlbumEntity.generateId())
                .title(payload.getTitle())
                .artist(payload.getArtist())
                .releaseYear(payload.getReleaseYear())
                .genre(payload.getGenre())
                .trackCount(payload.getTrackCount())
                .build();
    }

    public AlbumEntity toNewEntity(AlbumPayload payload) {
        // Always generate a fresh server-side ID — client id is ignored
        return AlbumEntity.builder()
                .id(AlbumEntity.generateId())
                .title(payload.getTitle())
                .artist(payload.getArtist())
                .releaseYear(payload.getReleaseYear())
                .genre(payload.getGenre())
                .trackCount(payload.getTrackCount())
                .build();
    }
}
