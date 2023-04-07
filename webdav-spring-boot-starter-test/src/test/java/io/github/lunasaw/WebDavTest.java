package io.github.lunasaw;

import com.alibaba.fastjson2.JSON;
import com.luna.common.file.FileTools;
import com.luna.common.io.IoUtil;
import com.luna.common.text.StringTools;
import com.luna.common.utils.Assert;
import io.github.lunasaw.webdav.WebDavSupport;
import io.github.lunasaw.webdav.request.WebDavJackrabbitUtils;
import io.github.lunasaw.webdav.request.WebDavUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.*;

/**
 * @author chenzhangyue
 * 2023/4/2
 */
@Slf4j
public class WebDavTest extends BaseTest {

    private static final String   IMAGE      = "buy_logo.jpeg";
    @Autowired
    private WebDavUtils           webDavUtils;

    @Autowired
    private WebDavSupport         webDavSupport;

    @Autowired
    private WebDavJackrabbitUtils webDavJackrabbitUtils;

    private String                SCOPE_PATH = StringUtils.EMPTY;

    @Before
    public void pre() {
        SCOPE_PATH = webDavSupport.getScopePath();
    }

    @Test
    public void a_upload_test() throws FileNotFoundException {
        File file = ResourceUtils.getFile(ResourceUtils.CLASSPATH_URL_PREFIX + IMAGE);
        Assert.isTrue(webDavUtils.upload(IMAGE, file.getAbsolutePath()));
        Assert.isTrue(webDavUtils.exist(SCOPE_PATH + IMAGE));
    }

    @Test
    public void download_test() {
        String localPath = FileTools.getUserHomePath() + "/buy_logo_{}.jpeg";
        localPath = StringTools.format(localPath, RandomUtils.nextInt());
        webDavUtils.download(SCOPE_PATH + IMAGE, localPath);
        Assert.isTrue(FileTools.isExists(localPath), localPath + "文件下载错误");
        FileTools.deleteIfExists(localPath);
    }

    @Test
    public void copy_file_test() {
        Assert.isTrue(webDavUtils.exist(SCOPE_PATH + IMAGE));
        boolean copy = webDavUtils.copy(SCOPE_PATH + IMAGE, SCOPE_PATH + IMAGE + ".copy");
        assertTrue(copy);
    }

    @Test
    public void copy_dir_test() {
        assertTrue(webDavUtils.mkdir(SCOPE_PATH + "test"));
        assertTrue(webDavUtils.existDir(SCOPE_PATH + "test"));
        boolean copy = webDavUtils.copyDir(SCOPE_PATH + "test", SCOPE_PATH + "test2");
        assertTrue(copy);
    }

    @Test
    public void copy_move() {
        Assert.isTrue(webDavUtils.exist(SCOPE_PATH + IMAGE + ".copy"));
        boolean copy = webDavUtils.move(SCOPE_PATH + IMAGE + ".copy", SCOPE_PATH + IMAGE + ".copy2");
        assertTrue(copy);
    }

    @Test
    public void exist_list() {
        List<String> list = webDavUtils.listFileName(SCOPE_PATH + "images", Integer.MAX_VALUE);
        System.out.println(JSON.toJSONString(list));
    }

    @Test
    public void option_list() {
        String url = webDavSupport.getBasePath();
        Set<String> option = webDavJackrabbitUtils.option(url);
        System.out.println(option);
    }

    @Test
    public void c_lock_first_test() {
        String url = String.format(SCOPE_PATH + "%s", UUID.randomUUID());
        assertTrue(webDavUtils.upload(url, IoUtil.toStream(new byte[] {1})));
        String token = webDavUtils.lockExclusive(url, 50000);
        assertTrue(token.startsWith("opaquelocktoken:"));
        assertFalse(webDavUtils.delete(url));
        boolean lock = webDavUtils.unLock(url, token);
        assertTrue(lock);
        assertTrue(webDavUtils.delete(url));
    }

    @Test
    public void b_create_test() {
        String url = String.format(SCOPE_PATH + "%s", UUID.randomUUID());
        assertTrue(webDavUtils.upload(url, IoUtil.toStream(new byte[] {1})));
        assertTrue(webDavUtils.exist(url));
    }

    @Test
    public void d_lock_second_test() {
        String url = String.format(SCOPE_PATH + "%s", UUID.randomUUID());
        assertTrue(webDavUtils.upload(url, IoUtil.toStream(new byte[] {1})));
        String token = webDavUtils.lockExclusive(url, 50000);
        assertTrue(token.startsWith("opaquelocktoken:"));
        String result = webDavUtils.refreshLock(url, 5000 * 20, token);
        assertEquals(token, result);
        assertFalse(webDavUtils.delete(url));
        assertTrue(webDavUtils.unLock(url, token));
        assertTrue(webDavUtils.delete(url));
    }

    @Test
    public void test_allow() {
        Set<String> allow = webDavJackrabbitUtils.getAllow(webDavSupport.getBasePath());
        assertTrue(CollectionUtils.isNotEmpty(allow));
    }
}
