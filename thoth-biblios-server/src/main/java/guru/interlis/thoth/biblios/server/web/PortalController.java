package guru.interlis.thoth.biblios.server.web;

import guru.interlis.thoth.biblios.access.PrincipalIdentity;
import guru.interlis.thoth.biblios.server.access.AccessService;
import guru.interlis.thoth.biblios.server.config.BibliosServerProperties;
import guru.interlis.thoth.biblios.server.publication.PackageCatalog;
import guru.interlis.thoth.biblios.server.publication.PublicationPackage;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Portal routes: home, component landing pages and search.
 * Access decisions happen here (not in the servlet filter chain), because they
 * depend on the per-documentation policies of the publication package.
 */
@RestController
public class PortalController {

    private final PortalFrames frames;
    private final AccessService accessService;
    private final PublicationPackage publicationPackage;
    private final PortalSession portalSession;
    private final BibliosServerProperties properties;
    private final RequestCache requestCache;

    public PortalController(PortalFrames frames, AccessService accessService,
                            PublicationPackage publicationPackage, PortalSession portalSession,
                            BibliosServerProperties properties, RequestCache requestCache) {
        this.frames = frames;
        this.accessService = accessService;
        this.publicationPackage = publicationPackage;
        this.portalSession = portalSession;
        this.properties = properties;
        this.requestCache = requestCache;
    }

    @GetMapping(value = {"/", ""}, produces = MediaType.TEXT_HTML_VALUE)
    public String home(HttpServletRequest request, Authentication authentication) {
        return frames.home(portalSession.frameSession(request, authentication));
    }

    @GetMapping(value = "/search", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> searchWithoutSlash() {
        return redirect("/search/");
    }

    @GetMapping(value = "/search/", produces = MediaType.TEXT_HTML_VALUE)
    public String search(HttpServletRequest request, Authentication authentication) {
        return frames.search(portalSession.frameSession(request, authentication));
    }

    @GetMapping(value = "/{component}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> componentWithoutSlash(@PathVariable String component) {
        if (publicationPackage.component(component) == null) {
            return notFound();
        }
        return redirect("/" + component + "/");
    }

    @GetMapping(value = "/{component}/", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> component(@PathVariable String component, HttpServletRequest request,
                                            HttpServletResponse response, Authentication authentication) {
        PackageCatalog.ComponentEntry entry = publicationPackage.component(component);
        if (entry == null) {
            return notFound();
        }
        PortalFrames.FrameSession session = portalSession.frameSession(request, authentication);
        if (!accessService.canAccessSource(component, session.principal())) {
            return denied(session.principal(), request, response);
        }
        return html(frames.component(entry, session));
    }

    private ResponseEntity<String> denied(PrincipalIdentity principal,
                                          HttpServletRequest request, HttpServletResponse response) {
        if (principal == null) {
            requestCache.saveRequest(request, response);
            return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, "/oauth2/authorization/" + properties.getRegistrationId())
                .build();
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .contentType(MediaType.TEXT_HTML)
            .body("<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"UTF-8\"><title>Access denied</title></head>"
                + "<body><h1>Access denied</h1><p>You do not have permission to read this documentation.</p>"
                + "<p><a href=\"/\">Back to the portal</a></p></body></html>");
    }

    private ResponseEntity<String> notFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .contentType(MediaType.TEXT_HTML)
            .body("<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"UTF-8\"><title>Not found</title></head>"
                + "<body><h1>Not found</h1></body></html>");
    }

    private ResponseEntity<String> html(String content) {
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(content);
    }

    private ResponseEntity<String> redirect(String location) {
        return ResponseEntity.status(HttpStatus.FOUND)
            .header(HttpHeaders.LOCATION, location)
            .build();
    }
}
