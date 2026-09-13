package guru.interlis.thoth.biblios.access;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AccessPolicyEvaluatorTest {

    private static final SubjectRef KANTON_GROUP = new SubjectRef("entra-kanton", "group-betrieb");
    private static final SubjectRef OTHER_GROUP = new SubjectRef("entra-kanton", "group-andere");

    private final AccessRules rules = new AccessRules(AccessDefault.DENY, java.util.Map.of(
        "public", AccessPolicy.publicPolicy("public"),
        "authenticated", AccessPolicy.authenticatedPolicy("authenticated"),
        "betrieb", AccessPolicy.restrictedPolicy("betrieb",
            Set.of(KANTON_GROUP),
            Set.of(new SubjectRef("entra-kanton", "user-special")))
    ));
    private final AccessPolicyEvaluator evaluator = new AccessPolicyEvaluator(rules);

    private PrincipalIdentity principal(String subject, SubjectRef... groups) {
        return PrincipalIdentity.of("entra-kanton", subject, "Test User", Set.of(groups));
    }

    @Test
    void publicPolicyAllowsAnonymous() {
        assertTrue(evaluator.isPublic("public"));
        assertTrue(evaluator.canAccess("public", null));
    }

    @Test
    void authenticatedPolicyRequiresLogin() {
        assertFalse(evaluator.isPublic("authenticated"));
        assertFalse(evaluator.canAccess("authenticated", null));
        assertTrue(evaluator.canAccess("authenticated", principal("user-1")));
    }

    @Test
    void restrictedPolicyAllowsGroupMember() {
        assertTrue(evaluator.canAccess("betrieb", principal("user-1", KANTON_GROUP)));
    }

    @Test
    void restrictedPolicyDeniesOtherGroup() {
        assertFalse(evaluator.canAccess("betrieb", principal("user-1", OTHER_GROUP)));
    }

    @Test
    void restrictedPolicyDeniesGroupFromOtherProvider() {
        assertFalse(evaluator.canAccess("betrieb",
            principal("user-1", new SubjectRef("keycloak-local", "group-betrieb"))));
    }

    @Test
    void restrictedPolicyAllowsListedUser() {
        assertTrue(evaluator.canAccess("betrieb", principal("user-special")));
    }

    @Test
    void restrictedPolicyDeniesAnonymous() {
        assertFalse(evaluator.canAccess("betrieb", null));
    }

    @Test
    void unknownPolicyFailsClosed() {
        assertFalse(evaluator.canAccess("missing", principal("user-1", KANTON_GROUP)));
        assertFalse(evaluator.isPublic("missing"));
    }

    @Test
    void blankPolicyNameUsesDefaultDeny() {
        assertFalse(evaluator.canAccess(null, principal("user-1", KANTON_GROUP)));
        assertFalse(evaluator.isPublic(""));
    }

    @Test
    void blankPolicyNameWithDefaultPublicIsPublic() {
        AccessPolicyEvaluator publicEvaluator = new AccessPolicyEvaluator(AccessRules.defaultPublic());
        assertTrue(publicEvaluator.canAccess(null, null));
        assertTrue(publicEvaluator.isPublic(null));
    }

    @Test
    void principalProviderIsNormalized() {
        PrincipalIdentity identity = PrincipalIdentity.of("Entra-Kanton", "user-1", "Test", Set.of(KANTON_GROUP));
        assertEquals("entra-kanton", identity.provider());
        assertTrue(evaluator.canAccess("betrieb", identity));
    }
}
