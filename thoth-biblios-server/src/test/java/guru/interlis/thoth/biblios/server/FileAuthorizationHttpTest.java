package guru.interlis.thoth.biblios.server;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FileAuthorizationHttpTest {
    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) throws Exception {
        var fixture = PathTestFixture.create(Files.createTempDirectory("biblios-real-http"));
        registry.add("biblios.package-dir", () -> fixture.packageDir().toString());
        registry.add("biblios.access-config", () -> fixture.accessConfig().toString());
    }
    @Value("${local.server.port}") int port;

    @Test
    void malformedPercentEscapesReturn400OverHttp() throws Exception {
        // URI/HttpClient and MockMvc reject these before reaching the server.
        for (String path : new String[] {"/public-docs/%", "/public-docs/%GG"}) {
            try (var socket = new java.net.Socket("localhost", port)) {
                socket.setSoTimeout(5000);
                socket.getOutputStream().write(("GET " + path + " HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\n\r\n")
                    .getBytes(java.nio.charset.StandardCharsets.US_ASCII));
                var reader = new java.io.BufferedReader(new java.io.InputStreamReader(socket.getInputStream()));
                assertTrue(reader.readLine().startsWith("HTTP/1.1 400"));
            }
        }
    }

    @Test
    void servletContainerPreservesTheAuthorizationBoundary() throws Exception {
        try (var client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build()) {
            for (String path : new String[] {"public+docs/main/attachment.txt", "public%2Bdocs/main/attachment.txt"}) {
                var response = client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/" + path)).build(),
                    HttpResponse.BodyHandlers.ofString());
                assertEquals(404, response.statusCode());
                assertTrue(response.headers().firstValue("Location").isEmpty());
                assertFalse(response.body().contains("PROTECTED PLUS"));
            }
            var hidden = client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port
                + "/public+docs/main/attachment.txt")).build(), HttpResponse.BodyHandlers.ofString());
            var unknown = client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port
                + "/unknown-docs/main/attachment.txt")).build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(hidden.statusCode(), unknown.statusCode());
            assertEquals(hidden.body().replaceAll("returnTo=[^\"]*", "returnTo=TARGET"),
                unknown.body().replaceAll("returnTo=[^\"]*", "returnTo=TARGET"));
            for (String header : new String[] {"Cache-Control", "Vary", "Content-Type", "Location", "Set-Cookie"}) {
                assertEquals(hidden.headers().allValues(header), unknown.headers().allValues(header));
            }
            var response = client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port
                + "/public%20docs/main/attachment.txt")).build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(200, response.statusCode());
            assertEquals("PUBLIC SPACE", response.body());
        }
    }
}
