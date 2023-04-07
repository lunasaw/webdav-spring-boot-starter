package io.github.lunasaw;

import com.alibaba.fastjson2.JSON;
import com.luna.common.file.FileTools;
import com.luna.common.text.StringTools;
import com.luna.common.utils.Assert;
import io.github.lunasaw.webdav.WebDavSupport;
import io.github.lunasaw.webdav.entity.MultiStatusResult;
import io.github.lunasaw.webdav.request.WebDavBaseUtils;
import io.github.lunasaw.webdav.request.WebDavJackrabbitUtils;
import io.github.lunasaw.webdav.request.WebDavUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.assertTrue;

/**
 * @author chenzhangyue
 * 2023/4/2
 */
@Slf4j
public class FileUploadTest extends BaseTest {

    @Autowired
    private WebDavUtils           webDavUtils;

    @Autowired
    private WebDavSupport         webDavSupport;

    @Autowired
    private WebDavJackrabbitUtils webDavJackrabbitUtils;

    @Test
    public void upload_test() {
        Assert.isTrue(webDavUtils.upload("buy_logo.jpeg", "buy_logo.jpeg"));
        Assert.isTrue(webDavUtils.exist(webDavSupport.getBasePath() + "buy_logo.jpeg"));
    }

    @Test
    public void download_test() {
        String localPath = "/Users/weidian/compose/images/buy_logo_{}.jpeg";
        localPath = StringTools.format(localPath, RandomUtils.nextInt());
        webDavUtils.download("luna", "/images/buy_logo.jpeg", localPath);
        Assert.isTrue(FileTools.isExists(localPath), localPath + "文件下载错误");
    }

    @Test
    public void copy_test() throws IOException {
        String filePath = webDavSupport.getBasePath();
        boolean copy = webDavUtils.copy(filePath + "/luna/", filePath + "/test4/", true, true);
        assertTrue(copy);
    }

    @Test
    public void copy_move() throws IOException {
        String filePath = webDavSupport.getBasePath();
        String url = String.format("testMove3/%s", UUID.randomUUID());
        webDavUtils.upload("luna", url, new byte[] {1, 2});
        assertTrue(webDavUtils.exist(filePath + "luna/" + url));
        String dest = String.format("testMove3/%s", UUID.randomUUID());
        boolean copy = webDavUtils.move(filePath + "luna/" + url, filePath + "luna/" + dest, true);
        assertTrue(copy);
    }

    @Test
    public void test_exist() {
        boolean exist = webDavUtils.exist(webDavSupport.getBasePath());
        assertTrue(exist);
    }

    @Test
    public void exist_list() {
        String filePath = webDavSupport.getBasePath();
        List<String> list = webDavUtils.listFileName(filePath + "luna/images", Integer.MAX_VALUE);
        System.out.println(JSON.toJSONString(list));
    }

    @Test
    public void option_list() {
        String url = webDavSupport.getBasePath();
        Set<String> option = webDavJackrabbitUtils.option(url);
        System.out.println(option);
    }

    @Test
    public void lock_first_test() {
        String basePath = webDavSupport.getBasePath();
        String url = String.format("luna/images2/%s", UUID.randomUUID());
        boolean upload = webDavUtils.uploadAutoScope(url, new byte[] {1});
        url = basePath + url;
        assertTrue(upload);
        String token = webDavUtils.lockExclusive(url, 50000);
        assertTrue(token.startsWith("opaquelocktoken:"));
        boolean delete = webDavUtils.delete(url);
        assertTrue(!delete);
        boolean lock = webDavUtils.unLock(url, token);
        assertTrue(lock);
        delete = webDavUtils.delete(url);
        assertTrue(delete);
    }

    @Test
    public void create_test() {
        String filePath = webDavSupport.getBasePath();
        String url = String.format("images/%s", UUID.randomUUID());
        webDavUtils.upload("luna", url, new byte[] {1, 2});
        assertTrue(webDavUtils.exist(filePath + "luna/" + url));
    }

    @Test
    public void lock_second_test() {
        String filePath = webDavSupport.getBasePath();
        String url = String.format("luna/%s", UUID.randomUUID());
        webDavUtils.uploadAutoScope(url, new byte[] {1}, true);
        url = filePath + url;
        String token = webDavUtils.lockExclusive(url, 5000);
        String result = webDavUtils.refreshLock(url, 5000 * 20, token);
        assertTrue(token.startsWith("opaquelocktoken:"));
        assertTrue(token.equals(result));

        boolean delete = webDavUtils.delete(url);
        assertTrue(!delete);
        boolean lock = webDavUtils.unLock(url, token);
        assertTrue(lock);
        delete = webDavUtils.delete(url);
        assertTrue(delete);
    }

    @Test
    public void test_allow() {
        Set<String> allow = webDavJackrabbitUtils.getAllow(webDavSupport.getBasePath());
        assertTrue(CollectionUtils.isNotEmpty(allow));
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
