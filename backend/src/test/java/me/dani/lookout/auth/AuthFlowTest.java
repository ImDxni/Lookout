package me.dani.lookout.auth;

import me.dani.lookout.ApiIntegrationTest;
import org.junit.jupiter.api.Test;

import static me.dani.lookout.ApiClient.json;
import static org.assertj.core.api.Assertions.assertThat;

class AuthFlowTest extends ApiIntegrationTest {

    @Test
    void meWithoutLoginIs401() {
        assertThat(client.get("/api/auth/me").statusCode()).isEqualTo(401);
    }

    @Test
    void firstRequestReceivesCsrfCookie() {
        client.get("/api/auth/me");

        assertThat(client.cookie("XSRF-TOKEN")).isPresent();
    }

    @Test
    void loginWithoutCsrfHeaderIs403() {
        client.get("/api/auth/me");

        var response = client.withoutCsrf().post("/api/auth/login", """
                {"email": "%s", "password": "%s"}
                """.formatted(ADMIN_EMAIL, ADMIN_PASSWORD));

        assertThat(response.statusCode()).isEqualTo(403);
    }

    @Test
    void loginWithWrongPasswordIs401WithGenericMessage() {
        var response = client.login(ADMIN_EMAIL, ADMIN_PASSWORD + "-wrong");

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(json(response, "$.detail").toString()).isEqualTo("Wrong credentials");
    }

    @Test
    void loginWithUnknownEmailGivesSameResponseAsWrongPassword() {
        var unknown = client.login("nobody@example.com", ADMIN_PASSWORD);

        assertThat(unknown.statusCode()).isEqualTo(401);
        assertThat(json(unknown, "$.detail").toString()).isEqualTo("Wrong credentials");
    }

    @Test
    void loginCreatesSessionAndMeReturnsEmail() {
        var login = client.login(ADMIN_EMAIL, ADMIN_PASSWORD);

        assertThat(login.statusCode()).isEqualTo(200);
        assertThat(client.cookie("JSESSIONID")).isPresent();

        var me = client.get("/api/auth/me");
        assertThat(me.statusCode()).isEqualTo(200);
        assertThat(me.body()).isEqualTo(ADMIN_EMAIL);
    }

    @Test
    void loginIgnoresEmailCaseAndSurroundingSpaces() {
        var login = client.login("  " + ADMIN_EMAIL.toUpperCase() + " ", ADMIN_PASSWORD);

        assertThat(login.statusCode()).isEqualTo(200);
    }

    @Test
    void loginRotatesCsrfToken() {
        client.get("/api/auth/me");
        String before = client.cookie("XSRF-TOKEN").orElseThrow();

        client.login(ADMIN_EMAIL, ADMIN_PASSWORD);

        assertThat(client.cookie("XSRF-TOKEN")).isPresent().get().isNotEqualTo(before);
    }

    @Test
    void loginChangesExistingSessionId() {
        client.login(ADMIN_EMAIL, ADMIN_PASSWORD);
        String firstSession = client.cookie("JSESSIONID").orElseThrow();

        client.login(ADMIN_EMAIL, ADMIN_PASSWORD);

        assertThat(client.cookie("JSESSIONID")).isPresent().get().isNotEqualTo(firstSession);
    }

    @Test
    void logoutIs204AndEndsTheSession() {
        client.login(ADMIN_EMAIL, ADMIN_PASSWORD);

        assertThat(client.logout().statusCode()).isEqualTo(204);
        assertThat(client.get("/api/auth/me").statusCode()).isEqualTo(401);
        assertThat(client.get("/api/sites").statusCode()).isEqualTo(401);
    }

    @Test
    void logoutWithoutCsrfHeaderIs403() {
        client.login(ADMIN_EMAIL, ADMIN_PASSWORD);

        assertThat(client.withoutCsrf().post("/api/auth/logout", null).statusCode()).isEqualTo(403);
        assertThat(client.get("/api/auth/me").statusCode()).isEqualTo(200);
    }

    @Test
    void healthIsPublic() {
        assertThat(client.get("/actuator/health").statusCode()).isEqualTo(200);
    }
}
