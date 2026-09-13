package guru.interlis.thoth.biblios.server.view;

import freemarker.cache.ClassTemplateLoader;
import freemarker.template.TemplateExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;

/**
 * FreeMarker setup for server-side frame rendering.
 *
 * <p>The server configures FreeMarker itself instead of relying on Spring Boot's
 * auto-configuration: the same classpath templates are used as in the generator,
 * with an optional external override directory at runtime. Localized lookup is
 * disabled, which keeps template rendering predictable in native images.</p>
 */
@Configuration
public class FreeMarkerServerConfiguration {

    @Bean
    public freemarker.template.Configuration freeMarkerConfiguration() {
        freemarker.template.Configuration configuration =
            new freemarker.template.Configuration(freemarker.template.Configuration.VERSION_2_3_34);
        configuration.setTemplateLoader(new ClassTemplateLoader(FrameRenderer.class, "/templates"));
        configuration.setDefaultEncoding(StandardCharsets.UTF_8.name());
        configuration.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        configuration.setLogTemplateExceptions(false);
        configuration.setWrapUncheckedExceptions(true);
        configuration.setLocalizedLookup(false);
        return configuration;
    }
}
