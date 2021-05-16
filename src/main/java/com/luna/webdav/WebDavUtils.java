package com.luna.webdav;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.InputStreamEntity;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.client.methods.HttpMkcol;
import org.apache.jackrabbit.webdav.client.methods.HttpPropfind;

import com.luna.common.file.FileTools;
import com.luna.common.net.HttpUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author luna@mac
 * 2021年03月12日 09:23:00
 */
@Service
public class WebDavUtils {

    @Autowired
    private WebDavConfig webDavConfig;

    /**
     * 上传文件 路径不存在则创建
     * 
     * @param url 网络文件路径
     * @param fis 文件流
     * @return
     */
    public boolean upload(String url, FileInputStream fis) {
        try {
            HttpPut put = new HttpPut(url);
            InputStreamEntity requestEntity = new InputStreamEntity(fis);
            put.setEntity(requestEntity);
            int status =
                webDavConfig.getClient().execute(put, webDavConfig.getContext()).getStatusLine().getStatusCode();
            return status == HttpStatus.SC_CREATED;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 上传文件 路径不存在则递归创建目录 不能覆盖
     *
     * @param url 网络文件路径
     * @param path 文件路径
     * @return
     */
    public boolean upload(String url, String path, boolean isCreate) {
        return upload(url, path, isCreate, false);
    }

    /**
     * 上传文件 路径不存在则递归创建目录 默认覆盖 不存在则创建
     *
     * @param url 网络文件路径
     * @param path 文件流
     * @return
     */
    public boolean upload(String url, String path) {
        return upload(url, path, true, true);
    }

    /**
     * 上传文件 路径不存在则递归创建目录，文件存在则覆盖
     * 
     * @param url 网络路径
     * @param path 文件
     * @param isCreate 路径不存在是否创建文件夹
     * @param cover 是否覆盖
     * @return
     * @throws IOException
     */
    public boolean upload(String url, String path, boolean isCreate, boolean cover) {
        if (!existDir(url)) {
            if (!isCreate) {
                throw new RuntimeException("路径不存在!");
            }
            makeDirs(url);
        }
        if (!cover) {
            throw new RuntimeException("路径已存在!");
        }
        delete(url);
        try {
            return upload(url, new FileInputStream(path));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 递归创建文件
     * 
     * @param path 文件网络路径
     * @throws IOException
     */
    public void makeDirs(String path) {
        path = path.replace(webDavConfig.getUri().toString(), StringUtils.EMPTY);
        String[] dirs = path.split("/");
        if (path.lastIndexOf(".") > 0) {
            dirs = Arrays.copyOf(dirs, dirs.length - 1);
        }
        StringBuilder stringBuilder = new StringBuilder(webDavConfig.getUri().toString());
        for (String string : dirs) {
            stringBuilder.append(string + "/");
            if (existDir(stringBuilder.toString())) {
                continue;
            }
            makeDir(stringBuilder.toString());
        }
    }

    /**
     * 判断文件或者文件夹是否存在
     * 
     * @param url 路径
     * @return
     */
    public boolean existDir(String url) {
        try {
            HttpPropfind propfind = new HttpPropfind(url, DavConstants.PROPFIND_BY_PROPERTY, 1);
            HttpResponse response = webDavConfig.getClient().execute(propfind, webDavConfig.getContext());
            int statusCode = response.getStatusLine().getStatusCode();
            return DavServletResponse.SC_MULTI_STATUS == statusCode;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 删除文件或者文件夹
     * 
     * @param url 文件路径
     * @return
     */
    public Boolean delete(String url) {
        try {
            HttpDelete delete = new HttpDelete(url);
            int statusCode =
                webDavConfig.getClient().execute(delete, webDavConfig.getContext()).getStatusLine().getStatusCode();
            return HttpStatus.SC_NO_CONTENT == statusCode;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 创建文件夹
     * 
     * @param url 路径
     * @return
     */
    private boolean makeDir(String url) {
        try {
            HttpMkcol mkcol = new HttpMkcol(url);
            int retCode =
                webDavConfig.getClient().execute(mkcol, webDavConfig.getContext()).getStatusLine().getStatusCode();
            return retCode == HttpStatus.SC_CREATED;
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
    public void download(String url, String filePath) {
        try {
            HttpGet get = new HttpGet(url);
            HttpResponse response = webDavConfig.getClient().execute(get, webDavConfig.getContext());
            byte[] bytes = HttpUtils.checkResponseStreamAndGetResult(response);
            FileTools.write(bytes, filePath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
