package guru.interlis.thoth.biblios.server.access;

import guru.interlis.thoth.biblios.access.PrincipalIdentity;
import guru.interlis.thoth.biblios.access.SubjectRef;
import guru.interlis.thoth.biblios.server.config.BibliosServerProperties;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Maps an OIDC user to the normalized {@link PrincipalIdentity} used by the access model.
 * Subjects and groups come from configurable claims; e-mail addresses and names are
 * never used for access decisions.
 */
@Component
public class IdentityMapper {

    private final BibliosServerProperties properties;

    public IdentityMapper(BibliosServerProperties properties) {
        this.properties = properties;
    }

    public PrincipalIdentity from(OidcUser user) {
        if (user == null) {
            return null;
        }
        Map<String, Object> claims = user.getClaims();
        Object subjectValue = claims.get(properties.getSubjectClaim());
        if (!(subjectValue instanceof String subject) || subject.isBlank()) {
            return null;
        }
        Set<SubjectRef> groups = new LinkedHashSet<>();
        Object groupsValue = claims.get(properties.getGroupsClaim());
        if (groupsValue instanceof Iterable<?> iterable) {
            for (Object group : iterable) {
                if (group != null && !group.toString().isBlank()) {
                    groups.add(new SubjectRef(properties.getProvider(), group.toString()));
                }
            }
        } else if (groupsValue instanceof String text && !text.isBlank()) {
            groups.add(new SubjectRef(properties.getProvider(), text));
        }
        return PrincipalIdentity.of(properties.getProvider(), subject, user.getFullName(), groups);
    }
}
