package guru.interlis.thoth.biblios.server.web;

import guru.interlis.thoth.biblios.server.access.AccessService;
import guru.interlis.thoth.biblios.server.publication.PublicationPackage;
import guru.interlis.thoth.biblios.server.publication.SearchIndexEntry;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Set;

/**
 * Serves the search index filtered by the access rights of the current visitor.
 * The full index only exists inside the publication package and is never served
 * directly.
 */
@RestController
public class SearchIndexController {

    private final PublicationPackage publicationPackage;
    private final AccessService accessService;
    private final PortalSession portalSession;
    private final ObjectMapper objectMapper;

    public SearchIndexController(PublicationPackage publicationPackage, AccessService accessService,
                                 PortalSession portalSession, ObjectMapper objectMapper) {
        this.publicationPackage = publicationPackage;
        this.accessService = accessService;
        this.portalSession = portalSession;
        this.objectMapper = objectMapper;
    }

    @GetMapping(value = "/api/search-index", produces = MediaType.APPLICATION_JSON_VALUE)
    public String searchIndex(HttpServletRequest request, Authentication authentication) {
        Set<String> allowed = accessService.allowedSourceIds(portalSession.principal(request, authentication));
        List<SearchIndexEntry> filtered = publicationPackage.searchIndex().stream()
            .filter(entry -> allowed.contains(entry.component()))
            .toList();
        return objectMapper.writeValueAsString(filtered);
    }
}
