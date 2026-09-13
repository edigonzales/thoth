package guru.interlis.thoth.biblios.access;

import java.util.Objects;

/**
 * Evaluates access rules against a normalized principal.
 *
 * <p>The evaluator is intentionally side-effect free and cheap: rules can be
 * re-applied on every request, while the principal (and therefore the group
 * memberships captured at login) only changes on re-authentication.</p>
 *
 * <p>Unknown policy names fail closed: a request for a policy that does not exist
 * is denied. Configuration validation happens at build/server start; see
 * {@link AccessPolicyResolver}.</p>
 */
public final class AccessPolicyEvaluator {
    private final AccessRules rules;

    public AccessPolicyEvaluator(AccessRules rules) {
        this.rules = Objects.requireNonNull(rules, "access rules are required");
    }

    public AccessRules rules() {
        return rules;
    }

    /**
     * Whether the given policy (or the default behavior for a blank policy name)
     * grants anonymous access.
     */
    public boolean isPublic(String policyName) {
        if (policyName == null || policyName.isBlank()) {
            return rules.isDefaultPublic();
        }
        AccessPolicy policy = rules.policy(policyName);
        return policy != null && policy.mode() == AccessMode.PUBLIC;
    }

    /**
     * Decide whether the principal may read the documentation that uses the given policy.
     *
     * @param policyName value of {@code access_policy} on the source; blank for sources
     *                   without an explicit policy
     * @param principal  authenticated principal, or {@code null} for anonymous visitors
     */
    public boolean canAccess(String policyName, PrincipalIdentity principal) {
        if (policyName == null || policyName.isBlank()) {
            return rules.isDefaultPublic();
        }
        AccessPolicy policy = rules.policy(policyName);
        if (policy == null) {
            return false;
        }
        return canAccessPolicy(policy, principal);
    }

    /**
     * Decide whether the principal may read a documentation that uses the given policy.
     */
    public boolean canAccessPolicy(AccessPolicy policy, PrincipalIdentity principal) {
        Objects.requireNonNull(policy, "policy is required");
        return switch (policy.mode()) {
            case PUBLIC -> true;
            case AUTHENTICATED -> principal != null;
            case RESTRICTED -> principal != null && matchesRestricted(policy, principal);
        };
    }

    private boolean matchesRestricted(AccessPolicy policy, PrincipalIdentity principal) {
        SubjectRef principalUser = new SubjectRef(principal.provider(), principal.subject());
        if (policy.users().contains(principalUser)) {
            return true;
        }
        if (principal.groups().isEmpty() || policy.groups().isEmpty()) {
            return false;
        }
        for (SubjectRef group : principal.groups()) {
            if (policy.groups().contains(group)) {
                return true;
            }
        }
        return false;
    }
}
