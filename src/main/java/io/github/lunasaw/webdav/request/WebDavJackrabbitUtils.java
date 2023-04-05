package io.github.lunasaw.webdav.request;

import ch.qos.logback.core.joran.spi.XMLUtil;
import com.alibaba.fastjson2.JSON;
import com.luna.common.constant.Constant;
import com.luna.common.constant.StrPoolConstant;
import com.luna.common.utils.Assert;
import com.luna.common.xml.XmlUtil;
import io.github.lunasaw.webdav.WebDavSupport;
import io.github.lunasaw.webdav.entity.MultiStatusResult;
import io.github.lunasaw.webdav.hander.LockResponseHandler;
import io.github.lunasaw.webdav.hander.MultiStatusHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.client.methods.*;
import org.apache.jackrabbit.webdav.lock.LockInfo;
import org.apache.jackrabbit.webdav.lock.Scope;
import org.apache.jackrabbit.webdav.lock.Type;
import org.json.JSONObject;
import org.json.XML;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;

import javax.xml.ws.Response;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * @author chenzhangyue
 * 2023/4/4
 */
@Component
@Slf4j
public class WebDavJackrabbitUtils implements InitializingBean {

    @Autowired
    private WebDavSupport webDavSupport;

    @Override
    public void afterPropertiesSet() throws Exception {
        boolean exist = exist(webDavSupport.getBasePath());
        Assert.isTrue(exist, "路径不存在");
    }

    public boolean move(String url, String dest, boolean overwrite) {
        Assert.isTrue(StringUtils.isNotBlank(url), "路径不能为空");
        try {
            HttpMove httpMove = new HttpMove(url, dest, overwrite);
            HttpResponse httpResponse = webDavSupport.getClient().execute(httpMove);
            return httpMove.succeeded(httpResponse);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

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
                makeDir(stringBuilder.toString());
            }
        }

        return stringBuilder.toString();
    }

    public boolean makeDir(String url) {
        return mkdir(url);
    }

    /**
     * 创建文件夹
     *
     * @param url 路径
     * @return
     */
    public boolean mkdir(String url) {
        Assert.isTrue(StringUtils.isNotBlank(url), "路径不能为空");
        try {
            HttpMkcol mkcol = new HttpMkcol(url);
            HttpResponse response = webDavSupport.executeWithContext(mkcol);
            boolean succeeded = mkcol.succeeded(response);
            if (!succeeded) {
                log.warn("mkdir::url = {}, response = {}", url, JSON.toJSON(response));
            }
            return succeeded;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public MultiStatusResult list(String url) {
        MultiStatusResult list = list(url, DavConstants.PROPFIND_ALL_PROP, Constant.NUMBER_ONE);
        return list;
    }

    /**
     * 判断文件或者文件夹是否存在
     *
     * @param url 路径
     * @return
     */
    public boolean exist(String url) {
        MultiStatusResult list = list(url, DavConstants.PROPFIND_BY_PROPERTY, Constant.NUMBER_ONE);
        return Optional.ofNullable(list.getMultistatus()).map(MultiStatusResult.Multistatus::getResponse).map(CollectionUtils::isNotEmpty)
            .orElse(false);
    }

    /**
     * 判断文件或者文件夹是否存在
     *
     * @param url 网络路径
     * @return
     */
    public MultiStatusResult list(String url, int propfindType, int dep) {
        Assert.isTrue(StringUtils.isNotBlank(url), "路径不能为空");
        try {
            HttpPropfind propfind = new HttpPropfind(url, propfindType, dep);
            return webDavSupport.execute(propfind, new MultiStatusHandler());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Set<String> getSearchGrammars(String url) {
        Assert.isTrue(StringUtils.isNotBlank(url), "路径不能为空");
        try {
            HttpOptions httpOptions = new HttpOptions(url);
            HttpResponse response = webDavSupport.executeWithContext(httpOptions);
            Set<String> searchGrammars = httpOptions.getSearchGrammars(response);
            return searchGrammars;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Set<String> getDavComplianceClasses(String url) {
        Assert.isTrue(StringUtils.isNotBlank(url), "路径不能为空");
        try {
            HttpOptions httpOptions = new HttpOptions(url);
            HttpResponse response = webDavSupport.executeWithContext(httpOptions);
            Set<String> searchGrammars = httpOptions.getDavComplianceClasses(response);
            return searchGrammars;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 文件拷贝
     *
     * @param url 原始地址
     * @param dest 目的地址
     * @param overwrite 是否覆盖
     * @param shallow 是否递归地复制所有子资源，包括子目录和它们的子项。 true 浅复制 false 深复制
     * @return
     */
    public boolean copy(String url, String dest, boolean overwrite, boolean shallow) {
        Assert.isTrue(StringUtils.isNotBlank(url), "路径不能为空");
        try {
            HttpCopy httpCopy = new HttpCopy(url, dest, overwrite, shallow);
            HttpResponse response = webDavSupport.executeWithContext(httpCopy);
            return httpCopy.succeeded(response);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 给资源加锁
     *
     * @param url 原始地址
     * @return
     */
    public boolean checkOut(String url) {
        Assert.isTrue(StringUtils.isNotBlank(url), "路径不能为空");
        try {
            HttpCheckout httpCheckout = new HttpCheckout(url);
            HttpResponse response = webDavSupport.executeWithContext(httpCheckout);
            return httpCheckout.succeeded(response);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 给资源解锁
     *
     * @param url 原始地址
     * @return
     */
    public boolean checkIn(String url) {
        Assert.isTrue(StringUtils.isNotBlank(url), "路径不能为空");
        try {
            HttpCheckin checkin = new HttpCheckin(url);
            HttpResponse response = webDavSupport.executeWithContext(checkin);
            return checkin.succeeded(response);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
     * @param url 路径
     * @param scope 锁定类型 {@link Scope}
     * @param type WRITE：表示只有写访问权限的锁定。
     * @param owner
     * @param timeout
     * @param isDeep
     * @return
     */
    public String lock(String url, Scope scope, Type type, String owner, Long timeout, boolean isDeep) {
        Assert.isTrue(StringUtils.isNotBlank(url), "路径不能为空");
        try {
            LockInfo lockInfo = new LockInfo(scope, type, owner, timeout, isDeep);
            HttpLock httpLock = new HttpLock(url, lockInfo);
            String response = webDavSupport.execute(httpLock, new LockResponseHandler());

            return response;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 续锁
     *
     * @param url 路径
     * @param timeout 超时
     * @param lockTokens 上次一次的token
     * @return
     */
    public String refreshLock(String url, long timeout, String... lockTokens) {
        Assert.isTrue(StringUtils.isNotBlank(url), "路径不能为空");
        try {
            HttpLock httpLock = new HttpLock(url, timeout, lockTokens);
            HttpResponse response = webDavSupport.executeWithContext(httpLock);
            return httpLock.getLockToken(response);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean unLock(String url, String lockToken) {
        Assert.isTrue(StringUtils.isNotBlank(url), "路径不能为空");
        try {
            HttpUnlock lockInfo = new HttpUnlock(url, lockToken);
            HttpResponse response = webDavSupport.executeWithContext(lockInfo);
            return lockInfo.succeeded(response);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
