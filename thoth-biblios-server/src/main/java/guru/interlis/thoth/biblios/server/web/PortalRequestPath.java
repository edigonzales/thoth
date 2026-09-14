package guru.interlis.thoth.biblios.server.web;

import jakarta.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;

/** Decodes a URI path exactly once, preserving literal plus signs. */
public final class PortalRequestPath {
    private PortalRequestPath() { }

    public static String from(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        for (int i = 0; i < path.length();) {
            if (path.charAt(i) == '%') {
                if (i + 2 >= path.length()) {
                    throw new IllegalArgumentException("Incomplete path escape");
                }
                int high = Character.digit(path.charAt(i + 1), 16);
                int low = Character.digit(path.charAt(i + 2), 16);
                if (high < 0 || low < 0 || path.charAt(i + 1) > 127 || path.charAt(i + 2) > 127) {
                    throw new IllegalArgumentException("Invalid path escape");
                }
                bytes.write(high * 16 + low);
                i += 3;
            } else {
                int end = i + Character.charCount(path.codePointAt(i));
                bytes.writeBytes(path.substring(i, end).getBytes(StandardCharsets.UTF_8));
                i = end;
            }
        }
        try {
            return StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes.toByteArray())).toString();
        } catch (CharacterCodingException e) {
            throw new IllegalArgumentException("Invalid UTF-8 path", e);
        }
    }
}
