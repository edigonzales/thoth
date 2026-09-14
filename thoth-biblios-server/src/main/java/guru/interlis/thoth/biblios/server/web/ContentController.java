package guru.interlis.thoth.biblios.server.web;

import guru.interlis.thoth.biblios.access.PrincipalIdentity;
import guru.interlis.thoth.biblios.server.access.AccessService;
import guru.interlis.thoth.biblios.server.publication.PublicationPackage;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;

import java.io.IOException;
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
    private final ResourceHttpRequestHandler fileRequestHandler;

    public ContentController(PortalFrames frames, AccessService accessService,
                             PublicationPackage publicationPackage, PortalSession portalSession,
                             ResourceHttpRequestHandler packageFileRequestHandler) {
        this.frames = frames;
        this.accessService = accessService;
        this.publicationPackage = publicationPackage;
        this.portalSession = portalSession;
        this.fileRequestHandler = packageFileRequestHandler;
    }

    @GetMapping("/**")
    public void handle(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
        throws Exception {
        final String path;
        try {
            path = PortalRequestPath.from(request);
        } catch (IllegalArgumentException e) {
            response.sendError(HttpStatus.BAD_REQUEST.value());
            return;
        }
        PrincipalIdentity principal = portalSession.principal(request, authentication);
        PublicationPackage.PageLocation location = publicationPackage.page(path);
        if (location == null) {
            var target = publicationPackage.defaultPageRouteForVersionRoot(path);
            if (target != null) {
                if (!accessService.canAccessSource(target.sourceId(), principal)) {
                    DocumentationNotFound.write(request, response);
                    return;
                }
                redirect(response, target.route());
                return;
            }
        }

        if (location != null) {
            if (!accessService.canAccessSource(location.componentId(), principal)) {
                DocumentationNotFound.write(request, response);
                return;
            }
            if (!path.endsWith("/")) {
                redirect(response, path + "/");
                return;
            }
            writeHtml(response, frames.page(location, portalSession.frameSessionForPrincipal(request, principal)));
            return;
        }

        String target = path.startsWith("/") ? path.substring(1) : path;
        var entry = publicationPackage.findFile(target);
        if (entry.isEmpty()
            || !"file".equals(entry.get().kind())
            || "internal".equals(entry.get().scope())) {
            DocumentationNotFound.write(request, response);
            return;
        }
        boolean shared = "shared".equals(entry.get().scope());
        if (!shared && !accessService.canAccessSource(entry.get().source(), principal)) {
            DocumentationNotFound.write(request, response);
            return;
        }
        final Path file;
        try {
            file = publicationPackage.resolveFilePath(target);
        } catch (IllegalArgumentException | IOException e) {
            DocumentationNotFound.write(request, response);
            return;
        }
        if (!Files.isRegularFile(file)) {
            DocumentationNotFound.write(request, response);
            return;
        }
        if (shared) {
            response.setHeader(HttpHeaders.CACHE_CONTROL, "public, max-age=300");
        } else {
            response.setHeader(HttpHeaders.CACHE_CONTROL, "private, no-store");
            response.setHeader(HttpHeaders.VARY, "Cookie");
        }
        request.setAttribute(AuthorizedFile.ATTRIBUTE,
            new AuthorizedFile(entry.get(), new FileSystemResource(file)));
        try {
            fileRequestHandler.handleRequest(request, response);
        } finally {
            request.removeAttribute(AuthorizedFile.ATTRIBUTE);
        }
    }

    private void writeHtml(HttpServletResponse response, String html) throws IOException {
        response.setStatus(HttpStatus.OK.value());
        response.setContentType(MediaType.TEXT_HTML_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader(HttpHeaders.CACHE_CONTROL, "private, no-store");
        response.setHeader(HttpHeaders.VARY, "Cookie");
        response.getWriter().write(html);
    }

    private void redirect(HttpServletResponse response, String location) throws IOException {
        response.setStatus(HttpStatus.FOUND.value());
        response.setHeader(HttpHeaders.LOCATION, location);
    }
}
