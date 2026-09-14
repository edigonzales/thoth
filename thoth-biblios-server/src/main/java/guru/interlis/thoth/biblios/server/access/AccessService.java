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

/** Loads a complete rules snapshot on each decision, denying all on invalid or unavailable rules. */
@Component
public class AccessService {
    private static final Logger log = LoggerFactory.getLogger(AccessService.class);

    private final PublicationPackage publicationPackage;
    private final Path accessConfigPath;
    private final Set<String> requiredPolicies = new LinkedHashSet<>();
    private final AccessRulesParser parser = new AccessRulesParser();

    private enum Status { VALID, INVALID, UNAVAILABLE }

    private record Snapshot(AccessPolicyEvaluator evaluator, String content, Status status) { }

    // Accessed only while reloadRules holds this service's monitor.
    private Snapshot snapshot;

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
        AccessPolicyEvaluator evaluator = reloadRules(false).evaluator();
        if (!publicationPackage.hasSource(sourceId)) {
            return false;
        }
        return evaluator.canAccess(publicationPackage.accessPolicyFor(sourceId), principal);
    }

    public boolean isSourcePublic(String sourceId) {
        AccessPolicyEvaluator evaluator = reloadRules(false).evaluator();
        if (!publicationPackage.hasSource(sourceId)) {
            return false;
        }
        return evaluator.isPublic(publicationPackage.accessPolicyFor(sourceId));
    }

    public Set<String> allowedSourceIds(PrincipalIdentity principal) {
        AccessPolicyEvaluator evaluator = reloadRules(false).evaluator();
        Set<String> allowed = new LinkedHashSet<>();
        for (PackageCatalog.SourceEntry source : publicationPackage.catalog().sources()) {
            if (evaluator.canAccess(source.accessPolicy(), principal)) {
                allowed.add(source.id());
            }
        }
        return allowed;
    }

    private synchronized Snapshot reloadRules(boolean initial) {
        final String content;
        try {
            content = Files.readString(accessConfigPath);
        } catch (IOException | SecurityException e) {
            if (initial) {
                throw new IllegalStateException("Access configuration must exist and be readable: "
                    + accessConfigPath, e);
            }
            if (snapshot.status() != Status.UNAVAILABLE) {
                log.error("Access configuration unavailable; denying all documentation: {}", accessConfigPath, e);
            }
            snapshot = denied(null, Status.UNAVAILABLE);
            return snapshot;
        }
        if (snapshot != null && snapshot.status() != Status.UNAVAILABLE && content.equals(snapshot.content())) {
            return snapshot;
        }
        try {
            AccessRules rules = parser.parseString(content);
            for (String required : requiredPolicies) {
                if (rules.policy(required) == null) {
                    throw new IllegalStateException(
                        "access.yml does not define policy '" + required + "' referenced by the catalog");
                }
            }
            Snapshot previous = snapshot;
            snapshot = new Snapshot(new AccessPolicyEvaluator(rules), content, Status.VALID);
            if (previous != null && previous.status() != Status.VALID) {
                log.info("Access configuration restored; documentation policies active again: {}", accessConfigPath);
            } else if (!initial) {
                log.info("Reloaded access configuration from {}", accessConfigPath);
            }
        } catch (RuntimeException e) {
            if (initial) {
                throw new IllegalStateException(
                    "Invalid access configuration " + accessConfigPath + ": " + e.getMessage(), e);
            }
            snapshot = denied(content, Status.INVALID);
            log.error("Invalid access configuration; denying all documentation: {}: {}", accessConfigPath, e.getMessage());
        }
        return snapshot;
    }

    private static Snapshot denied(String content, Status status) {
        return new Snapshot(new AccessPolicyEvaluator(AccessRules.denyAll()), content, status);
    }
}
