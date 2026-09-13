package guru.interlis.thoth.biblios.server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;
import java.time.Duration;

/**
 * Server configuration.
 */
@ConfigurationProperties(prefix = "biblios")
public class BibliosServerProperties {

    /** Directory of the publication package produced by {@code thoth-biblios build --package}. */
    private Path packageDir = Path.of("build/package");

    /** Access rules file ({@code access.yml}) owned by the portal operator. */
    private Path accessConfig = Path.of("access.yml");

    /** Maximum age of the captured identity (groups) before re-authentication is required. */
    private Duration maxIdentityAge = Duration.ofMinutes(60);

    /** Name used in access policies for principals of the configured provider. */
    private String provider = "keycloak-local";

    /** Claim that carries the stable subject (Entra ID: {@code oid}; OIDC: {@code sub}). */
    private String subjectClaim = "sub";

    /** Claim that carries group memberships. */
    private String groupsClaim = "groups";

    /** Spring Security registration id used for login redirects. */
    private String registrationId = "keycloak";

    public Path getPackageDir() {
        return packageDir;
    }

    public void setPackageDir(Path packageDir) {
        this.packageDir = packageDir;
    }

    public Path getAccessConfig() {
        return accessConfig;
    }

    public void setAccessConfig(Path accessConfig) {
        this.accessConfig = accessConfig;
    }

    public Duration getMaxIdentityAge() {
        return maxIdentityAge;
    }

    public void setMaxIdentityAge(Duration maxIdentityAge) {
        this.maxIdentityAge = maxIdentityAge;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getSubjectClaim() {
        return subjectClaim;
    }

    public void setSubjectClaim(String subjectClaim) {
        this.subjectClaim = subjectClaim;
    }

    public String getGroupsClaim() {
        return groupsClaim;
    }

    public void setGroupsClaim(String groupsClaim) {
        this.groupsClaim = groupsClaim;
    }

    public String getRegistrationId() {
        return registrationId;
    }

    public void setRegistrationId(String registrationId) {
        this.registrationId = registrationId;
    }
}
