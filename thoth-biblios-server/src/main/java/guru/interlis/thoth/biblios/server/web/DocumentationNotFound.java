package guru.interlis.thoth.biblios.server.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.HtmlUtils;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/** Identical missing/hidden resource responses, independent of catalog metadata. */
final class DocumentationNotFound {
    private DocumentationNotFound() { }

    static ResponseEntity<String> response(HttpServletRequest request) {
        String target = request.getRequestURI();
        if (request.getQueryString() != null) target += "?" + request.getQueryString();
        String login = request.getContextPath() + "/login?returnTo=" + URLEncoder.encode(target, StandardCharsets.UTF_8);
        String body = "<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"UTF-8\"><title>Not found</title></head>"
            + "<body><h1>Not found</h1><p><a class=\"login-link\" href=\"" + HtmlUtils.htmlEscape(login)
            + "\">Sign in</a></p><p><a href=\"" + HtmlUtils.htmlEscape(request.getContextPath() + "/")
            + "\">Back to the portal</a></p></body></html>";
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .contentType(new MediaType("text", "html", StandardCharsets.UTF_8))
            .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
            .header(HttpHeaders.VARY, "Cookie")
            .contentLength(body.getBytes(StandardCharsets.UTF_8).length).body(body);
    }

    static void write(HttpServletRequest request, HttpServletResponse response) throws IOException {
        var result = response(request);
        response.setStatus(result.getStatusCode().value());
        result.getHeaders().forEach((key, values) -> response.setHeader(key, String.join(", ", values)));
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        if (!request.getMethod().equals("HEAD")) response.getWriter().write(result.getBody());
    }
}
