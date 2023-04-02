package io.github.lunasaw;

import io.github.lunasaw.webdav.WebDavUtils;
import org.apache.http.HttpEntity;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;

/**
 * @author chenzhangyue
 * 2023/4/2
 */
public class FileUploadTest extends BaseTest{

    @Autowired
    private WebDavUtils webDavUtils;

    @Test
    public void atest() {
            webDavUtils.upload("test", "images/buy_logo.jpeg","/Users/weidian/compose/images/buy_logo.jpeg");
    }

}
