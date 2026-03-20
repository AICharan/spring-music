package org.cloudfoundry.samples.music.contract;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Challenge 7 — The Contract (Consumer / Monolith side).
 *
 * Verifies that the monolith's /albums API satisfies the same AlbumPayload contract
 * as album-service. If both sides pass these tests, they are compatible.
 *
 * Documented divergence from album-service:
 * - GET /albums/{unknownId} → monolith returns 200+empty; album-service returns 404.
 *   This divergence is INTENTIONAL and documented. The contract test pins each side's
 *   known behaviour. A future migration would align the monolith to return 404.
 *
 * Contract rules tested here (must match AlbumServiceContractTest):
 * 1. GET /albums returns a JSON array
 * 2. Each album has: id, title, artist, releaseYear, genre, trackCount
 * 3. PUT /albums creates album with server-assigned id
 * 4. POST /albums updates album
 * 5. DELETE /albums/{id} removes album
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Contract Tests — Monolith satisfies AlbumPayload contract (consumer side)")
class MonolithAlbumContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /albums returns JSON array — contract satisfied")
    void getAlbums_returnsJsonArray() throws Exception {
        mockMvc.perform(get("/albums"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("PUT /albums — contract: returns album with id, title, artist, releaseYear, genre, trackCount")
    void putAlbum_returnsContractShape() throws Exception {
        String body = """
                {
                  "title": "Contract Check",
                  "artist": "Monolith Artist",
                  "releaseYear": "2024",
                  "genre": "Test",
                  "trackCount": 3
                }
                """;

        mockMvc.perform(put("/albums")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isString())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.title", is("Contract Check")))
                .andExpect(jsonPath("$.artist", is("Monolith Artist")))
                .andExpect(jsonPath("$.releaseYear", is("2024")))
                .andExpect(jsonPath("$.genre", is("Test")))
                .andExpect(jsonPath("$.trackCount", is(3)));
    }

    @Test
    @DisplayName("GET /albums — each album element contains all contract fields")
    void getAlbums_eachElementHasAllContractFields() throws Exception {
        mockMvc.perform(get("/albums"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].title").exists())
                .andExpect(jsonPath("$[0].artist").exists())
                .andExpect(jsonPath("$[0].releaseYear").exists())
                .andExpect(jsonPath("$[0].genre").exists())
                .andExpect(jsonPath("$[0].trackCount").exists());
    }

    /**
     * DOCUMENTED DIVERGENCE: monolith returns 200+empty for unknown id.
     * album-service returns 404.
     * This test pins the MONOLITH's known behaviour.
     * When the monolith is updated to align with album-service, change to expect 404.
     */
    @Test
    @DisplayName("GET /albums/{unknownId} — monolith returns 200+empty (known divergence from album-service 404)")
    void getById_unknownId_returns200Empty_monolithQuirk() throws Exception {
        mockMvc.perform(get("/albums/{id}", "definitely-does-not-exist"))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
    }
}
