package org.cloudfoundry.samples.albumservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Challenge 6 — The Fence.
 *
 * This is album-service's OWN domain entity. It is NOT the monolith's Album class.
 * The monolith's @GeneratedValue/@GenericGenerator chain does not exist here.
 * The monolith's `albumId` field (legacy external reference) is not present here.
 *
 * The fence: nothing from org.cloudfoundry.samples.music.* is imported in this service.
 * All communication with the outside world goes through AlbumPayload (the ACL contract).
 */
@Entity
@Table(name = "albums")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlbumEntity {

    @Id
    @Column(length = 40)
    private String id;

    private String title;
    private String artist;
    private String releaseYear;
    private String genre;
    private int trackCount;

    public static String generateId() {
        return UUID.randomUUID().toString();
    }
}
