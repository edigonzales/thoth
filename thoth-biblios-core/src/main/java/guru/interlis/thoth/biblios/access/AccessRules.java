package guru.interlis.thoth.biblios.access;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable access rules loaded from {@code access.yml}.
 */
public final class AccessRules {
    private final AccessDefault defaultAccess;
    private final Map<String, AccessPolicy> policies;

    public AccessRules(AccessDefault defaultAccess, Map<String, AccessPolicy> policies) {
        this.defaultAccess = Objects.requireNonNull(defaultAccess, "access default is required");
        Objects.requireNonNull(policies, "policies are required");
        this.policies = Map.copyOf(new LinkedHashMap<>(policies));
    }

    /**
     * Rules used when no {@code access.yml} exists: sources without an explicit
     * {@code access_policy} are public, and any referenced policy name is a
     * configuration error (validation in {@link AccessPolicyResolver}).
     */
    public static AccessRules defaultPublic() {
        return new AccessRules(AccessDefault.PUBLIC, Map.of());
    }

    /**
     * Rules with default {@code deny} and no policies. Fail-closed baseline.
     */
    public static AccessRules denyAll() {
        return new AccessRules(AccessDefault.DENY, Map.of());
    }

    public AccessDefault defaultAccess() {
        return defaultAccess;
    }

    public boolean isDefaultPublic() {
        return defaultAccess == AccessDefault.PUBLIC;
    }

    public Map<String, AccessPolicy> policies() {
        return policies;
    }

    /**
     * Find a policy by name, or {@code null} when unknown.
     */
    public AccessPolicy policy(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        return policies.get(name.trim());
    }

    @Override
    public String toString() {
        return "AccessRules{default=" + defaultAccess.configValue() + ", policies=" + policies.size() + "}";
    }
}
