package guru.interlis.thoth.blog;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.FileTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
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
import java.util.HashMap;
import java.util.List;

public final class TemplateService {
    private final Configuration configuration;

    public TemplateService() {
        this(null);
    }

    public TemplateService(Path templateOverrideRoot) {
        configuration = new Configuration(Configuration.VERSION_2_3_34);
        configuration.setTemplateLoader(createTemplateLoader(templateOverrideRoot));
        configuration.setDefaultEncoding(StandardCharsets.UTF_8.name());
        configuration.setTemplateUpdateDelayMilliseconds(0);
        configuration.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        configuration.setLogTemplateExceptions(false);
        configuration.setWrapUncheckedExceptions(true);
    }

    private TemplateLoader createTemplateLoader(Path templateOverrideRoot) {
        TemplateLoader bundled = new ClassTemplateLoader(getClass(), "/templates");
        if (templateOverrideRoot == null || !Files.isDirectory(templateOverrideRoot)) {
            return bundled;
        }

        try {
            TemplateLoader overrides = new FileTemplateLoader(templateOverrideRoot.toFile());
            return new MultiTemplateLoader(new TemplateLoader[] {overrides, bundled});
        } catch (IOException ex) {
            throw new IllegalStateException(
                "Failed to initialize template override directory " + templateOverrideRoot,
                ex
            );
        }
    }

    public String render(String templateName, Map<String, Object> model) {
        try (Writer writer = new StringWriter()) {
            Template template = configuration.getTemplate(templateName);
            Map<String, Object> complete = new HashMap<>(model);
            complete.putIfAbsent("locale", "en");
            complete.putIfAbsent("messages", UiText.forLanguage(complete.get("locale").toString()));
            complete.putIfAbsent("siteRootUrl", "/index.html");
            complete.putIfAbsent("archiveUrl", "/archive.html");
            complete.putIfAbsent("feedUrl", "/feed.xml");
            complete.putIfAbsent("searchPageUrl", "/search.html");
            complete.putIfAbsent("searchIndexUrl", "/assets/search-index.json");
            complete.putIfAbsent("languageSwitcher", List.of());
            complete.putIfAbsent("alternates", List.of());
            complete.putIfAbsent("canonicalUrl", "");
            template.process(complete, writer);
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
