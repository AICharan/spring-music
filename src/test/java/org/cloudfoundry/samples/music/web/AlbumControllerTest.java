package org.cloudfoundry.samples.music.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.cloudfoundry.samples.music.domain.Album;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.repository.CrudRepository;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AlbumControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CrudRepository<Album, String> repository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void getAlbums_returnsEmptyList() throws Exception {
        mockMvc.perform(get("/albums"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void addAlbum_returnsCreatedAlbum() throws Exception {
        Album album = Album.builder()
                .title("Kind of Blue")
                .artist("Miles Davis")
                .releaseYear("1959")
                .genre("Jazz")
                .trackCount(5)
                .build();

        mockMvc.perform(put("/albums")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(album)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Kind of Blue")))
                .andExpect(jsonPath("$.artist", is("Miles Davis")))
                .andExpect(jsonPath("$.id", notNullValue()));
    }

    @Test
    void getAlbumById_returnsAlbum() throws Exception {
        Album saved = repository.save(Album.builder()
                .title("Abbey Road")
                .artist("The Beatles")
                .releaseYear("1969")
                .genre("Rock")
                .build());

        mockMvc.perform(get("/albums/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Abbey Road")))
                .andExpect(jsonPath("$.artist", is("The Beatles")));
    }

    @Test
    void getAlbumById_unknownId_returnsNull() throws Exception {
        mockMvc.perform(get("/albums/{id}", "does-not-exist"))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
    }

    @Test
    void updateAlbum_persistsChanges() throws Exception {
        Album saved = repository.save(Album.builder()
                .title("Original Title")
                .artist("Artist")
                .releaseYear("2000")
                .genre("Pop")
                .build());

        saved.setTitle("Updated Title");

        mockMvc.perform(post("/albums")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saved)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Updated Title")));
    }

    @Test
    void deleteAlbum_removesFromRepository() throws Exception {
        Album saved = repository.save(Album.builder()
                .title("To Delete")
                .artist("Artist")
                .releaseYear("2010")
                .genre("Pop")
                .build());

        mockMvc.perform(delete("/albums/{id}", saved.getId()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/albums/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
    }

    @Test
    void getAlbums_afterAddingMultiple_returnsAll() throws Exception {
        repository.save(Album.builder().title("Album 1").artist("Artist A").releaseYear("2001").genre("Rock").build());
        repository.save(Album.builder().title("Album 2").artist("Artist B").releaseYear("2002").genre("Jazz").build());
        repository.save(Album.builder().title("Album 3").artist("Artist C").releaseYear("2003").genre("Pop").build());

        mockMvc.perform(get("/albums"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));
    }
}
