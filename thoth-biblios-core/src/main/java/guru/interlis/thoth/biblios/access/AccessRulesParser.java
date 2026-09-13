package guru.interlis.thoth.biblios.access;

import guru.interlis.thoth.core.ThothBuildException;
import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Parses {@code access.yml} into {@link AccessRules}.
 *
 * <pre>
 * default: deny            # deny (default) or public
 * policies:
 *   public:                # referenced from biblios.yml as access_policy
 *     mode: public
 *   agi-betrieb:
 *     mode: restricted
 *     allow:
 *       groups:
 *         - provider: entra-kanton
 *           id: "&lt;group object id&gt;"
 *       users:
 *         - provider: entra-kanton
 *           id: "&lt;user object id&gt;"
 * </pre>
 *
 * <p>Parsing is strict: unknown keys, modes or malformed entries are configuration
 * errors. Restrictive defaults are applied when the file is present but omits
 * {@code default}.</p>
 */
public final class AccessRulesParser {
    private static final Set<String> ROOT_KEYS = Set.of("default", "policies");
    private static final Set<String> POLICY_KEYS = Set.of("mode", "allow");
    private static final Set<String> ALLOW_KEYS = Set.of("groups", "users");
    private static final Set<String> SUBJECT_KEYS = Set.of("provider", "id");

    private final Load yaml;

    public AccessRulesParser() {
        this.yaml = new Load(LoadSettings.builder().build());
    }

    /**
     * Parse an {@code access.yml} file. The file must exist and be a regular file.
     *
     * @throws ThothBuildException if the file is missing or invalid
     */
    public AccessRules parse(Path accessConfigPath) {
        if (accessConfigPath == null) {
            throw new ThothBuildException(
                "Access configuration path must not be null",
                ThothBuildException.ErrorSeverity.FATAL,
                "access"
            );
        }
        if (!Files.exists(accessConfigPath)) {
            throw new ThothBuildException(
                "Access configuration not found: " + accessConfigPath + "\n"
                    + "Create access.yml next to biblios.yml or pass --access-config.",
                ThothBuildException.ErrorSeverity.FATAL,
                "access"
            );
        }
        if (!Files.isRegularFile(accessConfigPath)) {
            throw new ThothBuildException(
                "Access configuration is not a regular file: " + accessConfigPath,
                ThothBuildException.ErrorSeverity.FATAL,
                "access"
            );
        }
        try (Reader reader = Files.newBufferedReader(accessConfigPath)) {
            return parseReader(reader, accessConfigPath.toString());
        } catch (IOException e) {
            throw new ThothBuildException(
                "Failed to read access configuration: " + accessConfigPath + " (" + e.getMessage() + ")",
                ThothBuildException.ErrorSeverity.FATAL,
                "access"
            );
        }
    }

    /**
     * Parse access rules if the file exists; empty otherwise. Used for the default
     * {@code access.yml} location next to {@code biblios.yml}.
     */
    public Optional<AccessRules> parseOptional(Path accessConfigPath) {
        if (accessConfigPath == null || !Files.exists(accessConfigPath)) {
            return Optional.empty();
        }
        return Optional.of(parse(accessConfigPath));
    }

    /**
     * Parse access rules from a YAML string. Intended for tests and embedded usage.
     */
    public AccessRules parseString(String yamlContent) {
        return parseReader(new StringReader(yamlContent), "<inline access rules>");
    }

    private AccessRules parseReader(Reader reader, String source) {
        Object loaded;
        try {
            loaded = yaml.loadFromReader(reader);
        } catch (Exception e) {
            throw new ThothBuildException(
                "Failed to parse access configuration " + source + ": " + e.getMessage(),
                ThothBuildException.ErrorSeverity.FATAL,
                "access"
            );
        }
        if (loaded == null) {
            throw error(source + ": access configuration is empty; define 'default' and/or 'policies' or remove the file");
        }
        if (!(loaded instanceof Map<?, ?> rawRoot)) {
            throw error(source + ": access configuration must be a YAML mapping");
        }
        Map<String, Object> root = castMap(rawRoot, source);
        rejectUnknownKeys(root, ROOT_KEYS, source);

        AccessDefault defaultAccess = AccessDefault.DENY;
        if (root.containsKey("default")) {
            Object value = root.get("default");
            if (!(value instanceof String text)) {
                throw error(source + ".default must be a string (deny or public)");
            }
            try {
                defaultAccess = AccessDefault.parse(text);
            } catch (IllegalArgumentException e) {
                throw error(source + ": " + e.getMessage());
            }
        }

        Map<String, AccessPolicy> policies = new LinkedHashMap<>();
        if (root.containsKey("policies")) {
            Object policiesValue = root.get("policies");
            if (!(policiesValue instanceof Map<?, ?> rawPolicies)) {
                throw error(source + ".policies must be a mapping of policy names");
            }
            for (Map.Entry<?, ?> entry : rawPolicies.entrySet()) {
                if (!(entry.getKey() instanceof String policyName) || policyName.isBlank()) {
                    throw error(source + ".policies keys must be non-blank strings");
                }
                String label = source + ".policies." + policyName;
                if (!(entry.getValue() instanceof Map<?, ?> rawPolicy)) {
                    throw error(label + " must be a mapping");
                }
                policies.put(policyName.trim(), parsePolicy(policyName.trim(), castMap(rawPolicy, label), label));
            }
        }
        return new AccessRules(defaultAccess, policies);
    }

    private AccessPolicy parsePolicy(String name, Map<String, Object> policyMap, String label) {
        rejectUnknownKeys(policyMap, POLICY_KEYS, label);
        Object modeValue = policyMap.get("mode");
        if (!(modeValue instanceof String modeText)) {
            throw error(label + ".mode is required and must be a string");
        }
        AccessMode mode;
        try {
            mode = AccessMode.parse(modeText);
        } catch (IllegalArgumentException e) {
            throw error(label + ": " + e.getMessage());
        }

        Set<SubjectRef> groups = Set.of();
        Set<SubjectRef> users = Set.of();
        if (policyMap.containsKey("allow")) {
            if (mode != AccessMode.RESTRICTED) {
                throw error(label + ".allow is only valid for mode 'restricted'");
            }
            Object allowValue = policyMap.get("allow");
            if (!(allowValue instanceof Map<?, ?> rawAllow)) {
                throw error(label + ".allow must be a mapping with groups and/or users");
            }
            Map<String, Object> allow = castMap(rawAllow, label + ".allow");
            rejectUnknownKeys(allow, ALLOW_KEYS, label + ".allow");
            groups = parseSubjects(allow, "groups", label + ".allow");
            users = parseSubjects(allow, "users", label + ".allow");
        }

        try {
            return switch (mode) {
                case PUBLIC -> AccessPolicy.publicPolicy(name);
                case AUTHENTICATED -> AccessPolicy.authenticatedPolicy(name);
                case RESTRICTED -> AccessPolicy.restrictedPolicy(name, groups, users);
            };
        } catch (IllegalArgumentException e) {
            throw error(label + ": " + e.getMessage());
        }
    }

    private Set<SubjectRef> parseSubjects(Map<String, Object> allow, String key, String label) {
        if (!allow.containsKey(key)) {
            return Set.of();
        }
        Object value = allow.get(key);
        if (!(value instanceof List<?> entries)) {
            throw error(label + "." + key + " must be a list");
        }
        Set<SubjectRef> subjects = new java.util.LinkedHashSet<>();
        for (int i = 0; i < entries.size(); i++) {
            Object item = entries.get(i);
            String itemLabel = label + "." + key + "[" + i + "]";
            if (!(item instanceof Map<?, ?> rawItem)) {
                throw error(itemLabel + " must be a mapping with provider and id");
            }
            Map<String, Object> subjectMap = castMap(rawItem, itemLabel);
            rejectUnknownKeys(subjectMap, SUBJECT_KEYS, itemLabel);
            try {
                subjects.add(SubjectRef.parse(subjectMap.get("provider"), subjectMap.get("id"), itemLabel));
            } catch (IllegalArgumentException e) {
                throw error(itemLabel + ": " + e.getMessage());
            }
        }
        return subjects;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Map<?, ?> map, String label) {
        for (Object key : map.keySet()) {
            if (!(key instanceof String)) {
                throw error(label + " contains a non-string key: " + key);
            }
        }
        return (Map<String, Object>) map;
    }

    private void rejectUnknownKeys(Map<String, Object> map, Set<String> allowed, String label) {
        List<String> unknown = new ArrayList<>();
        for (String key : map.keySet()) {
            if (!allowed.contains(key)) {
                unknown.add(key);
            }
        }
        if (!unknown.isEmpty()) {
            throw error(label + " contains unknown key(s): " + String.join(", ", unknown)
                + ". Allowed: " + String.join(", ", allowed));
        }
    }

    private ThothBuildException error(String message) {
        return new ThothBuildException(message, ThothBuildException.ErrorSeverity.FATAL, "access");
    }
}
