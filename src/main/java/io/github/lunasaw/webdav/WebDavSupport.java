package io.github.lunasaw.webdav;

import com.luna.common.constant.StrPoolConstant;
import com.luna.common.utils.Assert;
import io.github.lunasaw.webdav.properties.WebDavConfig;
import lombok.Data;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URL;

/**
 * @author chenzhangyue
 * 2023/4/2
 */
@Data
@Component
public class WebDavSupport implements InitializingBean {

    @Autowired
    private WebDavConfig      webDavConfig;

    private HttpClient        client;
    private HttpClientContext context;

    private URL               url;

    public HttpResponse executeWithContext(HttpRequestBase base) throws IOException {
        return client.execute(base, context);
    }

    public String getBasePath() {
        return url.toString();
    }

    public void initClientContext() {

        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        // 设置最大连接数
        cm.setMaxTotal(webDavConfig.getMaxTotal());
        cm.setDefaultMaxPerRoute(webDavConfig.getDefaultMaxPerRoute());
        HttpHost targetHost = new HttpHost(webDavConfig.getHost());

        CredentialsProvider provider = new BasicCredentialsProvider();

        UsernamePasswordCredentials upc = new UsernamePasswordCredentials(webDavConfig.getUsername(), webDavConfig.getPassword());
        provider.setCredentials(AuthScope.ANY, upc);

        AuthCache authCache = new BasicAuthCache();
        // Generate BASIC scheme object and add it to the local auth cache
        BasicScheme basicAuth = new BasicScheme();
        authCache.put(targetHost, basicAuth);

        // Add AuthCache to the execution context
        HttpClientContext context = HttpClientContext.create();
        context.setCredentialsProvider(provider);
        context.setAuthCache(authCache);
        this.context = context;
        this.client = HttpClients.custom().setConnectionManager(cm).build();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        initClientContext();
        this.url = new URL(webDavConfig.getHost() + webDavConfig.getPath() + StrPoolConstant.SLASH);
    }
}
