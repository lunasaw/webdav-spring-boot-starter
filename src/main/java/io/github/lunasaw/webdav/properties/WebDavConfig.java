package io.github.lunasaw.webdav.properties;

import java.net.URI;

import lombok.Data;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author luna@mac
 * 2021年03月12日 09:24:00
 */
@ConfigurationProperties(prefix = "luna.webdav")
@Data
public class WebDavConfig implements InitializingBean{

    private String            username;
    private String            password;

    /**
     * domain:port
     */
    private String            host               = "127.0.0.1:8080";

    /**
     * like "/webdav"
     */
    private String            path               = "";

    private Integer           maxTotal           = 100;

    private Integer           defaultMaxPerRoute = 80;

    private URI               uri;
    private HttpClient        client;
    private HttpClientContext context;

    public void initClientContext() {
        initClientContext(host, path, username, password);
    }

    public void initClientContext(String host, String path, String userName, String passWord) {

        this.uri = URI.create(host + path);
        this.username = userName;
        this.password = passWord;

        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        // 设置最大连接数
        cm.setMaxTotal(defaultMaxPerRoute);
        cm.setDefaultMaxPerRoute(defaultMaxPerRoute);
        HttpHost targetHost = new HttpHost(host);

        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        UsernamePasswordCredentials upc = new UsernamePasswordCredentials(this.username, this.password);
        credsProvider.setCredentials(new AuthScope(targetHost.getHostName(), targetHost.getPort()), upc);

        AuthCache authCache = new BasicAuthCache();
        // Generate BASIC scheme object and add it to the local auth cache
        BasicScheme basicAuth = new BasicScheme();
        authCache.put(targetHost, basicAuth);

        // Add AuthCache to the execution context
        this.context = HttpClientContext.create();
        this.context.setCredentialsProvider(credsProvider);
        this.context.setAuthCache(authCache);
        this.client = HttpClients.custom().setConnectionManager(cm).build();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        initClientContext();
    }
}
