package guru.interlis.thoth.biblios.server.web;

import guru.interlis.thoth.biblios.server.publication.PublicationPackage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;

import java.util.List;

/**
 * Spring's resource handler for package files: range requests, HEAD and
 * conditional requests are handled by the framework, authorization and manifest
 * checks stay in {@link ContentController}.
 */
@Configuration
public class PackageResourceConfiguration {

    @Bean
    public ResourceHttpRequestHandler packageFileRequestHandler(PublicationPackage publicationPackage) {
        ResourceHttpRequestHandler handler = new ResourceHttpRequestHandler();
        handler.setLocations(List.of(new FileSystemResource(
            publicationPackage.root().resolve("files").toString() + "/")));
        return handler;
    }
}
