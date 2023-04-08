package io.github.lunasaw.webdav.properties;

import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author luna@mac
 * 2021年03月12日 09:24:00
 */
@Data
@ConfigurationProperties(prefix = "spring.webdav")
public class WebDavConfig {

    /**
     * 用户名
     */
    private String        username;
    /**
     * 密码
     */
    private String        password;

    /**
     * domain:port
     */
    private String        host               = "127.0.0.1:8080";
    /**
     * like "/webdav"
     */
    private String        path               = "/";

    private Integer       maxTotal           = 100;

    private Integer       defaultMaxPerRoute = 80;

    /**
     * 项目名称
     */
    private String        scope;

    public static Boolean openLog            = false;

    public void setOpenLog(Boolean openLog) {
        WebDavConfig.openLog = openLog;
    }
}
