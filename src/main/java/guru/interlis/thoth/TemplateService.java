package guru.interlis.thoth;

import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public final class TemplateService {
    private final Configuration configuration;

    public TemplateService() {
        configuration = new Configuration(Configuration.VERSION_2_3_34);
        configuration.setTemplateLoader(new ClassTemplateLoader(getClass(), "/templates"));
        configuration.setDefaultEncoding(StandardCharsets.UTF_8.name());
        configuration.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        configuration.setLogTemplateExceptions(false);
        configuration.setWrapUncheckedExceptions(true);
    }

    public String render(String templateName, Map<String, Object> model) {
        try (Writer writer = new StringWriter()) {
            Template template = configuration.getTemplate(templateName);
            template.process(model, writer);
            return writer.toString();
        } catch (IOException | TemplateException ex) {
            throw new IllegalStateException("Failed to render template " + templateName, ex);
        }
    }

    public void renderToFile(String templateName, Map<String, Object> model, Path outputFile) throws IOException {
        String content = render(templateName, model);
        Files.createDirectories(outputFile.getParent());
        Files.writeString(outputFile, content, StandardCharsets.UTF_8);
    }
}
