package guru.interlis.thoth.biblios.server;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Minimal browser-like OIDC client for end-to-end tests: follows the
 * authorization code flow by posting the Keycloak login form.
 *
 * <p>Cookies are tracked manually because {@link java.net.CookieManager} maps
 * single-label hosts such as {@code localhost} to a {@code localhost.local}
 * domain and therefore never sends the Keycloak session cookies.</p>
 */
final class OidcTestClient {
    private static final Pattern FORM_ACTION = Pattern.compile("action=\"([^\"]+)\"");

    private final HttpClient httpClient;
    private final String baseUrl;
    private final Map<String, String> cookies = new LinkedHashMap<>();

    OidcTestClient(String baseUrl) {
        this.baseUrl = baseUrl;
        this.httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER)
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    }

    /**
     * Performs a full login and returns the response of the requested path.
     */
    HttpResponse<String> loginAndGet(String username, String password, String path) throws Exception {
        HttpResponse<String> response = get(baseUrl + path);

        if (response.statusCode() != 404) {
            throw new IllegalStateException("Expected a generic 404 before explicit login: " + response.statusCode());
        }
        var link = Pattern.compile("class=\"login-link\" href=\"([^\"]+)\"").matcher(response.body());
        if (!link.find()) throw new IllegalStateException("No explicit login link on 404 page");
        String loginStart = unescape(link.group(1));
        response = get(resolve(baseUrl, loginStart));

        // /login stores the local target, then redirects to the selected registration.
        response = get(resolve(baseUrl, location(response)));
        String authorizationEndpoint = location(response);
        response = get(authorizationEndpoint);

        String formAction = extractFormAction(response.body());
        String loginUrl = unescape(formAction);
        response = post(loginUrl, "username=" + encode(username) + "&password=" + encode(password));
        if (response.statusCode() != 302) {
            throw new IllegalStateException("Login POST to " + loginUrl + " returned "
                + response.statusCode() + ": " + preview(response));
        }

        String callback = location(response);
        response = get(callback);

        while (response.statusCode() >= 300 && response.statusCode() < 400) {
            response = get(resolve(baseUrl, location(response)));
        }
        return response;
    }

    /**
     * GET a path with the current session (without starting a login).
     */
    HttpResponse<String> getPath(String path) throws IOException, InterruptedException {
        return get(baseUrl + path);
    }

    private String extractFormAction(String html) {
        Matcher matcher = FORM_ACTION.matcher(html);
        if (!matcher.find()) {
            throw new IllegalStateException("No login form found in Keycloak response");
        }
        return matcher.group(1);
    }

    private String location(HttpResponse<String> response) {
        return response.headers().firstValue("Location")
            .orElseThrow(() -> new IllegalStateException(
                "Expected redirect, got " + response.statusCode() + ": " + preview(response)));
    }

    private String preview(HttpResponse<String> response) {
        String body = response.body() == null ? "" : response.body().replaceAll("\\s+", " ");
        return body.length() > 2000 ? body.substring(0, 2000) + "..." : body;
    }

    private HttpResponse<String> get(String url) throws IOException, InterruptedException {
        return send(HttpRequest.newBuilder(URI.create(url)).GET());
    }

    private HttpResponse<String> post(String url, String form) throws IOException, InterruptedException {
        return send(HttpRequest.newBuilder(URI.create(url))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(form)));
    }

    private HttpResponse<String> send(HttpRequest.Builder builder) throws IOException, InterruptedException {
        if (!cookies.isEmpty()) {
            builder.header("Cookie", cookieHeader());
        }
        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        storeCookies(response);
        return response;
    }

    private String cookieHeader() {
        StringBuilder header = new StringBuilder();
        for (Map.Entry<String, String> cookie : cookies.entrySet()) {
            if (!header.isEmpty()) {
                header.append("; ");
            }
            header.append(cookie.getKey()).append('=').append(cookie.getValue());
        }
        return header.toString();
    }

    private void storeCookies(HttpResponse<String> response) {
        for (String setCookie : response.headers().allValues("set-cookie")) {
            String pair = setCookie.split(";", 2)[0].trim();
            int equals = pair.indexOf('=');
            if (equals <= 0) {
                continue;
            }
            String name = pair.substring(0, equals).trim();
            String value = pair.substring(equals + 1).trim();
            boolean expired = setCookie.toLowerCase().contains("max-age=0")
                || setCookie.toLowerCase().contains("expires=thu, 01 jan 1970");
            if (expired) {
                cookies.remove(name);
            } else {
                cookies.put(name, value);
            }
        }
    }

    private String resolve(String base, String location) {
        if (location.startsWith("http://") || location.startsWith("https://")) {
            return location;
        }
        return base + location;
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String unescape(String html) {
        return html.replace("&amp;", "&")
            .replace("&#x2F;", "/")
            .replace("&quot;", "\"");
    }
}
