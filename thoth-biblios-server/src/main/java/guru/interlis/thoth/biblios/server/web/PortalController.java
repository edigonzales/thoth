package guru.interlis.thoth.biblios.server.web;

import guru.interlis.thoth.biblios.server.access.AccessService;
import guru.interlis.thoth.biblios.server.publication.PackageCatalog;
import guru.interlis.thoth.biblios.server.publication.PublicationPackage;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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

    public PortalController(PortalFrames frames, AccessService accessService,
                            PublicationPackage publicationPackage, PortalSession portalSession) {
        this.frames = frames;
        this.accessService = accessService;
        this.publicationPackage = publicationPackage;
        this.portalSession = portalSession;
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
    public ResponseEntity<String> componentWithoutSlash(@PathVariable String component,
                                                        HttpServletRequest request, Authentication authentication) {
        var principal = portalSession.principal(request, authentication);
        if (publicationPackage.component(component) == null || !accessService.canAccessSource(component, principal)) {
            return DocumentationNotFound.response(request);
        }
        return redirect("/" + component + "/");
    }

    @GetMapping(value = "/{component}/", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> component(@PathVariable String component, HttpServletRequest request,
                                            Authentication authentication) {
        var principal = portalSession.principal(request, authentication);
        PackageCatalog.ComponentEntry entry = publicationPackage.component(component);
        if (entry == null) {
            return DocumentationNotFound.response(request);
        }
        if (!accessService.canAccessSource(component, principal)) {
            return DocumentationNotFound.response(request);
        }
        return html(frames.component(entry, portalSession.frameSessionForPrincipal(request, principal)));
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
