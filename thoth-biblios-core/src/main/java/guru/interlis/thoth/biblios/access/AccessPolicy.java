package guru.interlis.thoth.biblios.access;

import java.util.Objects;
import java.util.Set;

/**
 * A named access policy as defined in {@code access.yml}.
 *
 * <p>The policy name is referenced by {@code access_policy} on a documentation
 * source in {@code biblios.yml}. The policy itself is owned by the portal operator
 * and should never live inside content repositories.</p>
 */
public final class AccessPolicy {
    private final String name;
    private final AccessMode mode;
    private final Set<SubjectRef> groups;
    private final Set<SubjectRef> users;

    private AccessPolicy(String name, AccessMode mode, Set<SubjectRef> groups, Set<SubjectRef> users) {
        this.name = requireName(name);
        this.mode = Objects.requireNonNull(mode, "access mode is required");
        this.groups = Set.copyOf(groups != null ? groups : Set.of());
        this.users = Set.copyOf(users != null ? users : Set.of());
        if (mode != AccessMode.RESTRICTED && (!this.groups.isEmpty() || !this.users.isEmpty())) {
            throw new IllegalArgumentException(
                "Policy '" + name + "' uses mode '" + mode.configValue()
                    + "' and must not declare allow entries; use mode 'restricted' instead.");
        }
    }

    public static AccessPolicy publicPolicy(String name) {
        return new AccessPolicy(name, AccessMode.PUBLIC, Set.of(), Set.of());
    }

    public static AccessPolicy authenticatedPolicy(String name) {
        return new AccessPolicy(name, AccessMode.AUTHENTICATED, Set.of(), Set.of());
    }

    public static AccessPolicy restrictedPolicy(String name, Set<SubjectRef> groups, Set<SubjectRef> users) {
        return new AccessPolicy(name, AccessMode.RESTRICTED, groups, users);
    }

    public String name() {
        return name;
    }

    public AccessMode mode() {
        return mode;
    }

    public Set<SubjectRef> groups() {
        return groups;
    }

    public Set<SubjectRef> users() {
        return users;
    }

    private static String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("policy name must not be blank");
        }
        return name.trim();
    }

    @Override
    public String toString() {
        return "AccessPolicy{name='" + name + "', mode=" + mode
            + ", groups=" + groups.size() + ", users=" + users.size() + "}";
    }
}
