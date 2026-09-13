package guru.interlis.thoth.biblios.access;

import java.util.Objects;
import java.util.Set;

/**
 * Normalized identity of an authenticated principal.
 *
 * <p>Built by the server from the OIDC token: the {@code provider} is the name of the
 * configured identity provider, {@code subject} is the stable provider subject
 * (Entra ID: {@code oid}; OIDC: {@code sub}) and {@code groups} are group references
 * issued by that provider. E-mail addresses and display names are for presentation
 * only and never used for access decisions.</p>
 */
public record PrincipalIdentity(String provider, String subject, String displayName, Set<SubjectRef> groups) {

    public PrincipalIdentity {
        provider = SubjectRef.normalizeProvider(provider);
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("principal subject must not be blank");
        }
        subject = subject.trim();
        groups = Set.copyOf(Objects.requireNonNullElse(groups, Set.of()));
    }

    public static PrincipalIdentity of(String provider, String subject, String displayName, Set<SubjectRef> groups) {
        return new PrincipalIdentity(provider, subject, displayName, groups);
    }

    @Override
    public String toString() {
        return "PrincipalIdentity{provider='" + provider + "', subject='" + subject
            + "', groups=" + groups.size() + "}";
    }
}
