package guru.interlis.thoth.biblios.server.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;

/** Keeps Spring's HTTP resource semantics without resolving the URL a second time. */
public final class AuthorizedFileRequestHandler extends ResourceHttpRequestHandler {
    @Override
    protected Resource getResource(HttpServletRequest request) {
        return request.getAttribute(AuthorizedFile.ATTRIBUTE) instanceof AuthorizedFile file
            ? file.resource() : null;
    }
}
