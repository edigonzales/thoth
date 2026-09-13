package guru.interlis.thoth.biblios.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Thoth Biblios Server - authenticated documentation portal.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class BibliosServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(BibliosServerApplication.class, args);
    }
}
