package io.github.lunasaw.webdav;

import java.io.*;
import java.net.URL;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.luna.common.constant.Constant;
import com.luna.common.constant.StrPoolConstant;
import com.luna.common.file.FileNameUtil;
import com.luna.common.io.IoUtil;
import com.luna.common.text.StringTools;
import com.luna.common.utils.Assert;
import io.github.lunasaw.webdav.properties.WebDavConfig;
import lombok.extern.slf4j.Slf4j;
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

/**
 * @author luna@mac
 * 2021年03月12日 09:23:00
 */
@Slf4j
public class WebDavUtils implements InitializingBean {

    @Autowired
    private WebDavSupport webDavSupport;

    @Autowired
    private WebDavConfig  webDavConfig;

    /**
     * 上传文件 路径不存在则递归创建目录 不能覆盖
     *
     * @param url 网络文件路径 注意这里需要是绝对路径
     * @param file 文件路径
     * @return
     */
    public boolean upload(URL url, String file, boolean isCreate) {
        return upload(url.toString(), FileTools.read(file), isCreate);
    }

    public boolean upload(URL url, String file) {
        return upload(url.toString(), FileTools.read(file), true);
    }

    /**
     * 上传文件 路径不存在则递归创建目录 默认覆盖 不存在则创建
     *
     * @param filePath 网络文件路径
     * @param scope 包路径
     * @param file 文件流
     * @return
     */
    public boolean upload(String scope, String filePath, String file) {
        return upload(scope, filePath, FileTools.read(file), true, true);
    }

    public boolean upload(String scope, String filePath, String file, boolean created, boolean cover) {
        return upload(scope, filePath, FileTools.read(file), created, cover);
    }

    public boolean upload(String scope, String filePath, String file, boolean created) {
        return upload(scope, filePath, FileTools.read(file), created, true);
    }

    public boolean upload(String url, byte[] file, boolean created) {
        if (!exist(url) && !created) {
            return false;
        }
        return upload(url, IoUtil.toStream(file));
    }

    public boolean upload(String filePath, String file) {
        return upload(webDavConfig.getScope(), filePath, FileTools.read(file), true, true);
    }

    /**
     * 上传文件
     *
     * @param scope 项目scope 最终文件 basePath/${scope}/${filePath}
     * @param filePath 文件名【带后缀】
     * @param file 文件地址
     * @return
     */
    public boolean upload(String scope, String filePath, byte[] file, boolean created, boolean cover) {
        Assert.notNull(scope, "scope 不能为空");
        Assert.notNull(filePath, "文件名不能为空");
        Assert.notNull(file, "上传文件不能为空");

        String fileName = FileNameUtil.getName(filePath);
        String directoryPath = StringTools.removeEnd(filePath, fileName);
        List<String> filePaths = Splitter.on(StrPoolConstant.SLASH).splitToList(directoryPath);
        String basePath = webDavSupport.getBasePath();

        String scopePath = basePath + scope + StrPoolConstant.SLASH;
        if (!exist(scopePath)) {
            if (!makeDir(scopePath)) {
                log.warn("upload::scope = {}, directoryPath = {}, file = {}, created = {}, cover = {}", scope, directoryPath, file, created, cover);
                return false;
            }
        }

        // 创建文件夹
        String lastDir = makeDirs(scopePath, filePaths, created);

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
     * 递归创建文件
     *
     * @param paths 文件网络路径
     * @throws IOException
     */
    public String makeDirs(String basePath, Collection<String> paths, boolean created) {
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
                if (created) {
                    makeDir(stringBuilder.toString());
                }
                throw new RuntimeException("not created, and path not exist");
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
        Assert.isTrue(StringUtils.isNotBlank(url), "路径不能为空");
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
        Assert.isTrue(StringUtils.isNotBlank(url), "路径不能为空");
        try {
            HttpMkcol mkcol = new HttpMkcol(url);
            HttpResponse response = webDavSupport.getClient().execute(mkcol, webDavSupport.getContext());
            return mkcol.succeeded(response);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void download(URL url, String localPath) {
        downloadUrl(url.toString(), localPath);
    }

    public void download(String filePath, String localPath) {
        download(webDavConfig.getScope(), filePath, localPath, true);
    }

    public void download(String scope, String filePath, String localPath) {
        download(scope, filePath, localPath, true);
    }

    public void download(String scope, String filePath, String localPath, boolean create) {
        download(scope, filePath, localPath, create, true);
    }

    /**
     * @param scope  项目scope
     * @param filePath 相对路径
     * @param localPath 本地文件路径
     * @param create
     */
    public void download(String scope, String filePath, String localPath, boolean create, boolean cover) {
        Assert.isTrue(StringUtils.isNotBlank(scope), "Scope路径不能为空");
        String url = Joiner.on(StrPoolConstant.SLASH).join(webDavSupport.getBasePath(), scope, filePath);

        download(url, localPath, create, cover);
    }

    /**
     * 下载文件
     * 
     * @param filePath 文件相对路径
     * @param localPath 本地文件路径
     * @param create 是否创建本地文件夹
     * @param cover 是否覆盖
     */
    public void download(String filePath, String localPath, boolean create, boolean cover) {
        Assert.isTrue(StringUtils.isNotBlank(localPath), "本地路径不能为空");
        Assert.isTrue(StringUtils.isNotBlank(filePath), "文件路径不能为空");
        Path path = Paths.get(localPath);
        if (Files.isDirectory(path) && !FileTools.isExists(localPath) && !create) {
            throw new RuntimeException("not create and path notExists");
        }

        String lastLocalPath = localPath;
        if (Files.isDirectory(path)) {
            if (FileTools.notExists(localPath)) {
                // 文件夹不存在
                if (create) {
                    FileTools.createDirectory(localPath);
                }
            }

            lastLocalPath = localPath + FileNameUtil.getName(filePath);
        } else {
            if (FileTools.isExists(lastLocalPath)) {
                if (!cover) {
                    throw new RuntimeException("not cover and file path isExists");
                }
                FileTools.deleteIfExists(lastLocalPath);
            }
        }

        Assert.isTrue(exist(filePath), "网络文件路径不能为空");
        downloadUrl(filePath, lastLocalPath);
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
            HttpResponse response = webDavSupport.getClient().execute(get, webDavSupport.getContext());
            byte[] bytes = HttpUtils.checkResponseStreamAndGetResult(response);
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
            return status == HttpStatus.SC_CREATED;
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
