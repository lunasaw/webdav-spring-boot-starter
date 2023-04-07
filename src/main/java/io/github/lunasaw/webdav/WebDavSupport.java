package io.github.lunasaw.webdav;

import com.luna.common.constant.StrPoolConstant;
import com.luna.common.utils.Assert;
import io.github.lunasaw.webdav.properties.WebDavConfig;
import lombok.Data;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.*;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
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

    /**
     * Validate the response using the response handler. Aborts the request if there is an exception.
     *
     * @param <T> Return type
     * @param request Request to execute
     * @param responseHandler Determines the return type.
     * @return parsed response
     */
    public <T> T execute(HttpRequestBase request, ResponseHandler<T> responseHandler)
        throws IOException {
        return execute(context, request, responseHandler);
    }

    /**
     * No validation of the response. Aborts the request if there is an exception.
     *
     * @param request Request to execute
     * @return The response to check the reply status code
     */
    public HttpResponse execute(HttpRequestBase request)
        throws IOException {
        return execute(context, request, null);
    }

    /**
     * Common method as single entry point responsible fo request execution
     * 
     * @param context clientContext to be used when executing request
     * @param request Request to execute
     * @param responseHandler can be null if you need raw HttpResponse or not null response handler for result handling.
     * @param <T> will return raw HttpResponse when responseHandler is null or value reslved using provided
     * ResponseHandler instance
     * @return value resolved using response handler or raw HttpResponse when responseHandler is null
     */
    protected <T> T execute(HttpClientContext context, HttpRequestBase request, ResponseHandler<T> responseHandler)
        throws IOException {
        HttpContext requestLocalContext = new BasicHttpContext(context);
        try {
            if (responseHandler != null) {
                return this.client.execute(request, responseHandler, requestLocalContext);
            } else {
                return (T)this.client.execute(request, requestLocalContext);
            }
        } catch (HttpResponseException e) {
            // Don't abort if we get this exception, caller may want to repeat request.
            throw e;
        } catch (IOException e) {
            request.abort();
            throw e;
        } finally {
            context.setAttribute(HttpClientContext.USER_TOKEN, requestLocalContext.getAttribute(HttpClientContext.USER_TOKEN));
        }
    }

    /**
     * 使用的基础项目路径 = webDavConfig.getHost() + webDavConfig.getPath() + StrPoolConstant.SLASH
     * @return
     */
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
