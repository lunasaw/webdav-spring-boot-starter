package com.luna.webdav;

import java.net.URI;

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
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author luna@mac
 * 2021年03月12日 09:24:00
 */
@ConfigurationProperties(prefix = "luna.webdav")
public class WebDavConfig {

    private String            username, password;
    private URI               uri;
    private HttpClient        client;
    private HttpClientContext context;

    public void initClientContext() {
        initClientContext(uri.toString(), username, password);
    }

    public void initClientContext(String baseUri, String userName,
        String passWord) {
        if (!baseUri.endsWith("/")) {
            baseUri += "/";
        }
        this.uri = URI.create(baseUri);
        this.username = userName;
        this.password = passWord;

        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        // 设置最大连接数
        cm.setMaxTotal(100);
        cm.setDefaultMaxPerRoute(80);
        HttpHost targetHost = new HttpHost(uri.getHost(), uri.getPort());

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

    public String getUsername() {
        return username;
    }

    public WebDavConfig setUsername(String username) {
        this.username = username;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public WebDavConfig setPassword(String password) {
        this.password = password;
        return this;
    }

    public URI getUri() {
        return uri;
    }

    public WebDavConfig setUri(URI uri) {
        this.uri = uri;
        return this;
    }

    public HttpClient getClient() {
        return client;
    }

    public WebDavConfig setClient(HttpClient client) {
        this.client = client;
        return this;
    }

    public HttpClientContext getContext() {
        return context;
    }

    public WebDavConfig setContext(HttpClientContext context) {
        this.context = context;
        return this;
    }

    @Override
    public String toString() {
        return "WebDavConfig{" +
            "username='" + username + '\'' +
            ", password='" + password + '\'' +
            ", uri=" + uri +
            '}';
    }
}
