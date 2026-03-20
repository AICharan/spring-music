package org.cloudfoundry.samples.albumservice.repository;

import org.cloudfoundry.samples.albumservice.domain.AlbumEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AlbumRepository extends JpaRepository<AlbumEntity, String> {
}
