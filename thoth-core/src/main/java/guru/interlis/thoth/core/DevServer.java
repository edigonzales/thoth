package guru.interlis.thoth.core;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
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
        private static final int BUFFER_SIZE = 16 * 1024;

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

            long fileSize = Files.size(filePath);
            String contentType = contentType(filePath);
            Headers headers = exchange.getResponseHeaders();
            headers.set("Content-Type", contentType);
            headers.set("Accept-Ranges", "bytes");

            ByteRange range = parseRange(exchange.getRequestHeaders().getFirst("Range"), fileSize);
            if (range == ByteRange.UNSATISFIABLE) {
                headers.set("Content-Range", "bytes */" + fileSize);
                exchange.sendResponseHeaders(416, -1);
                exchange.close();
                return;
            }

            boolean head = "HEAD".equalsIgnoreCase(exchange.getRequestMethod());
            if (range != null) {
                long contentLength = range.length();
                headers.set("Content-Range", "bytes " + range.start() + "-" + range.end() + "/" + fileSize);
                if (head) {
                    headers.set("Content-Length", Long.toString(contentLength));
                    exchange.sendResponseHeaders(206, -1);
                    exchange.close();
                    return;
                }
                exchange.sendResponseHeaders(206, contentLength);
                try (OutputStream out = exchange.getResponseBody()) {
                    streamFileRange(filePath, range.start(), contentLength, out);
                }
                return;
            }

            if (head) {
                headers.set("Content-Length", Long.toString(fileSize));
                exchange.sendResponseHeaders(200, -1);
                exchange.close();
                return;
            }

            exchange.sendResponseHeaders(200, fileSize);
            try (OutputStream out = exchange.getResponseBody()) {
                Files.copy(filePath, out);
            }
        }

        private ByteRange parseRange(String rangeHeader, long fileSize) {
            if (rangeHeader == null || rangeHeader.isBlank()) {
                return null;
            }

            String header = rangeHeader.trim();
            if (!header.startsWith("bytes=")) {
                return null;
            }

            String spec = header.substring("bytes=".length()).trim();
            if (spec.isEmpty() || spec.contains(",")) {
                return null;
            }

            try {
                if (spec.startsWith("-")) {
                    long suffixLength = Long.parseLong(spec.substring(1));
                    if (suffixLength <= 0 || fileSize == 0) {
                        return ByteRange.UNSATISFIABLE;
                    }
                    long start = Math.max(0, fileSize - suffixLength);
                    return new ByteRange(start, fileSize - 1);
                }

                int dashIndex = spec.indexOf('-');
                if (dashIndex < 0) {
                    return null;
                }

                long start = Long.parseLong(spec.substring(0, dashIndex).trim());
                String endPart = spec.substring(dashIndex + 1).trim();
                long end = endPart.isEmpty() ? fileSize - 1 : Long.parseLong(endPart);
                if (start < 0 || end < start || start >= fileSize || fileSize == 0) {
                    return ByteRange.UNSATISFIABLE;
                }
                return new ByteRange(start, Math.min(end, fileSize - 1));
            } catch (NumberFormatException ex) {
                return null;
            }
        }

        private void streamFileRange(Path filePath, long start, long length, OutputStream out) throws IOException {
            try (InputStream in = Files.newInputStream(filePath)) {
                in.skipNBytes(start);
                byte[] buffer = new byte[BUFFER_SIZE];
                long remaining = length;
                while (remaining > 0) {
                    int read = in.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                    if (read == -1) {
                        break;
                    }
                    out.write(buffer, 0, read);
                    remaining -= read;
                }
            }
        }

        private record ByteRange(long start, long end) {
            private static final ByteRange UNSATISFIABLE = new ByteRange(-1, -1);

            long length() {
                return end - start + 1;
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
            types.put("jar", "application/java-archive");
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
