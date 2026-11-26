package me.dani.lookout.site;

import me.dani.lookout.ApiIntegrationTest;
import org.junit.jupiter.api.Test;

import java.net.http.HttpResponse;
import java.util.List;
import java.util.UUID;

import static me.dani.lookout.ApiClient.json;
import static org.assertj.core.api.Assertions.assertThat;

class SiteApiTest extends ApiIntegrationTest {

    private static final String UNKNOWN_ID = "00000000-0000-0000-0000-000000000000";

    @Test
    void listWithoutLoginIs401() {
        assertThat(client.get("/api/sites").statusCode()).isEqualTo(401);
    }

    @Test
    void createWithoutCsrfHeaderIs403() {
        loggedInClient();

        var response = client.withoutCsrf().post("/api/sites", siteJson(uniqueName(), "[]", "1.0"));

        assertThat(response.statusCode()).isEqualTo(403);
    }

    @Test
    void createIs201WithLocationAndGeneratedKey() {
        loggedInClient();

        var response = createSite(uniqueName());

        assertThat(response.statusCode()).isEqualTo(201);
        String id = json(response, "$.id");
        assertThat(response.headers().firstValue("Location")).hasValue("/api/sites/" + id);
        assertThat(json(response, "$.publicKey").toString()).startsWith("pk_");
        List<String> origins = json(response, "$.allowedOrigins");
        assertThat(origins).containsExactlyInAnyOrder("https://blog.example.com", "https://www.blog.example.com");
    }

    @Test
    void createWithSameNameIs409() {
        loggedInClient();
        String name = uniqueName();
        createSite(name);

        assertThat(createSite(name).statusCode()).isEqualTo(409);
    }

    @Test
    void createWithSameNamePaddedBySpacesIs409() {
        loggedInClient();
        String name = uniqueName();
        createSite(name);

        assertThat(createSite("  " + name + "  ").statusCode()).isEqualTo(409);
    }

    @Test
    void createWithSameNameDifferentCaseIs409() {
        loggedInClient();
        String name = uniqueName();
        createSite(name);

        assertThat(createSite(name.toUpperCase()).statusCode()).isEqualTo(409);
    }

    @Test
    void createInvalidIs400() {
        loggedInClient();

        var response = client.post("/api/sites", """
                {"name": "   ", "samplingRate": 1.5}
                """);

        assertThat(response.statusCode()).isEqualTo(400);
    }

    @Test
    void createWithMalformedJsonIs400() {
        loggedInClient();

        assertThat(client.post("/api/sites", "{ \"name\": \"broken\",").statusCode()).isEqualTo(400);
    }

    @Test
    void getByIdIs200() {
        loggedInClient();
        String id = json(createSite(uniqueName()), "$.id");

        var response = client.get("/api/sites/" + id);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(json(response, "$.id").toString()).isEqualTo(id);
    }

    @Test
    void getUnknownIdIs404() {
        loggedInClient();

        assertThat(client.get("/api/sites/" + UNKNOWN_ID).statusCode()).isEqualTo(404);
    }

    @Test
    void getWithNonUuidIdIs400() {
        loggedInClient();

        assertThat(client.get("/api/sites/not-a-uuid").statusCode()).isEqualTo(400);
    }

    @Test
    void listContainsCreatedSite() {
        loggedInClient();
        String id = json(createSite(uniqueName()), "$.id");

        var response = client.get("/api/sites");

        assertThat(response.statusCode()).isEqualTo(200);
        List<String> ids = json(response, "$[*].id");
        assertThat(ids).contains(id);
    }

    @Test
    void updateIs200() {
        loggedInClient();
        String name = uniqueName();
        String id = json(createSite(name), "$.id");

        var response = client.put("/api/sites/" + id, siteJson(name + " renamed", "[\"https://new.example.com\"]", "0.25"));

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(json(response, "$.name").toString()).isEqualTo(name + " renamed");
        assertThat(((Number) json(response, "$.samplingRate")).doubleValue()).isEqualTo(0.25);
    }

    @Test
    void updateKeepingOwnNameWithDifferentCaseIs200() {
        loggedInClient();
        String name = uniqueName();
        String id = json(createSite(name), "$.id");

        var response = client.put("/api/sites/" + id, siteJson(name.toUpperCase(), "[]", "1.0"));

        assertThat(response.statusCode()).isEqualTo(200);
    }

    @Test
    void updateToAnotherSitesNameIs409() {
        loggedInClient();
        String taken = uniqueName();
        createSite(taken);
        String id = json(createSite(uniqueName()), "$.id");

        assertThat(client.put("/api/sites/" + id, siteJson(taken, "[]", "1.0")).statusCode()).isEqualTo(409);
    }

    @Test
    void updateInvalidIs400() {
        loggedInClient();
        String id = json(createSite(uniqueName()), "$.id");

        var response = client.put("/api/sites/" + id, """
                {"name": "", "allowedOrigins": [""], "samplingRate": -0.1}
                """);

        assertThat(response.statusCode()).isEqualTo(400);
    }

    @Test
    void updateUnknownIdIs404() {
        loggedInClient();

        assertThat(client.put("/api/sites/" + UNKNOWN_ID, siteJson(uniqueName(), "[]", "1.0")).statusCode())
                .isEqualTo(404);
    }

    @Test
    void deleteIs204ThenGoneWith404() {
        loggedInClient();
        String id = json(createSite(uniqueName()), "$.id");

        assertThat(client.delete("/api/sites/" + id).statusCode()).isEqualTo(204);
        assertThat(client.delete("/api/sites/" + id).statusCode()).isEqualTo(404);
        assertThat(client.get("/api/sites/" + id).statusCode()).isEqualTo(404);
    }

    private HttpResponse<String> createSite(String name) {
        return client.post("/api/sites",
                siteJson(name, "[\"https://blog.example.com\", \"https://www.blog.example.com\"]", "0.5"));
    }

    private static String siteJson(String name, String originsJsonArray, String samplingRate) {
        return """
                {"name": "%s", "allowedOrigins": %s, "samplingRate": %s}
                """.formatted(name, originsJsonArray, samplingRate);
    }

    private static String uniqueName() {
        return "Site " + UUID.randomUUID().toString().substring(0, 8);
    }
}
