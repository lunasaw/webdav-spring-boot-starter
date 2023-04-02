package com.luna.webdav.config;

import com.luna.webdav.WebDavConfig;
import com.luna.webdav.WebDavUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author luna@mac
 * 2021年05月16日 22:44
 */
@Configuration
@EnableConfigurationProperties(WebDavConfig.class)
@ConditionalOnProperty(prefix = "luna.webdav", name = "host")
public class WebDavAutoConfiguration {

    @Autowired
    private final WebDavConfig webDavConfig;

    public WebDavAutoConfiguration(WebDavConfig webDavConfig) {
        this.webDavConfig = webDavConfig;
    }

    @Bean
    public WebDavUtils webDavUtils() {
        return new WebDavUtils();
    }

}
