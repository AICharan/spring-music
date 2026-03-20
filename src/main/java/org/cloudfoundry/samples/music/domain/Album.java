package org.cloudfoundry.samples.music.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Album {

    @Id
    @Column(length = 40)
    @GeneratedValue(generator = "randomId")
    @GenericGenerator(name = "randomId", strategy = "org.cloudfoundry.samples.music.domain.RandomIdGenerator")
    private String id;

    private String title;
    private String artist;
    private String releaseYear;
    private String genre;
    private int trackCount;
    private String albumId;
}
