package guru.interlis.thoth.biblios.server.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import static org.junit.jupiter.api.Assertions.*;

class PortalRequestPathTest {
    @Test void decodesOncePreservingPlusAndStrippingContext() {
        var request = new MockHttpServletRequest("GET", "/portal/a+b%20c%252B%C3%BC.txt");
        request.setContextPath("/portal");
        assertEquals("/a+b c%2Bü.txt", PortalRequestPath.from(request));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/%", "/%GG", "/%FF", "/%C0%AF", "/%４１"})
    void malformedEncodingIsNotSilentlyReplaced(String path) {
        assertThrows(IllegalArgumentException.class,
            () -> PortalRequestPath.from(new MockHttpServletRequest("GET", path)));
    }
}
