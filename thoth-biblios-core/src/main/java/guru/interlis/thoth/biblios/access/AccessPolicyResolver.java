package guru.interlis.thoth.biblios.access;

import guru.interlis.thoth.biblios.config.BibliosConfig;
import guru.interlis.thoth.biblios.config.ContentGroup;
import guru.interlis.thoth.biblios.config.ContentSection;
import guru.interlis.thoth.biblios.config.SourceConfig;
import guru.interlis.thoth.core.ThothBuildException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Resolves and validates the access configuration of documentation sources.
 *
 * <p>Configuration errors are reported as {@link ThothBuildException} with severity
 * {@code FATAL}: an unknown policy name in {@code biblios.yml} must never be
 * interpreted as "public".</p>
 */
public final class AccessPolicyResolver {
    private final AccessRules rules;
    private final AccessPolicyEvaluator evaluator;

    public AccessPolicyResolver(AccessRules rules) {
        this.rules = Objects.requireNonNull(rules, "access rules are required");
        this.evaluator = new AccessPolicyEvaluator(rules);
    }

    public AccessRules rules() {
        return rules;
    }

    public AccessPolicyEvaluator evaluator() {
        return evaluator;
    }

    /**
     * Validate that every named policy referenced by a source exists.
     *
     * @throws ThothBuildException if a source references an unknown policy
     */
    public void validate(List<SourceConfig> sources) {
        List<String> unknown = new ArrayList<>();
        for (SourceConfig source : sources) {
            String policyName = source.accessPolicy();
            if (policyName != null && !policyName.isBlank() && rules.policy(policyName) == null) {
                unknown.add(source.id() + " -> access_policy: " + policyName);
            }
        }
        if (!unknown.isEmpty()) {
            throw new ThothBuildException(
                "Unknown access policy reference(s) in biblios.yml:\n  " + String.join("\n  ", unknown)
                    + "\nDefine the polic(y|ies) in access.yml (or fix the spelling).",
                ThothBuildException.ErrorSeverity.FATAL,
                "access"
            );
        }
    }

    /**
     * Whether at least one source is not publicly readable.
     */
    public boolean hasNonPublicSources(List<SourceConfig> sources) {
        for (SourceConfig source : sources) {
            if (!evaluator.isPublic(source.accessPolicy())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Identifiers of all non-public sources, for diagnostics.
     */
    public List<String> nonPublicSourceIds(List<SourceConfig> sources) {
        List<String> ids = new ArrayList<>();
        for (SourceConfig source : sources) {
            if (!evaluator.isPublic(source.accessPolicy())) {
                ids.add(source.id());
            }
        }
        return ids;
    }

    /**
     * Build a configuration that only contains publicly readable sources.
     *
     * <p>Documentation groups are filtered accordingly; empty groups are removed so
     * that the generated site does not reference excluded documentation.</p>
     *
     * @throws ThothBuildException if no public source remains
     */
    public BibliosConfig publicOnly(BibliosConfig config) {
        Objects.requireNonNull(config, "config is required");
        List<SourceConfig> publicSources = new ArrayList<>();
        for (SourceConfig source : config.content().sources()) {
            if (evaluator.isPublic(source.accessPolicy())) {
                publicSources.add(source);
            }
        }
        if (publicSources.isEmpty()) {
            throw new ThothBuildException(
                "Public export would contain no sources. All documentation is protected by access policies.",
                ThothBuildException.ErrorSeverity.FATAL,
                "access"
            );
        }

        Set<String> publicIds = new HashSet<>();
        for (SourceConfig source : publicSources) {
            publicIds.add(source.id());
        }
        List<ContentGroup> groups = new ArrayList<>();
        for (ContentGroup group : config.content().groups()) {
            List<String> groupSources = new ArrayList<>();
            for (String sourceId : group.sources()) {
                if (publicIds.contains(sourceId)) {
                    groupSources.add(sourceId);
                }
            }
            if (!groupSources.isEmpty()) {
                groups.add(new ContentGroup(group.title(), groupSources));
            }
        }

        ContentSection content = new ContentSection(publicSources, groups);
        return new BibliosConfig(
            config.site(),
            config.output(),
            config.ui(),
            config.pdf(),
            config.docx(),
            content
        );
    }
}
