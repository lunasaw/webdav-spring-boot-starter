package io.github.lunasaw.webdav.config;

import io.github.lunasaw.webdav.properties.WebDavConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * @author luna@mac
 * 2021年05月16日 22:44
 */
@Configuration
@EnableConfigurationProperties(WebDavConfig.class)
@ConditionalOnProperty(prefix = "spring.webdav", name = "host")
@ComponentScan("io.github.lunasaw.webdav")
public class WebDavAutoConfiguration {

    @Autowired
    private final WebDavConfig webDavConfig;

    public WebDavAutoConfiguration(WebDavConfig webDavConfig) {
        this.webDavConfig = webDavConfig;
    }
}
