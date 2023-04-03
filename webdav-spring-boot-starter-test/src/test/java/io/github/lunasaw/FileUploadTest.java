package io.github.lunasaw;

import com.luna.common.file.FileTools;
import com.luna.common.text.StringTools;
import com.luna.common.utils.Assert;
import io.github.lunasaw.webdav.WebDavUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * @author chenzhangyue
 * 2023/4/2
 */
public class FileUploadTest extends BaseTest {

    @Autowired
    private WebDavUtils webDavUtils;

    @Test
    public void atest() {
        boolean test =
            webDavUtils.upload("/images/buy_logo.jpeg", "/Users/weidian/compose/images/buy_logo.jpeg");
        Assert.isTrue(test);
        boolean exist = webDavUtils.exist("http://localhost:8080/webdav/project/test/images/buy_logo.jpeg");
        Assert.isTrue(exist);
    }

    @Test
    public void btest() {
        String ROUND_FILE_PATH = "/Users/weidian/compose/images/buy_logo_{}.jpeg";
        ROUND_FILE_PATH = StringTools.format(ROUND_FILE_PATH, RandomUtils.nextInt());
        webDavUtils.download("test", "/images/buy_logo.jpeg", ROUND_FILE_PATH);
        Assert.isTrue(FileTools.isExists(ROUND_FILE_PATH), ROUND_FILE_PATH + "文件下载错误");
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
