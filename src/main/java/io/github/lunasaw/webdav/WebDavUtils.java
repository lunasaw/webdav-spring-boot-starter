package io.github.lunasaw.webdav;

import java.io.*;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.luna.common.constant.Constant;
import com.luna.common.constant.StrPoolConstant;
import com.luna.common.file.FileNameUtil;
import com.luna.common.io.IoUtil;
import com.luna.common.text.StringTools;
import com.luna.common.utils.Assert;
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
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author luna@mac
 * 2021年03月12日 09:23:00
 */
public class WebDavUtils implements InitializingBean {

    @Autowired
    private WebDavSupport webDavSupport;

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
     * @param fileName 网络文件路径
     * @param path 文件流
     * @return
     */
    public boolean upload(String scope, String fileName, String path) {
        return upload(scope, fileName, FileTools.read(path), true);
    }

    /**
     * 上传文件
     * 
     * @param scope 项目scope
     * @param filePath 文件名【带后缀】
     * @param file 文件地址
     * @return
     */
    public boolean upload(String scope, String filePath, byte[] file, boolean cover) {
        Assert.notNull(scope, "scope 不能为空");
        Assert.notNull(filePath, "文件名不能为空");
        Assert.notNull(file, "上传文件不能为空");

        String fileName = FileNameUtil.getName(filePath);
        filePath = StringTools.removeEnd(filePath, fileName);
        List<String> filePaths = Splitter.on(StrPoolConstant.SLASH).splitToList(filePath);
        String basePath = webDavSupport.getBasePath();

        String scopePath = basePath + scope + StrPoolConstant.SLASH;
        if (!exist(scopePath)) {
            makeDir(scopePath);
        }

        // 创建文件夹
        String lastDir = makeDirs(scopePath, filePaths);

        ByteArrayInputStream inputStream = IoUtil.toStream(file);

        String absoluteFilePath = lastDir + fileName;
        if (exist(absoluteFilePath)) {
            if (cover) {
                if (delete(absoluteFilePath)) {
                    return upload(absoluteFilePath, inputStream);
                }
            } else {
                return false;
            }
        }
        return upload(absoluteFilePath, inputStream);

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
        if (!exist(url)) {
            if (!isCreate) {
                throw new RuntimeException("路径不存在!");
            }
            makeDirs("", Lists.newArrayList(url));
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
     * @param paths 文件网络路径
     * @throws IOException
     */
    public String makeDirs(String basePath, Collection<String> paths) {
        StringBuilder stringBuilder = new StringBuilder(basePath);

        for (String path : paths) {
            if (StringUtils.isBlank(path)) {
                continue;
            }
            stringBuilder.append(path);
            if (!path.endsWith(StrPoolConstant.SLASH)) {
                stringBuilder.append(StrPoolConstant.SLASH);
            }
            if (!exist(stringBuilder.toString())) {
                makeDir(stringBuilder.toString());
            }
        }

        return stringBuilder.toString();
    }

    /**
     * 判断文件或者文件夹是否存在
     * 
     * @param url 路径
     * @return
     */
    public boolean exist(String url) {
        return exist(url, DavConstants.PROPFIND_BY_PROPERTY, Constant.NUMBER_ONE);
    }

    /**
     * 判断文件或者文件夹是否存在
     *
     * @param url 网络路径
     * @return
     */
    public boolean exist(String url, int propfindType, int dep) {
        Assert.isTrue(StringUtils.isNotBlank(url), "路径不能为空");
        try {
            HttpPropfind propfind = new HttpPropfind(url, propfindType, dep);
            HttpResponse response = webDavSupport.executeWithContext(propfind);
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
    public boolean delete(String url) {
        try {
            HttpDelete delete = new HttpDelete(url);
            int statusCode =
                webDavSupport.getClient().execute(delete, webDavSupport.getContext()).getStatusLine().getStatusCode();
            return HttpStatus.SC_NO_CONTENT == statusCode;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean makeDir(String url) {
        return touchFile(url);
    }

    /**
     * 创建文件夹
     * 
     * @param url 路径
     * @return
     */
    private boolean touchFile(String url) {
        try {
            HttpMkcol mkcol = new HttpMkcol(url);
            HttpResponse response = webDavSupport.getClient().execute(mkcol, webDavSupport.getContext());
            return mkcol.succeeded(response);
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
            HttpResponse response = webDavSupport.getClient().execute(get, webDavSupport.getContext());
            byte[] bytes = HttpUtils.checkResponseStreamAndGetResult(response);
            FileTools.write(bytes, filePath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        boolean exist = exist(webDavSupport.getBasePath());
        Assert.isTrue(exist, "路径不存在");
    }
}
