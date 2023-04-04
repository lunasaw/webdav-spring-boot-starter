package io.github.lunasaw.webdav.request;

import java.io.*;
import java.nio.charset.StandardCharsets;

import com.alibaba.fastjson.JSON;
import com.luna.common.utils.Assert;
import io.github.lunasaw.webdav.WebDavSupport;
import io.github.lunasaw.webdav.properties.WebDavConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.*;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.InputStreamEntity;
import com.luna.common.file.FileTools;
import org.apache.http.entity.StringEntity;
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
    public boolean delete(String url) {
        Assert.isTrue(StringUtils.isNotBlank(url), "路径不能为空");
        try {
            HttpDelete delete = new HttpDelete(url);
            HttpResponse httpResponse = webDavSupport.getClient().execute(delete);
            int statusCode = httpResponse.getStatusLine().getStatusCode();
            return HttpStatus.SC_NO_CONTENT == statusCode;
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
    public void downloadUrl(String url, String filePath) {
        Assert.isTrue(StringUtils.isNotBlank(url), "路径不能为空");
        Assert.isTrue(StringUtils.isNotBlank(filePath), "文件夹不能为空");

        try {
            HttpGet get = new HttpGet(url);
            HttpResponse response = webDavSupport.executeWithContext(get);
            byte[] bytes = com.luna.common.net.HttpUtils.checkResponseStreamAndGetResult(response);
            FileTools.write(bytes, filePath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 上传文件 路径不存在则创建
     *
     * @param url 网络文件路径
     * @param fis 文件流
     * @return
     */
    public boolean upload(String url, InputStream fis) {
        try {
            HttpPut put = new HttpPut(url);
            InputStreamEntity requestEntity = new InputStreamEntity(fis);
            put.setEntity(requestEntity);
            HttpResponse response = webDavSupport.executeWithContext(put);
            int status = response.getStatusLine().getStatusCode();
            if (status != HttpStatus.SC_CREATED){
                log.warn("upload::url = {}, response = {}", url, JSON.toJSONString(response));
                return false;
            }
            return true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
