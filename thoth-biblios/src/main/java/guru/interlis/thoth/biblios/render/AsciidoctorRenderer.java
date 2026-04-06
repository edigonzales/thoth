package guru.interlis.thoth.biblios.render;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.AttributesBuilder;
import org.asciidoctor.OptionsBuilder;
import org.asciidoctor.SafeMode;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

/**
 * Renders AsciiDoc content to HTML using AsciidoctorJ.
 * Thread-safe after initialization. Should be closed when done.
 */
public final class AsciidoctorRenderer implements AutoCloseable {

    private final Asciidoctor asciidoctor;

    public AsciidoctorRenderer() {
        this.asciidoctor = Asciidoctor.Factory.create();
    }

    /**
     * Render AsciiDoc content from a file to HTML.
     *
     * @param sourcePath path to the .adoc file
     * @return rendered HTML content
     * @throws IOException if the file cannot be read or rendered
     */
    public String renderFile(Path sourcePath) throws IOException {
        try {
            AttributesBuilder attributes = org.asciidoctor.Attributes.builder();
            attributes.attribute("source-highlighter", "prettify");
            attributes.attribute("sectnums", "");
            attributes.attribute("toc", "left");
            attributes.attribute("icons", "font");

            OptionsBuilder options = org.asciidoctor.Options.builder()
                .backend("html5")
                .safe(SafeMode.UNSAFE)
                .standalone(false)
                .baseDir(sourcePath.getParent().toFile())
                .attributes(attributes.build());

            return asciidoctor.convertFile(sourcePath.toFile(), options.build(), String.class);
        } catch (Exception e) {
            throw new IOException("Failed to render AsciiDoc file: " + sourcePath, e);
        }
    }

    /**
     * Render AsciiDoc content from a string to HTML.
     *
     * @param content AsciiDoc content string
     * @return rendered HTML content
     */
    public String renderString(String content) {
        try {
            AttributesBuilder attributes = org.asciidoctor.Attributes.builder();
            attributes.attribute("source-highlighter", "prettify");

            OptionsBuilder options = org.asciidoctor.Options.builder()
                .backend("html5")
                .safe(SafeMode.UNSAFE)
                .standalone(false)
                .attributes(attributes.build());

            return asciidoctor.convert(content, options.build());
        } catch (Exception e) {
            throw new RuntimeException("Failed to render AsciiDoc content", e);
        }
    }

    /**
     * Extract the document title from AsciiDoc content.
     *
     * @param sourcePath path to the .adoc file
     * @return the document title, or null if not found
     */
    public String extractTitle(Path sourcePath) {
        try {
            OptionsBuilder options = org.asciidoctor.Options.builder()
                .safe(SafeMode.UNSAFE)
                .standalone(false);

            org.asciidoctor.ast.Document doc = asciidoctor.loadFile(
                sourcePath.toFile(),
                options.build()
            );
            return doc.getDoctitle();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void close() {
        try {
            asciidoctor.close();
        } catch (Exception ignored) {
        }
    }
}
