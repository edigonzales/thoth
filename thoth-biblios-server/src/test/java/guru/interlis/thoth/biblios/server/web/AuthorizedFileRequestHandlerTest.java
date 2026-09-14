package guru.interlis.thoth.biblios.server.web;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.core.io.FileSystemResource;
import static org.junit.jupiter.api.Assertions.*;

class AuthorizedFileRequestHandlerTest {
    @Test void handlerHasNoUrlBasedFallbackWithoutAnAuthorizedFile() {
        var handler = new AuthorizedFileRequestHandler();
        var request = new MockHttpServletRequest("GET", "/site-assets/styles.css");
        assertNull(handler.getResource(request));
        request.setAttribute(AuthorizedFile.ATTRIBUTE, new FileSystemResource("/tmp/untrusted"));
        assertNull(handler.getResource(request));
    }
}
