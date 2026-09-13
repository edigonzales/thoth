package guru.interlis.thoth.biblios.server.web;

import guru.interlis.thoth.biblios.access.PrincipalIdentity;
import guru.interlis.thoth.biblios.server.access.AccessService;
import guru.interlis.thoth.biblios.server.config.BibliosServerProperties;
import guru.interlis.thoth.biblios.server.publication.PublicationPackage;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Serves documentation pages and package files for all nested portal paths.
 *
 * <p>Page routes come from the catalog; everything else must be a manifest entry
 * of kind {@code file}. Unknown and internal paths are 404. Files are delegated
 * to Spring's resource handler (range requests, HEAD, conditional requests) and
 * require the access policy of their documentation.</p>
 */
@RestController
public class ContentController {

    private final PortalFrames frames;
    private final AccessService accessService;
    private final PublicationPackage publicationPackage;
    private final PortalSession portalSession;
    private final BibliosServerProperties properties;
    private final ResourceHttpRequestHandler fileRequestHandler;
    private final RequestCache requestCache;

    public ContentController(PortalFrames frames, AccessService accessService,
                             PublicationPackage publicationPackage, PortalSession portalSession,
                             BibliosServerProperties properties,
                             ResourceHttpRequestHandler packageFileRequestHandler,
                             RequestCache requestCache) {
        this.frames = frames;
        this.accessService = accessService;
        this.publicationPackage = publicationPackage;
        this.portalSession = portalSession;
        this.properties = properties;
        this.fileRequestHandler = packageFileRequestHandler;
        this.requestCache = requestCache;
    }

    @GetMapping("/**")
    public void handle(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
        throws Exception {
        String path = requestPath(request);
        PublicationPackage.PageLocation location = publicationPackage.page(path);
        if (location == null && !path.endsWith("/")) {
            location = publicationPackage.page(path + "/");
            if (location != null) {
                redirect(response, path + "/");
                return;
            }
        }
        if (location == null) {
            String defaultRoute = publicationPackage.defaultPageRouteForVersionRoot(path);
            if (defaultRoute != null) {
                redirect(response, defaultRoute);
                return;
            }
        }

        PortalFrames.FrameSession session = portalSession.frameSession(request, authentication);
        PrincipalIdentity principal = session.principal();
        if (location != null) {
            if (!accessService.canAccessSource(location.componentId(), principal)) {
                denied(request, response, principal);
                return;
            }
            writeHtml(response, frames.page(location, session));
            return;
        }

        String target = path.startsWith("/") ? path.substring(1) : path;
        var entry = publicationPackage.findFile(target);
        if (entry.isEmpty()
            || !"file".equals(entry.get().kind())
            || "internal".equals(entry.get().scope())) {
            notFound(response);
            return;
        }
        boolean shared = "shared".equals(entry.get().scope());
        if (!shared && !accessService.canAccessSource(entry.get().source(), principal)) {
            denied(request, response, principal);
            return;
        }
        Path file = publicationPackage.resolvePackagePath("files/" + target);
        if (!Files.isRegularFile(file)) {
            notFound(response);
            return;
        }
        if (shared) {
            response.setHeader(HttpHeaders.CACHE_CONTROL, "public, max-age=300");
        } else {
            response.setHeader(HttpHeaders.CACHE_CONTROL, "private, no-store");
            response.setHeader(HttpHeaders.VARY, "Cookie");
        }
        fileRequestHandler.handleRequest(request, response);
    }

    private void writeHtml(HttpServletResponse response, String html) throws IOException {
        response.setStatus(HttpStatus.OK.value());
        response.setContentType(MediaType.TEXT_HTML_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader(HttpHeaders.CACHE_CONTROL, "private, no-store");
        response.setHeader(HttpHeaders.VARY, "Cookie");
        response.getWriter().write(html);
    }

    private void denied(HttpServletRequest request, HttpServletResponse response, PrincipalIdentity principal)
        throws IOException {
        if (principal == null) {
            requestCache.saveRequest(request, response);
            redirect(response, "/oauth2/authorization/" + properties.getRegistrationId());
            return;
        }
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.TEXT_HTML_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(
            "<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"UTF-8\"><title>Access denied</title></head>"
                + "<body><h1>Access denied</h1><p>You do not have permission to read this documentation.</p>"
                + "<p><a href=\"/\">Back to the portal</a></p></body></html>");
    }

    private void notFound(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.NOT_FOUND.value());
        response.setContentType(MediaType.TEXT_HTML_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(
            "<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"UTF-8\"><title>Not found</title></head>"
                + "<body><h1>Not found</h1></body></html>");
    }

    private void redirect(HttpServletResponse response, String location) throws IOException {
        response.setStatus(HttpStatus.FOUND.value());
        response.setHeader(HttpHeaders.LOCATION, location);
    }

    private String requestPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        String context = request.getContextPath();
        if (context != null && !context.isEmpty() && path.startsWith(context)) {
            path = path.substring(context.length());
        }
        return URLDecoder.decode(path, StandardCharsets.UTF_8);
    }
}
