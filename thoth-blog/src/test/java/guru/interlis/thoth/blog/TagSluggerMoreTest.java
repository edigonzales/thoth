package guru.interlis.thoth.blog;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Additional TagSlugger tests covering edge cases and boundary conditions.
 */
class TagSluggerMoreTest {

    @Test
    void returnsDefaultForNull() {
        assertEquals("tag", TagSlugger.slugify(null));
    }

    @Test
    void returnsDefaultForEmptyString() {
        assertEquals("tag", TagSlugger.slugify(""));
    }

    @Test
    void returnsDefaultForWhitespaceOnly() {
        assertEquals("tag", TagSlugger.slugify("   "));
    }

    @Test
    void trimsLeadingAndTrailingSpaces() {
        assertEquals("java", TagSlugger.slugify("  Java  "));
    }

    @Test
    void handlesSingleWord() {
        assertEquals("java", TagSlugger.slugify("Java"));
    }

    @Test
    void handlesMultipleSpacesBetweenWords() {
        assertEquals("java-ai", TagSlugger.slugify("Java   AI"));
    }

    @Test
    void removesSpecialCharacters() {
        assertEquals("hello-world", TagSlugger.slugify("Hello! World?"));
    }

    @Test
    void handlesNumbers() {
        assertEquals("java-8-features", TagSlugger.slugify("Java 8 Features"));
    }

    @Test
    void handlesAllUmlauts() {
        // Uppercase umlauts get replaced first, then lowercase ones in combined form
        // "Ä Ö Ü äöü" -> "Ae Oe Ue aeoeue" -> "ae oe ue aeoeue" -> "ae-oe-ue-aeoeue"
        assertEquals("ae-oe-ue-aeoeue", TagSlugger.slugify("Ä Ö Ü äöü"));
    }

    @Test
    void handlesEszett() {
        assertEquals("strasse", TagSlugger.slugify("Straße"));
    }

    @Test
    void removesSpecialCharactersBetweenLetters() {
        // Special chars are removed (not turned into dashes); only spaces/commas become dashes
        assertEquals("ab", TagSlugger.slugify("a!!!@#$%^&*()b"));
    }

    @Test
    void removesLeadingAndTrailingDashes() {
        assertEquals("hello", TagSlugger.slugify("!!!hello!!!"));
    }

    @Test
    void handlesCommasAsSeparators() {
        assertEquals("java-ai-machine-learning", TagSlugger.slugify("Java, AI, Machine Learning"));
    }

    @Test
    void convertsToLowercase() {
        assertEquals("typescript", TagSlugger.slugify("TypeScript"));
    }

    @Test
    void handlesMixedCaseWithUmlauts() {
        assertEquals("zuerich", TagSlugger.slugify("ZÜRICH"));
    }

    @Test
    void handlesAccentedCharacters() {
        assertEquals("cafe", TagSlugger.slugify("Café"));
    }
}
