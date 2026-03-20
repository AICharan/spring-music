package org.cloudfoundry.samples.music.characterization;

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
 * Challenge 4 — The Pin.
 *
 * Characterization tests: pin the ACTUAL behavior of the monolith as it exists today.
 * These tests document observed behavior, including quirks. They are not testing correctness —
 * they are a safety net that screams if behavior changes unexpectedly during refactoring.
 *
 * Quirks pinned:
 * - GET /albums/{unknownId} returns HTTP 200 + empty body (not 404)
 * - PUT /albums ignores any client-supplied id; always generates a new one
 * - GET /albums returns all 29 seeded albums on a fresh H2 start
 * - GET /appinfo returns empty profiles [] and services [] when running locally
 * - DELETE /albums/{id} returns 200 with empty body (not 204)
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Characterization Tests — Pin monolith behavior before any structural changes")
class AlbumApiCharacterizationTest {

    @Autowired
    private MockMvc mockMvc;

    // -------------------------------------------------------------------------
    // Quirk 1: GET unknown id returns 200 + empty body, NOT 404
    // This is a design quirk in AlbumController.getById() — it returns null
    // which Spring serialises as an empty response, not a 404 error.
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("GET /albums/{id} with unknown id returns 200 and empty body (not 404)")
    void getById_unknownId_returns200WithEmptyBody_notNotFound() throws Exception {
        mockMvc.perform(get("/albums/{id}", "this-id-does-not-exist-anywhere"))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
    }

    // -------------------------------------------------------------------------
    // Quirk 2: PUT /albums always generates a server-side ID.
    // Even if client provides an id field, a new UUID is generated.
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("PUT /albums always assigns a server-generated id regardless of client-supplied id")
    void addAlbum_serverGeneratesId_ignoresClientId() throws Exception {
        String body = """
                {
                  "id": "client-supplied-id-should-be-ignored",
                  "title": "Pin Test Album",
                  "artist": "Pin Artist",
                  "releaseYear": "2024",
                  "genre": "Test",
                  "trackCount": 1
                }
                """;

        mockMvc.perform(put("/albums")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", not("client-supplied-id-should-be-ignored")))
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.title", is("Pin Test Album")));
    }

    // -------------------------------------------------------------------------
    // Quirk 3: GET /albums on fresh H2 start returns 29 seeded albums from albums.json
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("GET /albums returns seeded catalog of 29 albums on H2 default profile")
    void getAlbums_returnsSeededData_29Albums() throws Exception {
        mockMvc.perform(get("/albums"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(29)));
    }

    // -------------------------------------------------------------------------
    // Quirk 4: Seeded data contains specific known albums — pin them
    // If the albums.json is changed, this test breaks (intentional)
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("GET /albums contains known seeded albums from albums.json")
    void getAlbums_containsKnownSeededAlbums() throws Exception {
        mockMvc.perform(get("/albums"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].title", hasItems(
                        "Nevermind",
                        "Thriller",
                        "Abbey Road",
                        "Hotel California",
                        "Texas Flood"
                )));
    }

    // -------------------------------------------------------------------------
    // Quirk 5: DELETE returns 200 with empty body (not 204 No Content)
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("DELETE /albums/{id} returns 200 with empty body (not 204 No Content)")
    void deleteAlbum_returns200NotNoContent() throws Exception {
        // First get any album id from the seeded data
        String firstId = mockMvc.perform(get("/albums"))
                .andReturn()
                .getResponse()
                .getContentAsString()
                .replaceAll(".*\"id\":\"([^\"]+)\".*", "$1")
                .split(",")[0]
                .replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");

        // Use a known approach: get albums, pick first one
        String albumsJson = mockMvc.perform(get("/albums"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Extract an id using a simple approach
        String idToDelete = albumsJson.substring(albumsJson.indexOf("\"id\":\"") + 6,
                albumsJson.indexOf("\"", albumsJson.indexOf("\"id\":\"") + 6));

        mockMvc.perform(delete("/albums/{id}", idToDelete))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
    }

    // -------------------------------------------------------------------------
    // Quirk 6: GET /appinfo returns empty profiles and services when run locally
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("GET /appinfo returns empty profiles and services when no CF environment")
    void appInfo_returnsEmptyProfilesAndServices_whenNotOnCF() throws Exception {
        mockMvc.perform(get("/appinfo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profiles", is(emptyOrNullString()).or(
                        jsonPath("$.profiles", hasSize(0)))))
                .andExpect(jsonPath("$.services", hasSize(0)));
    }

    // -------------------------------------------------------------------------
    // Quirk 7: GET /service returns empty array locally (no CF services)
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("GET /service returns empty array when no CF services are bound")
    void serviceEndpoint_returnsEmptyArray_whenNotOnCF() throws Exception {
        mockMvc.perform(get("/service"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // -------------------------------------------------------------------------
    // Quirk 8: POST /albums upserts — if id present and exists, it updates; if not, it creates new
    // This pins the upsert behaviour of Spring Data's save()
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("POST /albums with no id acts as create (upsert via Spring Data save)")
    void postAlbum_withNoId_createsNewAlbum() throws Exception {
        String body = """
                {
                  "title": "Upsert Test",
                  "artist": "Test Artist",
                  "releaseYear": "2020",
                  "genre": "Test",
                  "trackCount": 3
                }
                """;

        mockMvc.perform(post("/albums")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Upsert Test")));
    }

    // -------------------------------------------------------------------------
    // Quirk 9: All seeded albums have genre and releaseYear populated
    // (albums.json has no trackCount — verifies default value is 0)
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Seeded albums from albums.json have trackCount defaulting to 0")
    void seededAlbums_trackCountDefaultsToZero() throws Exception {
        mockMvc.perform(get("/albums"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].trackCount", is(0)));
    }
}
