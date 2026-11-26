package me.dani.lookout;

import com.jayway.jsonpath.JsonPath;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;

public final class ApiClient {

    private final String baseUrl;
    private final CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
    private final HttpClient http;
    private boolean sendCsrf = true;

    public ApiClient(int port) {
        this.baseUrl = "http://localhost:" + port;
        this.http = HttpClient.newBuilder().cookieHandler(cookies).build();
    }

    public ApiClient withoutCsrf() {
        this.sendCsrf = false;
        return this;
    }

    public HttpResponse<String> get(String path) {
        return send(request(path).GET(), false);
    }

    public HttpResponse<String> post(String path, String json) {
        return send(request(path).POST(body(json)), true);
    }

    public HttpResponse<String> put(String path, String json) {
        return send(request(path).PUT(body(json)), true);
    }

    public HttpResponse<String> delete(String path) {
        return send(request(path).DELETE(), true);
    }

    public HttpResponse<String> login(String email, String password) {
        ensureCsrfCookie();
        return post("/api/auth/login", """
                {"email": "%s", "password": "%s"}
                """.formatted(email, password));
    }

    public HttpResponse<String> logout() {
        return post("/api/auth/logout", null);
    }

    public Optional<String> cookie(String name) {
        return cookies.getCookieStore().getCookies().stream()
                .filter(c -> c.getName().equals(name))
                .map(HttpCookie::getValue)
                .findFirst();
    }

    public static <T> T json(HttpResponse<String> response, String path) {
        return JsonPath.read(response.body(), path);
    }

    private void ensureCsrfCookie() {
        if (cookie("XSRF-TOKEN").isEmpty()) {
            get("/api/auth/me");
        }
    }

    private HttpRequest.Builder request(String path) {
        return HttpRequest.newBuilder(URI.create(baseUrl + path));
    }

    private static HttpRequest.BodyPublisher body(String json) {
        return json == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(json);
    }

    private HttpResponse<String> send(HttpRequest.Builder builder, boolean mutating) {
        builder.header("Content-Type", "application/json");
        if (mutating && sendCsrf) {
            cookie("XSRF-TOKEN").ifPresent(token -> builder.header("X-XSRF-TOKEN", token));
        }
        sendCsrf = true;
        try {
            return http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
