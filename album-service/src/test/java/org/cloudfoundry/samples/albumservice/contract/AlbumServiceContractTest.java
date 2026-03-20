package org.cloudfoundry.samples.albumservice.contract;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.cloudfoundry.samples.albumservice.acl.AlbumPayload;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Challenge 7 — The Contract (Provider side).
 *
 * These tests verify that album-service satisfies the AlbumPayload contract.
 * The same contract is verified against the monolith in MonolithAlbumContractTest.
 *
 * Contract rules:
 * 1. GET /albums returns a JSON array
 * 2. Each element has: id (string), title (string), artist (string),
 *    releaseYear (string), genre (string), trackCount (int)
 * 3. PUT /albums accepts a payload without id; responds with the same schema + server-assigned id
 * 4. GET /albums/{id} returns the album by id; returns 404 if not found
 *    (NOTE: album-service returns 404, monolith returns 200+empty — documented quirk)
 * 5. POST /albums updates an existing album
 * 6. DELETE /albums/{id} removes the album; subsequent GET /albums/{id} returns 404
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Contract Tests — album-service satisfies AlbumPayload contract")
class AlbumServiceContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("GET /albums returns JSON array")
    void getAlbums_returnsJsonArray() throws Exception {
        mockMvc.perform(get("/albums"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("PUT /albums returns album with server-assigned id and all contract fields")
    void putAlbum_returnsContractShape() throws Exception {
        AlbumPayload input = AlbumPayload.builder()
                .title("Contract Test Album")
                .artist("Contract Artist")
                .releaseYear("2024")
                .genre("Test")
                .trackCount(7)
                .build();

        MvcResult result = mockMvc.perform(put("/albums")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isString())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.title", is("Contract Test Album")))
                .andExpect(jsonPath("$.artist", is("Contract Artist")))
                .andExpect(jsonPath("$.releaseYear", is("2024")))
                .andExpect(jsonPath("$.genre", is("Test")))
                .andExpect(jsonPath("$.trackCount", is(7)))
                .andReturn();

        // Verify deserialisable to AlbumPayload
        AlbumPayload response = objectMapper.readValue(
                result.getResponse().getContentAsString(), AlbumPayload.class);
        assertNotNull(response.getId());
        assertEquals("Contract Test Album", response.getTitle());
    }

    @Test
    @DisplayName("GET /albums/{id} returns 200 when album exists")
    void getById_existingAlbum_returns200() throws Exception {
        // Create an album first
        AlbumPayload input = AlbumPayload.builder()
                .title("Fetchable Album")
                .artist("Some Artist")
                .releaseYear("2023")
                .genre("Rock")
                .trackCount(10)
                .build();

        MvcResult created = mockMvc.perform(put("/albums")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andReturn();

        AlbumPayload createdAlbum = objectMapper.readValue(
                created.getResponse().getContentAsString(), AlbumPayload.class);

        mockMvc.perform(get("/albums/{id}", createdAlbum.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(createdAlbum.getId())))
                .andExpect(jsonPath("$.title", is("Fetchable Album")));
    }

    @Test
    @DisplayName("GET /albums/{id} returns 404 when album does not exist (album-service behaviour)")
    void getById_unknownId_returns404() throws Exception {
        // album-service returns 404 (correct REST semantics)
        // This DIFFERS from the monolith which returns 200+empty (documented quirk)
        mockMvc.perform(get("/albums/{id}", "nonexistent-album-id-xyz"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /albums updates an existing album and returns updated contract shape")
    void postAlbum_updatesAlbum_returnsContractShape() throws Exception {
        // Create
        AlbumPayload input = AlbumPayload.builder()
                .title("Original")
                .artist("Artist")
                .releaseYear("2000")
                .genre("Jazz")
                .trackCount(5)
                .build();

        MvcResult created = mockMvc.perform(put("/albums")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andReturn();

        AlbumPayload createdAlbum = objectMapper.readValue(
                created.getResponse().getContentAsString(), AlbumPayload.class);

        // Update
        createdAlbum.setTitle("Updated Title");
        mockMvc.perform(post("/albums")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createdAlbum)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(createdAlbum.getId())))
                .andExpect(jsonPath("$.title", is("Updated Title")));
    }

    @Test
    @DisplayName("DELETE /albums/{id} removes album; subsequent GET returns 404")
    void deleteAlbum_thenGetReturns404() throws Exception {
        AlbumPayload input = AlbumPayload.builder()
                .title("To Delete")
                .artist("Artist")
                .releaseYear("2010")
                .genre("Pop")
                .trackCount(3)
                .build();

        MvcResult created = mockMvc.perform(put("/albums")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andReturn();

        AlbumPayload createdAlbum = objectMapper.readValue(
                created.getResponse().getContentAsString(), AlbumPayload.class);

        mockMvc.perform(delete("/albums/{id}", createdAlbum.getId()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/albums/{id}", createdAlbum.getId()))
                .andExpect(status().isNotFound());
    }
}
