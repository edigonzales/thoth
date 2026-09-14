package guru.interlis.thoth.biblios.server.security;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.net.URISyntaxException;

/** One-use local return targets, set only by an explicit login action. */
public final class LoginReturnTarget {
    private static final String PENDING = LoginReturnTarget.class.getName() + ".pending";
    private static final String ACTIVE = LoginReturnTarget.class.getName() + ".active";
    private LoginReturnTarget() { }

    private record Intent(String token, String target) implements java.io.Serializable { }

    public static String prepare(HttpServletRequest request) {
        var session = request.getSession(true);
        session.removeAttribute(ACTIVE);
        String token = java.util.UUID.randomUUID().toString();
        session.setAttribute(PENDING, new Intent(token, validate(request.getParameter("returnTo"), request.getContextPath())));
        return token;
    }

    static void beginAuthorization(HttpServletRequest request) {
        var session = request.getSession(false);
        if (session == null) return;
        Object pending = session.getAttribute(PENDING);
        session.removeAttribute(PENDING);
        session.removeAttribute(ACTIVE);
        if (pending instanceof Intent intent && intent.token().equals(request.getParameter("loginIntent"))) {
            session.setAttribute(ACTIVE, intent.target());
        }
    }

    static String consume(HttpServletRequest request) {
        var session = request.getSession(false);
        Object target = session != null ? session.getAttribute(ACTIVE) : null;
        if (session != null) {
            session.removeAttribute(ACTIVE);
            session.removeAttribute(PENDING);
        }
        return validate(target instanceof String value ? value : null, request.getContextPath());
    }

    static String validate(String target, String context) {
        String fallback = context + "/";
        if (target == null || unsafeCharacters(target)) return fallback;
        try {
            URI uri = new URI(target);
            if (uri.isAbsolute() || uri.getRawAuthority() != null || uri.getRawFragment() != null
                || uri.getRawPath() == null || !uri.getRawPath().startsWith("/") || uri.getRawPath().startsWith("//")) return fallback;
            String decoded = uri.getPath();
            if (unsafeCharacters(decoded) || decoded.startsWith("//")
                || (uri.getQuery() != null && unsafeCharacters(uri.getQuery()))) return fallback;
            String normalized = new URI(null, null, decoded, null).normalize().getPath();
            if (!normalized.startsWith(context + "/") || normalized.startsWith("//")
                || normalized.contains("/../") || normalized.endsWith("/..")) return fallback;
            String local = normalized.substring(context.length());
            for (String reserved : new String[] {"/login", "/logout", "/oauth2"}) {
                if (local.equals(reserved) || local.startsWith(reserved + "/") || local.startsWith(reserved + ";")) return fallback;
            }
            return uri.normalize().toASCIIString();
        } catch (URISyntaxException | IllegalArgumentException e) {
            return fallback;
        }
    }

    private static boolean unsafeCharacters(String text) {
        return text.indexOf('\\') >= 0 || text.codePoints().anyMatch(Character::isISOControl);
    }
}
