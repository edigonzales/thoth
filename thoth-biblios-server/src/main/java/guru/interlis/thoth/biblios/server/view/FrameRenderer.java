package guru.interlis.thoth.biblios.server.view;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

/**
 * Renders server-side frames from FreeMarker templates.
 */
@Component
public class FrameRenderer {
    private final Configuration configuration;

    public FrameRenderer(Configuration configuration) {
        this.configuration = configuration;
    }

    public String render(String templateName, Map<String, Object> model) {
        try {
            Template template = configuration.getTemplate(templateName);
            try (StringWriter writer = new StringWriter()) {
                template.process(model, writer);
                return writer.toString();
            }
        } catch (IOException | TemplateException e) {
            throw new IllegalStateException("Failed to render template: " + templateName, e);
        }
    }
}
