package guru.interlis.thoth.core;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DevServerTest {

    @TempDir Path tempDir;

    private DevServer server;
    private int port;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    void fullGetServesFileWithAcceptRanges() throws Exception {
        Path file = writeAsset("asset.bin", "0123456789abcdefghijklmnopqrstuvwxyz");
        HttpClient client = startServer();

        HttpResponse<byte[]> response = client.send(
            request("/asset.bin").build(),
            HttpResponse.BodyHandlers.ofByteArray()
        );

        assertEquals(200, response.statusCode());
        assertEquals(Files.size(file), contentLength(response));
        assertEquals("bytes", header(response, "accept-ranges"));
        assertArrayEquals(Files.readAllBytes(file), response.body());
    }

    @Test
    void headServesHeadersWithoutBody() throws Exception {
        Path file = writeAsset("asset.bin", "0123456789abcdefghijklmnopqrstuvwxyz");
        HttpClient client = startServer();

        HttpResponse<byte[]> response = client.send(
            request("/asset.bin").method("HEAD", HttpRequest.BodyPublishers.noBody()).build(),
            HttpResponse.BodyHandlers.ofByteArray()
        );

        assertEquals(200, response.statusCode());
        assertEquals(Files.size(file), contentLength(response));
        assertEquals("bytes", header(response, "accept-ranges"));
        assertEquals(0, response.body().length);
    }

    @Test
    void servesExplicitByteRange() throws Exception {
        writeAsset("asset.bin", "0123456789abcdefghijklmnopqrstuvwxyz");
        HttpClient client = startServer();

        HttpResponse<byte[]> response = client.send(
            request("/asset.bin").header("Range", "bytes=0-15").build(),
            HttpResponse.BodyHandlers.ofByteArray()
        );

        assertEquals(206, response.statusCode());
        assertEquals("bytes 0-15/36", header(response, "content-range"));
        assertEquals(16, contentLength(response));
        assertEquals("0123456789abcdef", new String(response.body(), StandardCharsets.UTF_8));
    }

    @Test
    void headServesRangeHeadersWithoutBody() throws Exception {
        writeAsset("asset.bin", "0123456789abcdefghijklmnopqrstuvwxyz");
        HttpClient client = startServer();

        HttpResponse<byte[]> response = client.send(
            request("/asset.bin")
                .header("Range", "bytes=0-15")
                .method("HEAD", HttpRequest.BodyPublishers.noBody())
                .build(),
            HttpResponse.BodyHandlers.ofByteArray()
        );

        assertEquals(206, response.statusCode());
        assertEquals("bytes 0-15/36", header(response, "content-range"));
        assertEquals(16, contentLength(response));
        assertEquals(0, response.body().length);
    }

    @Test
    void servesOpenEndedRange() throws Exception {
        writeAsset("asset.bin", "0123456789abcdefghijklmnopqrstuvwxyz");
        HttpClient client = startServer();

        HttpResponse<byte[]> response = client.send(
            request("/asset.bin").header("Range", "bytes=10-").build(),
            HttpResponse.BodyHandlers.ofByteArray()
        );

        assertEquals(206, response.statusCode());
        assertEquals("bytes 10-35/36", header(response, "content-range"));
        assertEquals("abcdefghijklmnopqrstuvwxyz", new String(response.body(), StandardCharsets.UTF_8));
    }

    @Test
    void servesSuffixRange() throws Exception {
        writeAsset("asset.bin", "0123456789abcdefghijklmnopqrstuvwxyz");
        HttpClient client = startServer();

        HttpResponse<byte[]> response = client.send(
            request("/asset.bin").header("Range", "bytes=-8").build(),
            HttpResponse.BodyHandlers.ofByteArray()
        );

        assertEquals(206, response.statusCode());
        assertEquals("bytes 28-35/36", header(response, "content-range"));
        assertEquals("stuvwxyz", new String(response.body(), StandardCharsets.UTF_8));
    }

    @Test
    void clampsRangeEndToFileSize() throws Exception {
        writeAsset("asset.bin", "0123456789abcdefghijklmnopqrstuvwxyz");
        HttpClient client = startServer();

        HttpResponse<byte[]> response = client.send(
            request("/asset.bin").header("Range", "bytes=30-99").build(),
            HttpResponse.BodyHandlers.ofByteArray()
        );

        assertEquals(206, response.statusCode());
        assertEquals("bytes 30-35/36", header(response, "content-range"));
        assertEquals("uvwxyz", new String(response.body(), StandardCharsets.UTF_8));
    }

    @Test
    void outOfRangeRequestReturns416() throws Exception {
        writeAsset("asset.bin", "0123456789abcdefghijklmnopqrstuvwxyz");
        HttpClient client = startServer();

        HttpResponse<byte[]> response = client.send(
            request("/asset.bin").header("Range", "bytes=100-120").build(),
            HttpResponse.BodyHandlers.ofByteArray()
        );

        assertEquals(416, response.statusCode());
        assertEquals("bytes */36", header(response, "content-range"));
        assertEquals("bytes", header(response, "accept-ranges"));
    }

    @Test
    void unsupportedRangesFallBackToFullResponse() throws Exception {
        writeAsset("asset.bin", "0123456789abcdefghijklmnopqrstuvwxyz");
        HttpClient client = startServer();

        HttpResponse<byte[]> unsupportedUnit = client.send(
            request("/asset.bin").header("Range", "items=0-15").build(),
            HttpResponse.BodyHandlers.ofByteArray()
        );
        assertEquals(200, unsupportedUnit.statusCode());
        assertEquals("0123456789abcdefghijklmnopqrstuvwxyz", new String(unsupportedUnit.body(), StandardCharsets.UTF_8));

        HttpResponse<byte[]> multiRange = client.send(
            request("/asset.bin").header("Range", "bytes=0-2,10-12").build(),
            HttpResponse.BodyHandlers.ofByteArray()
        );
        assertEquals(200, multiRange.statusCode());
        assertEquals("0123456789abcdefghijklmnopqrstuvwxyz", new String(multiRange.body(), StandardCharsets.UTF_8));
    }

    @Test
    void keepsExistingErrorBehaviour() throws Exception {
        writeAsset("asset.bin", "0123456789abcdefghijklmnopqrstuvwxyz");
        Files.writeString(tempDir.resolve("secret.txt"), "secret");
        HttpClient client = startServer(tempDir.resolve("site"));

        HttpResponse<String> traversal = client.send(
            request("/%2e%2e/secret.txt").build(),
            HttpResponse.BodyHandlers.ofString()
        );
        assertEquals(404, traversal.statusCode());

        HttpResponse<String> missing = client.send(
            request("/missing.bin").build(),
            HttpResponse.BodyHandlers.ofString()
        );
        assertEquals(404, missing.statusCode());

        HttpResponse<String> methodNotAllowed = client.send(
            request("/asset.bin").POST(HttpRequest.BodyPublishers.noBody()).build(),
            HttpResponse.BodyHandlers.ofString()
        );
        assertEquals(405, methodNotAllowed.statusCode());
    }

    private Path writeAsset(String relativePath, String content) throws IOException {
        Path root = tempDir.resolve("site");
        Path file = root.resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file;
    }

    private HttpClient startServer() throws IOException {
        return startServer(tempDir.resolve("site"));
    }

    private HttpClient startServer(Path root) throws IOException {
        port = findFreePort();
        server = new DevServer(root, port);
        server.start();
        return HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    }

    private HttpRequest.Builder request(String path) {
        return HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + path))
            .timeout(Duration.ofSeconds(5));
    }

    private static String header(HttpResponse<?> response, String name) {
        return response.headers().firstValue(name).orElse("");
    }

    private static long contentLength(HttpResponse<?> response) {
        return Long.parseLong(header(response, "content-length"));
    }

    private static int findFreePort() throws IOException {
        try (ServerSocket socket = new ServerSocket()) {
            socket.bind(new InetSocketAddress("localhost", 0));
            return socket.getLocalPort();
        }
    }
}
