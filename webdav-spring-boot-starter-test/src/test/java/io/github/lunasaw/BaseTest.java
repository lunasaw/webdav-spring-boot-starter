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

}
