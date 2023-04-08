package io.github.lunasaw.webdav.request;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.luna.common.constant.Constant;
import com.luna.common.constant.StrPoolConstant;
import com.luna.common.file.FileNameUtil;
import com.luna.common.file.FileTools;
import com.luna.common.io.IoUtil;
import com.luna.common.text.StringTools;
import com.luna.common.utils.Assert;
import com.luna.common.utils.ObjectUtils;
import io.github.lunasaw.webdav.WebDavSupport;
import io.github.lunasaw.webdav.entity.MultiStatusResult;
import io.github.lunasaw.webdav.properties.WebDavConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.client.methods.HttpOptions;
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
import java.util.*;
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
     * 文件树创建文件
     *
     * @param basePath 基础路径
     * @param paths 新建路径
     * @param created 父级目录不存在 是否创建
     * @return
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
                    mkdir(stringBuilder.toString());
                }
            }
        }

        return stringBuilder.toString();
    }

    /**
     * 复制文件夹
     * 
     * @param url 源文件夹
     * @param dest 目标文件夹
     * 默认覆盖 递归
     * @return
     */
    public boolean copyDir(String url, String dest) {
        return copyDir(url, dest, true, true);
    }

    /**
     * 拷贝目录
     * 
     * @param url 源文件夹
     * @param dest 目标文件夹
     * @param overwrite 是否覆盖
     * @param shallow 是否递归
     * @return
     */
    public boolean copyDir(String url, String dest, boolean overwrite, boolean shallow) {
        url = checkUrlAndFullSlash(url);
        dest = checkUrlAndFullSlash(dest);
        return copy(url, dest, overwrite, shallow);
    }

    public boolean copy(String url, String dest) {
        return copy(url, dest, true);
    }

    public boolean copy(String url, String dest, boolean overwrite) {
        return copy(url, dest, overwrite, true);
    }

    public boolean move(String url, String dest) {
        return move(url, dest, true);
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

    /**
     * 上传文件
     * 
     * @param filePath 文件上传后的相对路径
     * @param file 文件
     * @return
     */
    public boolean uploadAutoScope(String filePath, byte[] file) {
        return uploadAutoScope(filePath, file, true);
    }

    /**
     * 自动锁定scope上传
     * 
     * @param filePath 文件上传后的相对路径
     * @param file 文件
     * @return
     */
    public boolean uploadAutoScope(String filePath, byte[] file, boolean created) {
        return upload(StringUtils.EMPTY, filePath, file, created);
    }

    public boolean uploadDefaultScope(String filePath, byte[] file) {
        return uploadDefaultScope(filePath, file, true);
    }

    public boolean uploadDefaultScope(String filePath, byte[] file, boolean created) {
        return upload(webDavConfig.getScope(), filePath, file, created, true);
    }

    public boolean upload(String scope, String filePath, byte[] file) {
        return upload(scope, filePath, file, true);
    }

    public boolean upload(String scope, String filePath, byte[] file, boolean created) {
        return upload(scope, filePath, file, created, true);
    }

    public boolean upload(String filePath, String file) {
        byte[] read = FileTools.read(file);
        if (ObjectUtils.isEmpty(read)) {
            return false;
        }
        return upload(webDavConfig.getScope(), filePath, read, true, true);
    }

    public boolean upload(String scope, String filePath, byte[] file, boolean created, boolean cover) {
        String scopePath = getScopePath(scope, webDavSupport.getBasePath());
        return uploadScopePath(scopePath, filePath, file, created, cover);
    }

    /**
     * 上传文件
     *
     * @param scopePath 项目scope 最终文件 basePath/${scope}/${filePath}
     * @param filePath 文件名【带后缀】
     * @param file 文件地址
     * @return
     */
    public boolean uploadScopePath(String scopePath, String filePath, byte[] file, boolean created, boolean cover) {
        Assert.notNull(scopePath, "scope 不能为空");
        Assert.notNull(filePath, "文件名不能为空");
        Assert.notNull(file, "上传文件不能为空");
        Assert.isTrue(!ObjectUtils.isEmpty(file), "文件不能为空");

        String fileName = FileNameUtil.getName(filePath);
        String directoryPath = StringTools.removeEnd(filePath, fileName);
        List<String> filePaths =
            Splitter.on(StrPoolConstant.SLASH).splitToList(directoryPath).stream().filter(StringUtils::isNoneBlank).collect(Collectors.toList());

        ByteArrayInputStream inputStream = IoUtil.toStream(file);

        // 创建文件夹
        String lastDir = makeDirs(scopePath, filePaths, created);
        if (filePaths.size() > Constant.NUMBER_ONE && !created) {
            return false;
        }
        String absoluteFilePath = lastDir + fileName;

        return upload(absoluteFilePath, inputStream, cover);
    }

    public boolean upload(String absoluteFilePath, byte[] file) {
        return upload(absoluteFilePath, file, true);
    }

    public void download(URL filePath, String localPath) {
        download(webDavConfig.getScope(), filePath.getPath(), localPath, true);
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
        download(filePath, lastLocalPath);
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

    /**
     * 返回所有属性信息
     * 
     * @param url - 网络绝对路径
     * @param dep - 深度
     * @return
     */
    public MultiStatusResult listAllProp(String url, int dep) {
        return list(url, DavConstants.PROPFIND_ALL_PROP, dep);
    }

    /**
     * 返回基础信息
     * 
     * @param url - 网络绝对路径
     * @param dep - 深度
     * @return
     */
    public MultiStatusResult listByProperty(String url, int dep) {
        return list(url, DavConstants.PROPFIND_BY_PROPERTY, dep);
    }

    /**
     * 返回属性名称
     * 
     * @param url - 网络绝对路径
     * @param dep - 深度
     * @return
     */
    public MultiStatusResult listPropfindPropertyNames(String url, int dep) {
        return list(url, DavConstants.PROPFIND_PROPERTY_NAMES, dep);
    }

    /**
     * 返回所有属性信息 RFC 4918, Section 9.1
     * 
     * @param url - 网络绝对路径
     * @param dep - 深度
     * @return
     */
    public MultiStatusResult listPropfindAllPropInclude(String url, int dep) {
        return list(url, DavConstants.PROPFIND_ALL_PROP_INCLUDE, dep);
    }

    /**
     * 文件列表
     *
     * @param url - 网路绝对路径
     * @param dep - 深度 注意这里的深度是指文件夹的深度，不是文件的深度 可选 { 0,1 Integer.MAX_VALUE }
     * 其中 Integer.MAX_VALUE需要开启DavDepthInfinity On
     * {@link <a href="https://www.wenjiangs.com/doc/apache22-mod-mod_dav#9f8b9669ae18a8623329fa0ae85d7410">...</a>}
     * @return - 文件名列表
     */
    public List<String> listFileName(String url, int dep) {
        return Optional.ofNullable(list(url, dep)).map(MultiStatusResult::getMultistatus).map(MultiStatusResult.Multistatus::getResponse)
            .map(e -> e.stream().map(MultiStatusResult.ResponseItem::getHref).collect(Collectors.toList())).orElse(Lists.newArrayList());
    }

    // =============================================以下是基础方法================================================

    public void download(String url, String lastLocalPath) {
        Assert.isTrue(exist(url), "路径不能为空");
        Assert.isTrue(StringUtils.isNotBlank(lastLocalPath), "文件夹不能为空");
        try {
            webDavBaseUtils.download(url, lastLocalPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 绝对路径上传
     * 
     * @param absoluteFilePath - 绝对路径
     * @param file - 文件
     * @param cover - 是否覆盖
     * @return
     */
    public boolean upload(String absoluteFilePath, byte[] file, boolean cover) {
        return upload(absoluteFilePath, IoUtil.toStream(file), cover);
    }

    /**
     * 文件列表
     *
     * @param url - 网路绝对路径
     * @param dep - 深度 注意这里的深度是指文件夹的深度，不是文件的深度 可选 { 0,1 Integer.MAX_VALUE }
     * @return
     */
    public MultiStatusResult list(String url, int dep) {
        url = checkUrlAndFullSlash(url);
        return listByProperty(url, dep);
    }

    /**
     * 文件列表
     * 
     * @param url - 网路绝对路径
     * 默认深度为1
     * @return
     */
    public MultiStatusResult list(String url) {
        url = checkUrlAndFullSlash(url);
        return listByProperty(url, Constant.NUMBER_ONE);
    }

    /**
     * 删除文件
     * 
     * @param url - 网络绝对路径
     * @return
     */
    public boolean delete(String url) {
        checkUrl(url);
        try {
            webDavBaseUtils.delete(url);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String getScopePath(String scope, String basePath) {
        String scopePath = basePath + scope + StrPoolConstant.SLASH;
        if (!exist(scopePath)) {
            if (!mkdir(scopePath)) {
                throw new RuntimeException("mkdir error");
            }
        }
        return scopePath;
    }

    /**
     * 上传文件
     * 
     * @param stream 文件流
     * @param cover 是否覆盖
     * @param absoluteFilePath 带文件名的绝对网路路径
     * @return
     */
    public boolean upload(String absoluteFilePath, InputStream stream, boolean cover) {
        if (exist(absoluteFilePath)) {
            if (cover) {
                delete(absoluteFilePath);
                return upload(absoluteFilePath, stream);
            }
        }
        return upload(absoluteFilePath, stream);
    }

    /**
     * 上传文件
     * 
     * @param url - 网路绝对路径
     * @param fis - 文件流
     * @return
     */
    public boolean upload(String url, InputStream fis) {
        checkUrl(url);
        try {
            webDavBaseUtils.upload(url, fis);
            return exist(url);
        } catch (IOException e) {
            log.error("upload::url = {}", url, e);
            return false;
        }
    }

    /**
     * 文件夹是否存在
     * 
     * @param url
     * @return
     */
    public boolean existDir(String url) {
        return exist(checkUrlAndFullSlash(url));
    }

    /**
     * 路径是否存在
     * 
     * @param url - 网络绝对路径
     * @return
     */
    public boolean exist(String url) {
        checkUrl(url);
        try {
            return webDavJackrabbitUtils.exist(url);
        } catch (Exception e) {
            return false;
        }
    }

    public String lock(String url, Scope scope, Type type, String owner, Long timeout, boolean isDeep) {
        checkUrl(url);
        try {
            webDavJackrabbitUtils.exist(url);
            return webDavJackrabbitUtils.lock(url, scope, type, owner, timeout, isDeep);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String refreshLock(String url, long timeout, String... lockTokens) {
        checkUrl(url);
        try {
            return webDavJackrabbitUtils.refreshLock(url, timeout, lockTokens);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean unLock(String url, String lockToken) {
        checkUrl(url);
        try {
            return webDavJackrabbitUtils.unLock(url, lockToken);
        } catch (Exception e) {
            log.error("unLock::url = {}, lockToken = {} ", url, lockToken, e);
            return false;
        }
    }

    public MultiStatusResult list(String url, int propfindType, int dep) {
        url = checkUrlAndFullSlash(url);
        try {
            return webDavJackrabbitUtils.list(url, propfindType, dep);
        } catch (IOException e) {
            log.error("list::url = {}, propfindType = {}, dep = {} ", url, propfindType, dep, e);
            return null;
        }
    }

    public boolean mkdir(String url) {
        url = checkUrl(url);
        if (!exist(url)) {
            return true;
        }
        try {
            return webDavJackrabbitUtils.mkdir(url);
        } catch (Exception e) {
            log.error("mkdir::url = {} ", url, e);
            return false;
        }
    }

    /**
     * 复制文件
     * 
     * @param url - 网络绝对路径
     * @param dest - 目标网络绝对路径
     * @param overwrite - 是否覆盖
     * @param shallow - 是否浅拷贝
     * @return
     */
    public boolean copy(String url, String dest, boolean overwrite, boolean shallow) {
        url = checkUrl(url);
        dest = checkUrl(dest);
        if (!exist(url)) {
            return false;
        }
        try {
            webDavJackrabbitUtils.copy(url, dest, overwrite, shallow);
            return true;
        } catch (Exception e) {
            log.error("copy::url = {}, dest = {}, overwrite = {}, shallow = {} ", url, dest, overwrite, shallow, e);
            return false;
        }
    }

    /**
     * 移动文件 改名
     * 
     * @param url
     * @param dest
     * @param overwrite
     * @return
     */
    public boolean move(String url, String dest, boolean overwrite) {
        url = checkUrl(url);
        dest = checkUrl(dest);
        try {
            webDavJackrabbitUtils.move(url, dest, overwrite);
            return true;
        } catch (Exception e) {
            log.error("move::url = {} ", url, e);
            return false;
        }
    }

    /**
     * 获取支持的请求方法
     * 
     * @param url
     * @return
     */
    public Set<String> option(String url) {
        Assert.isTrue(StringUtils.isNotBlank(url), "路径不能为空");
        try {
            return webDavJackrabbitUtils.option(url);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取支持的请求方法
     * 
     * @param url - 网络路径
     * @return
     */
    public Set<String> getAllow(String url) {
        Assert.isTrue(StringUtils.isNotBlank(url), "路径不能为空");
        try {
            return webDavJackrabbitUtils.getAllow(url);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 
     * 检查URl 并且补全斜杠
     * 
     * @param url
     * @return
     */
    private static String checkUrlAndFullSlash(String url) {
        url = StringTools.trim(url);
        Assert.isTrue(StringUtils.isNotBlank(url), "路径不能为空");
        if (!url.endsWith(StrPoolConstant.SLASH)) {
            return StringTools.appendIfMissing(url, StrPoolConstant.SLASH);
        }
        return url;
    }

    private static String checkUrl(String url) {
        Assert.isTrue(StringUtils.isNotBlank(url), "路径不能为空");
        return StringTools.trim(url);
    }
}
