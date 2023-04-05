package io.github.lunasaw.webdav.request;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.luna.common.constant.Constant;
import com.luna.common.constant.StrPoolConstant;
import com.luna.common.file.FileNameUtil;
import com.luna.common.file.FileTools;
import com.luna.common.io.IoUtil;
import com.luna.common.text.StringTools;
import com.luna.common.utils.Assert;
import com.luna.common.utils.ObjectUtils;
import io.github.lunasaw.webdav.WebDavSupport;
import io.github.lunasaw.webdav.hander.LockResponseHandler;
import io.github.lunasaw.webdav.properties.WebDavConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.webdav.client.methods.HttpLock;
import org.apache.jackrabbit.webdav.lock.LockInfo;
import org.apache.jackrabbit.webdav.lock.Scope;
import org.apache.jackrabbit.webdav.lock.Type;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author weidian
 * @description Http基础协议处理
 * @date 2023/4/4
 */
@Component
@Slf4j
public class WebDavUtils {

    @Autowired
    private WebDavJackrabbitUtils webDavJackrabbitUtils;

    @Autowired
    private WebDavBaseUtils       webDavBaseUtils;

    @Autowired
    private WebDavConfig          webDavConfig;

    @Autowired
    private WebDavSupport         webDavSupport;

    /**
     * 上传文件 路径不存在则递归创建目录 不能覆盖
     *
     * @param url 网络文件路径 注意这里需要是绝对路径
     * @param file 文件路径
     * @return
     */
    public void upload(URL url, String file, boolean isCreate) {
        upload(url.toString(), FileTools.read(file), isCreate);
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
        upload(url, IoUtil.toStream(file));
        return true;
    }

    public boolean upload(String filePath, String file) {
        byte[] read = FileTools.read(file);
        if (ObjectUtils.isEmpty(read)) {
            return false;
        }
        return upload(webDavConfig.getScope(), filePath, read, true, true);
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
        Assert.isTrue(!ObjectUtils.isEmpty(file), "文件不能为空");

        String fileName = FileNameUtil.getName(filePath);
        String directoryPath = StringTools.removeEnd(filePath, fileName);
        List<String> filePaths =
            Splitter.on(StrPoolConstant.SLASH).splitToList(directoryPath).stream().filter(StringUtils::isNoneBlank).collect(Collectors.toList());
        String basePath = webDavSupport.getBasePath();

        String scopePath = basePath + scope + StrPoolConstant.SLASH;
        if (!exist(scopePath)) {
            if (!webDavJackrabbitUtils.makeDir(scopePath)) {
                log.warn("upload::scope = {}, directoryPath = {}, created = {}, cover = {}", scope, directoryPath, created, cover);
                return false;
            }
        }

        if (filePaths.size() > Constant.NUMBER_ONE && !created) {
            return false;
        }

        // 创建文件夹
        String lastDir = webDavJackrabbitUtils.makeDirs(scopePath, filePaths, created);

        ByteArrayInputStream inputStream = IoUtil.toStream(file);

        String absoluteFilePath = lastDir + fileName;
        if (exist(absoluteFilePath)) {
            if (cover) {
                delete(absoluteFilePath);
                upload(absoluteFilePath, inputStream);
                return true;
            }
        }
        upload(absoluteFilePath, inputStream);
        return true;
    }

    public void download(URL url, String localPath) {
        webDavBaseUtils.downloadUrl(url.toString(), localPath);
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
     * @param scope 项目scope
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
        webDavBaseUtils.downloadUrl(filePath, lastLocalPath);
    }


    /**
     * 使用现有锁继续锁定
     *
     * @param url
     * @return
     */
    public String lockExist(String url, String... lockTokens) {
        return refreshLock(url, Integer.MAX_VALUE, lockTokens);
    }

    public String lockExclusive(String url) {
        return lockExclusive(url, null, Integer.MAX_VALUE);
    }

    public String lockExclusive(String url, long timeout) {
        return lockExclusive(url, null, timeout);
    }

    public String lockExclusive(String url, String owner, long timeout) {
        return lockExclusive(url, owner, timeout, true);
    }

    public String lockExclusive(String url, String owner, long timeout, boolean isDeep) {
        return lockExclusive(url, Type.WRITE, owner, timeout, isDeep);
    }

    /**
     * 独占锁 任何其他会话都无法修改该节点。
     *
     * @return
     */
    public String lockExclusive(String url, Type type, String owner, long timeout, boolean isDeep) {
        return lock(url, Scope.EXCLUSIVE, type, owner, timeout, isDeep);
    }

    /**
     * 共享锁 允许其他会话读取节点，但不允许修改节点。
     *
     * @return
     */
    public String lockShare(String url, Type type, String owner, long timeout, boolean isDeep) {
        return lock(url, Scope.SHARED, type, owner, timeout, isDeep);
    }


    // =============================================

    public void delete(String url) {
        webDavBaseUtils.delete(url);
    }

    public boolean upload(String url, InputStream fis) {
        try {
            webDavBaseUtils.upload(url, fis);
            return true;
        } catch (IOException e) {
            log.error("upload::url = {}", url, e);
            return false;
        }
    }

    public boolean exist(String url) {
        try {
            return webDavJackrabbitUtils.exist(url);
        } catch (Exception e) {
            return false;
        }
    }

    public String lock(String url, Scope scope, Type type, String owner, Long timeout, boolean isDeep){
        Assert.isTrue(StringUtils.isNotBlank(url), "路径不能为空");
        exist(url);
        try {
            return webDavJackrabbitUtils.lock(url, scope, type,owner, timeout, isDeep);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String refreshLock(String url, long timeout, String... lockTokens) {
        Assert.isTrue(StringUtils.isNotBlank(url), "路径不能为空");
        try {
            return webDavJackrabbitUtils.refreshLock(url, timeout, lockTokens);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean unLock(String url, String lockToken) {
        Assert.isTrue(StringUtils.isNotBlank(url), "路径不能为空");
        try {
            return webDavJackrabbitUtils.unLock(url, lockToken);
        } catch (Exception e) {
            log.error("unLock::url = {}, lockToken = {} ", url, lockToken, e);
            return false;
        }
    }
}
