package io.github.lunasaw;

import com.alibaba.fastjson2.JSON;
import io.github.lunasaw.webdav.WebDavSupport;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * @author chenzhangyue
 * 2023/4/2
 */
@RunWith(SpringRunner.class)
@TestPropertySource(
        properties = "spring.profiles.active=dev",
        locations = "classpath:application-dev.yml")
@SpringBootTest(
        classes = Main.class)
public class BaseTest {

    @Autowired
    private Environment environment;

    @Autowired
    private WebDavSupport webDavSupport;

    @Test
    public void atest() {
        System.out.println(JSON.toJSONString(webDavSupport.getBasePath()));
    }



    @Test
    public void test() throws MalformedURLException {
        URL url = new URL("http://www.cnblogs.com/index.html?language=cn#j2se");
        // # URL：http://www.cnblogs.com/index.html?language=cn#j2se
        System.out.println("URL = " + url.toString());
        // # protocol：http
        System.out.println("protocol = " + url.getProtocol());
        // # authority：www.cnblogs.com
        System.out.println("authority = " + url.getAuthority());
        // # filename：/index.html?language=cn
        System.out.println("filename = " + url.getFile());
        // # host：www.cnblogs.com
        System.out.println("host = " + url.getHost());
        // # path：/index.html
        System.out.println("path = " + url.getPath());
        // # port：-1
        System.out.println("port = " + url.getPort());
        // # default port：80
        System.out.println("default port = " + url.getDefaultPort());
        // # query：language=cn
        System.out.println("query = " + url.getQuery());
        // # ref：j2se
        System.out.println("ref = " + url.getRef());
    }
}
