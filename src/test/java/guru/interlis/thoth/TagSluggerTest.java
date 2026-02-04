package guru.interlis.thoth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TagSluggerTest {
    @Test
    public void slugifiesBasicText() {
        assertEquals("java-ai", TagSlugger.slugify("Java AI"));
        assertEquals("interlis-goes-ai", TagSlugger.slugify("INTERLIS goes AI"));
    }

    @Test
    public void normalizesUmlautsAndSpecialCharacters() {
        assertEquals("gruesse-aus-zuerich", TagSlugger.slugify("Grüsse aus Zürich"));
        assertEquals("mcp-agent", TagSlugger.slugify("MCP & Agent"));
    }
}
