package guru.interlis.thoth.biblios.server.access;

import guru.interlis.thoth.biblios.access.AccessPolicyEvaluator;
import guru.interlis.thoth.biblios.access.AccessRules;
import guru.interlis.thoth.biblios.access.AccessRulesParser;
import guru.interlis.thoth.biblios.access.PrincipalIdentity;
import guru.interlis.thoth.biblios.server.config.BibliosServerProperties;
import guru.interlis.thoth.biblios.server.publication.PackageCatalog;
import guru.interlis.thoth.biblios.server.publication.PublicationPackage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Evaluates the access rules against the current principal.
 *
 * <p>The rules file is re-read when its modification time changes, so grants can
 * be changed without a rebuild or restart. Invalid updates are ignored and the
 * last valid rules stay active (fail closed). Configuration errors at startup
 * abort the server: a policy referenced by the catalog but missing in
 * {@code access.yml} must never silently become public.</p>
 */
@Component
public class AccessService {
    private static final Logger log = LoggerFactory.getLogger(AccessService.class);

    private final PublicationPackage publicationPackage;
    private final Path accessConfigPath;
    private final Set<String> requiredPolicies = new LinkedHashSet<>();
    private final AccessRulesParser parser = new AccessRulesParser();

    private volatile AccessPolicyEvaluator evaluator;
    private volatile long lastModified = Long.MIN_VALUE;

    public AccessService(PublicationPackage publicationPackage, BibliosServerProperties properties) {
        this.publicationPackage = publicationPackage;
        this.accessConfigPath = properties.getAccessConfig().toAbsolutePath().normalize();
        for (PackageCatalog.SourceEntry source : publicationPackage.catalog().sources()) {
            String policyName = source.accessPolicy();
            if (policyName != null && !policyName.isBlank()) {
                requiredPolicies.add(policyName);
            }
        }
        reloadRules(true);
    }

    public boolean canAccessSource(String sourceId, PrincipalIdentity principal) {
        ensureFresh();
        if (!publicationPackage.hasSource(sourceId)) {
            return false;
        }
        return evaluator.canAccess(publicationPackage.accessPolicyFor(sourceId), principal);
    }

    public boolean isSourcePublic(String sourceId) {
        ensureFresh();
        if (!publicationPackage.hasSource(sourceId)) {
            return false;
        }
        return evaluator.isPublic(publicationPackage.accessPolicyFor(sourceId));
    }

    public Set<String> allowedSourceIds(PrincipalIdentity principal) {
        ensureFresh();
        Set<String> allowed = new LinkedHashSet<>();
        for (PackageCatalog.SourceEntry source : publicationPackage.catalog().sources()) {
            if (evaluator.canAccess(source.accessPolicy(), principal)) {
                allowed.add(source.id());
            }
        }
        return allowed;
    }

    private void ensureFresh() {
        reloadRules(false);
    }

    private synchronized void reloadRules(boolean initial) {
        final long modified;
        try {
            modified = Files.exists(accessConfigPath)
                ? Files.getLastModifiedTime(accessConfigPath).toMillis()
                : -1L;
        } catch (IOException e) {
            log.warn("Could not check access configuration {}: {}", accessConfigPath, e.getMessage());
            return;
        }
        if (modified == lastModified && evaluator != null) {
            return;
        }
        try {
            AccessRules rules = parser.parseOptional(accessConfigPath).orElseGet(AccessRules::defaultPublic);
            for (String required : requiredPolicies) {
                if (rules.policy(required) == null) {
                    throw new IllegalStateException(
                        "access.yml does not define policy '" + required + "' referenced by the catalog");
                }
            }
            this.evaluator = new AccessPolicyEvaluator(rules);
            this.lastModified = modified;
            if (!initial) {
                log.info("Reloaded access configuration from {}", accessConfigPath);
            }
        } catch (RuntimeException e) {
            // Keep the last valid rules and do not retry until the file changes again.
            this.lastModified = modified;
            if (initial) {
                throw e instanceof IllegalStateException illegal
                    ? illegal
                    : new IllegalStateException(
                        "Invalid access configuration " + accessConfigPath + ": " + e.getMessage(), e);
            }
            log.warn("Ignoring invalid access configuration {}: {}", accessConfigPath, e.getMessage());
        }
    }
}
