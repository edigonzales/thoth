package guru.interlis.thoth.biblios.access;

import java.util.Locale;
import java.util.Objects;

/**
 * Reference to an identity provider subject or group.
 *
 * <p>The identity is intentionally opaque: {@code provider} names a configured
 * identity provider (for example {@code entra-kanton} or {@code keycloak-local})
 * and {@code id} is the provider-specific, stable identifier. For Entra ID that is
 * the user or group object ID (never an e-mail address or UPN); for OIDC in general
 * the subject or group identifier issued by the provider.</p>
 *
 * <p>Provider names are compared case-insensitively (normalized to lower case);
 * identifiers are compared exactly, because group identifiers may be paths.</p>
 */
public record SubjectRef(String provider, String id) {

    public SubjectRef {
        provider = normalizeProvider(provider);
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("subject id must not be blank");
        }
        id = id.trim();
    }

    /**
     * Parse a reference from configuration values.
     */
    public static SubjectRef parse(Object provider, Object id, String label) {
        if (!(provider instanceof String providerText) || providerText.isBlank()) {
            throw new IllegalArgumentException(label + ".provider must be a non-blank string");
        }
        if (!(id instanceof String idText) || idText.isBlank()) {
            throw new IllegalArgumentException(label + ".id must be a non-blank string");
        }
        return new SubjectRef(providerText, idText);
    }

    /**
     * Normalize a provider name for comparisons (case-insensitive, trimmed).
     */
    public static String normalizeProvider(String provider) {
        if (provider == null || provider.isBlank()) {
            throw new IllegalArgumentException("subject provider must not be blank");
        }
        return provider.trim().toLowerCase(Locale.ROOT);
    }

    @Override
    public String toString() {
        return provider + ":" + id;
    }
}
