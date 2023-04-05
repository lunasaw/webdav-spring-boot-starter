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
import io.github.lunasaw.webdav.properties.WebDavConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
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
    private WebDavBaseUtils webDavBaseUtils;

    @Autowired
    private WebDavConfig webDavConfig;

    @Autowired
    private WebDavSupport webDavSupport;

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
        if (!webDavJackrabbitUtils.exist(url) && !created) {
            return false;
        }
        return webDavBaseUtils.upload(url, IoUtil.toStream(file));
    }

    public boolean upload(String filePath, String file) {
        byte[] read = FileTools.read(file);
        if (ObjectUtils.isEmpty(read)){
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
        if (!webDavJackrabbitUtils.exist(scopePath)) {
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
        if (webDavJackrabbitUtils.exist(absoluteFilePath)) {
            if (cover) {
                webDavBaseUtils.delete(absoluteFilePath);
                return webDavBaseUtils.upload(absoluteFilePath, inputStream);
            } else {
                return false;
            }
        }
        return webDavBaseUtils.upload(absoluteFilePath, inputStream);

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

        Assert.isTrue(webDavJackrabbitUtils.exist(filePath), "网络文件路径不能为空");
        webDavBaseUtils.downloadUrl(filePath, lastLocalPath);
    }

}
