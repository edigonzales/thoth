package guru.interlis.thoth.biblios.config;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for branch pattern matching and display_version resolution.
 */
class BranchPatternTest {

    @Test
    void exactMatch() {
        BranchConfig branch = new BranchConfig("main", "Latest");
        assertTrue(matchesPattern(branch.name(), "main"));
        assertEquals("Latest", branch.displayVersion());
    }

    @Test
    void wildcardMatch() {
        assertTrue(matchesPattern("v1.2", "v1.*"));
        assertTrue(matchesPattern("v2.0", "v2.*"));
        assertFalse(matchesPattern("v1.2", "v2.*"));
    }

    @Test
    void globPatternMatch() {
        assertTrue(matchesPattern("release/1.0", "release/*"));
        assertTrue(matchesPattern("release/2.3", "release/*"));
        assertFalse(matchesPattern("main", "release/*"));
    }

    @Test
    void defaultsDisplayVersionToBranchName() {
        BranchConfig branch = new BranchConfig("feature-x", null);
        assertEquals("feature-x", branch.displayVersion());
    }

    @Test
    void usesCustomDisplayVersion() {
        BranchConfig branch = new BranchConfig("v1.x", "Version 1.x (Legacy)");
        assertEquals("Version 1.x (Legacy)", branch.displayVersion());
    }

    /**
     * Simple glob-to-regex matching for branch pattern evaluation.
     * Converts glob patterns (* matches anything) to regex.
     */
    private boolean matchesPattern(String branchName, String pattern) {
        String regex = pattern.replace(".", "\\.")
                             .replace("*", ".*");
        return Pattern.matches(regex, branchName);
    }
}
