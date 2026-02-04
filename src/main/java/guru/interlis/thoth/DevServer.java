package guru.interlis.thoth;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class DevServer {
    private final Path outputRoot;
    private final int port;

    private HttpServer server;
    private ExecutorService executor;

    public DevServer(Path outputRoot, int port) {
        this.outputRoot = outputRoot.toAbsolutePath().normalize();
        this.port = port;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        executor = Executors.newFixedThreadPool(4);
        server.createContext("/", new StaticFileHandler(outputRoot));
        server.setExecutor(executor);
        server.start();
        System.out.println("[serve] http://localhost:" + port + "/");
    }

    public void stop() {
        if (server != null) {
            server.stop(1);
        }
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    private static final class StaticFileHandler implements HttpHandler {
        private static final Map<String, String> CONTENT_TYPES = createContentTypes();

        private final Path outputRoot;

        private StaticFileHandler(Path outputRoot) {
            this.outputRoot = outputRoot;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())
                && !"HEAD".equalsIgnoreCase(exchange.getRequestMethod())) {
                send(exchange, 405, "Method Not Allowed", "text/plain; charset=utf-8");
                return;
            }

            Path filePath = resolvePath(exchange.getRequestURI().getPath());
            if (filePath == null || !Files.exists(filePath) || Files.isDirectory(filePath)) {
                send(exchange, 404, "Not Found", "text/plain; charset=utf-8");
                return;
            }

            byte[] body = Files.readAllBytes(filePath);
            String contentType = contentType(filePath);
            Headers headers = exchange.getResponseHeaders();
            headers.set("Content-Type", contentType);

            if ("HEAD".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, -1);
                exchange.close();
                return;
            }

            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        }

        private Path resolvePath(String rawPath) {
            String decoded = URLDecoder.decode(rawPath, StandardCharsets.UTF_8);
            if (decoded.isBlank() || "/".equals(decoded)) {
                decoded = "/index.html";
            }

            String withoutLeadingSlash = decoded.startsWith("/") ? decoded.substring(1) : decoded;
            Path candidate = outputRoot.resolve(withoutLeadingSlash).normalize();
            if (!candidate.startsWith(outputRoot)) {
                return null;
            }

            if (Files.isDirectory(candidate)) {
                return candidate.resolve("index.html");
            }

            if (!Files.exists(candidate) && !withoutLeadingSlash.endsWith(".html")) {
                Path prettyUrlCandidate = outputRoot.resolve(withoutLeadingSlash).resolve("index.html").normalize();
                if (prettyUrlCandidate.startsWith(outputRoot) && Files.exists(prettyUrlCandidate)) {
                    return prettyUrlCandidate;
                }
            }

            return candidate;
        }

        private String contentType(Path file) throws IOException {
            String probed = Files.probeContentType(file);
            if (probed != null) {
                return probed;
            }

            String fileName = file.getFileName().toString();
            int extensionIndex = fileName.lastIndexOf('.');
            if (extensionIndex < 0) {
                return "application/octet-stream";
            }

            String extension = fileName.substring(extensionIndex + 1).toLowerCase();
            return CONTENT_TYPES.getOrDefault(extension, "application/octet-stream");
        }

        private static Map<String, String> createContentTypes() {
            Map<String, String> types = new HashMap<>();
            types.put("html", "text/html; charset=utf-8");
            types.put("css", "text/css; charset=utf-8");
            types.put("js", "application/javascript; charset=utf-8");
            types.put("json", "application/json; charset=utf-8");
            types.put("xml", "application/rss+xml; charset=utf-8");
            types.put("png", "image/png");
            types.put("jpg", "image/jpeg");
            types.put("jpeg", "image/jpeg");
            types.put("gif", "image/gif");
            types.put("svg", "image/svg+xml");
            types.put("woff2", "font/woff2");
            types.put("woff", "font/woff");
            return types;
        }

        private void send(HttpExchange exchange, int statusCode, String body, String contentType) throws IOException {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(statusCode, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        }
    }
}
