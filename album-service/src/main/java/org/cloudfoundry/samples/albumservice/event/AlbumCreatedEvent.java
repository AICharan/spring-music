package org.cloudfoundry.samples.albumservice.event;

import org.cloudfoundry.samples.albumservice.acl.AlbumPayload;

/**
 * Challenge 9 — The Second Cut.
 *
 * Domain event published by album-service after a new album is persisted.
 * This event is the contract between album-service and recommendation-service.
 *
 * The event carries AlbumPayload (the ACL DTO) — not the internal AlbumEntity.
 * This preserves the fence: recommendation-service only knows the public contract.
 */
public record AlbumCreatedEvent(AlbumPayload album) {
}
