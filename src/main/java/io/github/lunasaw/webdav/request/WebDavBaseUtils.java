package io.github.lunasaw.webdav.request;

import java.io.*;

import com.luna.common.utils.Assert;
import io.github.lunasaw.webdav.WebDavSupport;
import io.github.lunasaw.webdav.hander.DownloadHandler;
import io.github.lunasaw.webdav.hander.ValidatingResponseHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.*;
import org.apache.http.entity.InputStreamEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author luna
 * @description 基础org.apache.http.client.methods请求
 * @date 2021年03月12日 09:23:00
 */
@Slf4j
@Component
public class WebDavBaseUtils {

    @Autowired
    private WebDavSupport webDavSupport;

    /**
     * 删除文件或者文件夹
     *
     * @param url 文件路径
     * @return
     */
    public void delete(String url) {
        Assert.isTrue(StringUtils.isNotBlank(url), "路径不能为空");
        try {
            HttpDelete delete = new HttpDelete(url);
            webDavSupport.execute(delete, new ValidatingResponseHandler<Void>() {
                @Override
                public Void handleResponse(HttpResponse httpResponse) throws IOException {
                    this.validateResponse(httpResponse);
                    return null;
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 下载文件
     *
     * @param url 路径
     * @param filePath 文件存储路径
     * @throws IOException
     */
    public Boolean download(String url, String filePath) throws IOException {
        HttpGet get = new HttpGet(url);
        return webDavSupport.execute(get, new DownloadHandler(filePath));
    }

    /**
     * 上传文件 路径不存在则创建
     *
     * @param url 网络文件路径
     * @param fis 文件流
     * @return
     */
    public void upload(String url, InputStream fis) throws IOException {
        HttpPut put = new HttpPut(url);
        InputStreamEntity requestEntity = new InputStreamEntity(fis);
        put.setEntity(requestEntity);
        webDavSupport.execute(put, new ValidatingResponseHandler<Void>() {
            @Override
            public Void handleResponse(HttpResponse httpResponse) throws IOException {
                this.validateResponse(httpResponse);
                return null;
            }
        });
    }
}
