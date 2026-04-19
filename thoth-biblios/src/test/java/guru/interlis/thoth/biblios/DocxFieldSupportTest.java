package guru.interlis.thoth.biblios;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DocxFieldSupportTest {

    @Test
    void buildsFieldInstructions() {
        assertEquals(" REF b_target \\h ", DocxFieldSupport.refInstruction("b_target"));
        assertEquals(" PAGEREF b_target \\h ", DocxFieldSupport.pageRefInstruction("b_target"));
        assertEquals(" SEQ Figure \\* ARABIC ", DocxFieldSupport.seqInstruction("Figure"));
    }

    @Test
    void bookmarkRegistryNormalizesAndDeduplicatesDeterministically() {
        DocxFieldSupport.BookmarkRegistry registry = new DocxFieldSupport.BookmarkRegistry();

        String first = registry.nameFor("foo-bar");
        String second = registry.nameFor("foo bar");
        String firstAgain = registry.nameFor("foo-bar");

        assertEquals("foo_bar", first);
        assertEquals("foo_bar_2", second);
        assertEquals(first, firstAgain);
        assertNotEquals(first, second);
    }

    @Test
    void bookmarkRegistryRejectsBlankKeys() {
        DocxFieldSupport.BookmarkRegistry registry = new DocxFieldSupport.BookmarkRegistry();
        assertThrows(IllegalArgumentException.class, () -> registry.nameFor("   "));
    }
}
