package io.github.lunasaw.webdav.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author luna@mac
 * 2021年03月12日 09:24:00
 */
@ConfigurationProperties(prefix = "luna.webdav")
@Data
public class WebDavConfig {

    private String  username;
    private String  password;

    /**
     * domain:port
     */
    private String  host               = "127.0.0.1:8080";
    /**
     * like "/webdav"
     */
    private String  path               = "";

    private Integer maxTotal           = 100;

    private Integer defaultMaxPerRoute = 80;

    private String  scope;
}
