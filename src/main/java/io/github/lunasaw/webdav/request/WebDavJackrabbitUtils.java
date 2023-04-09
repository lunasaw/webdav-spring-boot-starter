package io.github.lunasaw.webdav.request;

import ch.qos.logback.core.joran.spi.XMLUtil;
import com.alibaba.fastjson2.JSON;
import com.google.common.base.Splitter;
import com.google.common.collect.Sets;
import com.luna.common.constant.Constant;
import com.luna.common.constant.StrPoolConstant;
import com.luna.common.utils.Assert;
import io.github.lunasaw.webdav.WebDavSupport;
import io.github.lunasaw.webdav.entity.MultiStatusResult;
import io.github.lunasaw.webdav.hander.LockResponseHandler;
import io.github.lunasaw.webdav.hander.MultiStatusHandler;
import io.github.lunasaw.webdav.hander.ValidatingResponseHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.client.methods.*;
import org.apache.jackrabbit.webdav.lock.LockInfo;
import org.apache.jackrabbit.webdav.lock.Scope;
import org.apache.jackrabbit.webdav.lock.Type;
import org.apache.jackrabbit.webdav.observation.EventType;
import org.apache.jackrabbit.webdav.observation.Filter;
import org.apache.jackrabbit.webdav.observation.SubscriptionInfo;
import org.apache.jackrabbit.webdav.property.DavProperty;
import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.apache.jackrabbit.webdav.property.DavPropertyNameSet;
import org.apache.jackrabbit.webdav.property.DavPropertySet;
import org.apache.jackrabbit.webdav.search.SearchInfo;
import org.apache.jackrabbit.webdav.version.LabelInfo;
import org.apache.jackrabbit.webdav.version.UpdateInfo;
import org.apache.jackrabbit.webdav.version.report.ReportInfo;
import org.apache.jackrabbit.webdav.version.report.ReportType;
import org.apache.jackrabbit.webdav.xml.Namespace;
import org.json.JSONObject;
import org.json.XML;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;

import javax.xml.ws.Response;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;

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
        exist(webDavSupport.getBasePath());
    }

    public void move(String url, String dest, boolean overwrite) throws IOException {
        HttpMove httpMove = new HttpMove(url, dest, overwrite);
        webDavSupport.execute(httpMove, new ValidatingResponseHandler<Void>() {
            @Override
            public Void handleResponse(HttpResponse httpResponse) {
                this.validateResponse(httpResponse);
                return null;
            }
        });
    }

    /**
     * 创建文件夹
     *
     * @param url 路径
     * @return
     */
    public boolean mkdir(String url) throws IOException {
        HttpMkcol mkcol = new HttpMkcol(url);
        HttpResponse response = webDavSupport.execute(mkcol, new ValidatingResponseHandler<HttpResponse>() {
            @Override
            public HttpResponse handleResponse(HttpResponse httpResponse) {
                this.validateResponse(httpResponse);
                return httpResponse;
            }
        });
        return mkcol.succeeded(response);
    }

    /**
     * 判断文件或者文件夹是否存在
     *
     * @param url 路径
     * @return
     */
    public boolean exist(String url) throws IOException {
        MultiStatusResult list = list(url, DavConstants.PROPFIND_BY_PROPERTY, Constant.NUMBER_ONE);
        return Optional.ofNullable(list.getMultistatus()).map(MultiStatusResult.Multistatus::getResponse).map(CollectionUtils::isNotEmpty)
            .orElse(false);
    }

    /**
     * 判断文件或者文件夹是否存在
     * 
     * @param url 网络路径
     * @param propfindType
     * @param dep {@link org.apache.jackrabbit.webdav.header.DepthHeader}
     * {@link DavConstants}
     * @return
     */
    public MultiStatusResult list(String url, int propfindType, int dep) throws IOException {
        HttpPropfind propfind = new HttpPropfind(url, propfindType, dep);
        return webDavSupport.execute(propfind, new MultiStatusHandler());
    }

    /**
     * 设置属性
     *
     * @param url - 网络路径
     * @param propertySet - 设置属性
     * @param removeProperties - 移除属性
     * @return
     * @throws IOException
     */
    public boolean proppatch(String url, Set<DavProperty<?>> propertySet, Set<DavPropertyName> removeProperties) throws IOException {
        DavPropertySet setProperties = new DavPropertySet();
        for (DavProperty<?> davProperty : propertySet) {
            setProperties.add(davProperty);
        }
        DavPropertyNameSet removePropertyNameSet = new DavPropertyNameSet();
        for (DavPropertyName davPropertyName : removeProperties) {
            removePropertyNameSet.add(davPropertyName);
        }
        return proppatch(url, setProperties, removePropertyNameSet);
    }

    /**
     * 设置属性
     * 
     * @param url - 网络路径
     * @param setProperties - 设置属性
     * @param removeProperties - 移除属性
     * @return
     * @throws IOException
     */
    public boolean proppatch(String url, DavPropertySet setProperties, DavPropertyNameSet removeProperties) throws IOException {
        HttpProppatch httpProppatch = new HttpProppatch(url, setProperties, removeProperties);
        return webDavSupport.execute(httpProppatch, new ValidatingResponseHandler<Boolean>() {
            @Override
            public Boolean handleResponse(HttpResponse httpResponse) throws ClientProtocolException, IOException {
                this.validateResponse(httpResponse);
                return httpProppatch.succeeded(httpResponse);
            }
        });
    }

    /**
     * 获取支持的请求方法
     * 
     * @param url - 网络路径
     * @return
     */
    public Set<String> getAllow(String url) throws IOException {
        HttpOptions httpOptions = new HttpOptions(url);
        HttpResponse response = webDavSupport.execute(httpOptions);
        Header[] allHeaders = response.getHeaders("Allow");
        String allowMethod = Arrays.stream(allHeaders).findFirst().map(Header::getValue).orElse(StringUtils.EMPTY);
        List<String> list = Splitter.on(StrPoolConstant.COMMA).splitToList(allowMethod);
        return Sets.newHashSet(list);

    }

    /**
     * 获取支持的请求方法
     * 
     * @param url - 网络路径
     * @return
     */
    public Set<String> option(String url) throws IOException {
        HttpOptions httpOptions = new HttpOptions(url);
        HttpResponse response = webDavSupport.execute(httpOptions);
        Set<String> searchGrammars = httpOptions.getDavComplianceClasses(response);
        return searchGrammars;
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
    public void copy(String url, String dest, boolean overwrite, boolean shallow) throws IOException {
        HttpCopy httpCopy = new HttpCopy(url, dest, overwrite, shallow);
        webDavSupport.execute(httpCopy, new ValidatingResponseHandler<Void>() {
            @Override
            public Void handleResponse(HttpResponse httpResponse) throws IOException {
                this.validateResponse(httpResponse);
                return null;
            }
        });
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
            HttpResponse response = webDavSupport.execute(httpCheckout);
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
            HttpResponse response = webDavSupport.execute(checkin);
            return checkin.succeeded(response);
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
    public String refreshLock(String url, long timeout, String... lockTokens) throws IOException {
        HttpLock httpLock = new HttpLock(url, timeout, lockTokens);
        return webDavSupport.execute(httpLock, new LockResponseHandler());
    }

    /**
     * @param url 路径
     * @param scope 锁定类型 {@link Scope}
     * @param type WRITE：表示只有写访问权限的锁定。
     * @param owner
     * @param timeout 超时时间
     * @param isDeep
     * @return
     */
    public String lock(String url, Scope scope, Type type, String owner, Long timeout, boolean isDeep) throws IOException {
        Assert.isTrue(StringUtils.isNotBlank(url), "路径不能为空");
        LockInfo lockInfo = new LockInfo(scope, type, owner, timeout, isDeep);
        HttpLock httpLock = new HttpLock(url, lockInfo);
        return webDavSupport.execute(httpLock, new LockResponseHandler());
    }

    /**
     * 解锁
     * 
     * @param url
     * @param lockToken
     * @return
     */
    public boolean unLock(String url, String lockToken) throws IOException {
        HttpUnlock lockInfo = new HttpUnlock(url, lockToken);
        return webDavSupport.execute(lockInfo, new ValidatingResponseHandler<Boolean>() {
            @Override
            public Boolean handleResponse(HttpResponse httpResponse) throws IOException {
                this.validateResponse(httpResponse);
                return lockInfo.succeeded(httpResponse);
            }
        });
    }

    /**
     * 搜索文件
     * 
     * @param url - 网络路径
     * @param language - 查询语言
     * @param languageNamespace - 查询语言命名空间
     * @param query - 查询语句
     * @param namespaces - 命名空间
     * @return
     */
    public MultiStatusResult search(String url, String language, Namespace languageNamespace, String query,
        Map<String, String> namespaces) throws IOException {
        SearchInfo searchInfo = new SearchInfo(language, languageNamespace, query, namespaces);
        HttpSearch httpSearch = new HttpSearch(url, searchInfo);
        MultiStatusResult execute = webDavSupport.execute(httpSearch, new MultiStatusHandler());
        return execute;
    }

    /**
     * 删除文件
     *
     * @param url
     * @return
     */
    public boolean delete(String url) throws IOException {
        HttpDelete httpDelete = new HttpDelete(url);
        return webDavSupport.execute(httpDelete, new ValidatingResponseHandler<Boolean>() {
            @Override
            public Boolean handleResponse(HttpResponse httpResponse) {
                this.validateResponse(httpResponse);
                return httpDelete.succeeded(httpResponse);
            }
        });
    }

    public boolean update(String url, String[] updateSource, int updateType, DavPropertyName... davPropertyName) throws IOException {
        DavPropertyNameSet davPropertyNames = new DavPropertyNameSet();
        for (DavPropertyName propertyName : davPropertyName) {
            davPropertyNames.add(propertyName);
        }
        UpdateInfo updateInfo = new UpdateInfo(updateSource, updateType, davPropertyNames);
        HttpUpdate httpUpdate = new HttpUpdate(url, updateInfo);
        return webDavSupport.execute(httpUpdate, new ValidatingResponseHandler<Boolean>() {
            @Override
            public Boolean handleResponse(HttpResponse httpResponse) {
                this.validateResponse(httpResponse);
                return httpUpdate.succeeded(httpResponse);
            }
        });
    }

    /**
     * 为资源打标签
     * 
     * @param url 资源路径
     * @param labelName 标签名称
     * @param type 标签类型
     * TYPE_SET = 0;
     * TYPE_REMOVE = 1;
     * TYPE_ADD = 2;
     * @param depth 0：只对当前资源打标签 1：对当前资源和子资源打标签
     * @return
     * @throws IOException
     */
    public boolean lable(String url, String labelName, int type, int depth) throws IOException {
        LabelInfo labelInfo = new LabelInfo(labelName, type, depth);
        HttpLabel httpLabel = new HttpLabel(url, labelInfo);
        return webDavSupport.execute(httpLabel, new ValidatingResponseHandler<Boolean>() {
            @Override
            public Boolean handleResponse(HttpResponse httpResponse) {
                this.validateResponse(httpResponse);
                return httpLabel.succeeded(httpResponse);
            }
        });
    }

    /**
     * 报告
     * 
     * @param url 资源路径
     * @param typelocalName 报告类型 {@link ReportType}
     * @param typeNamespace
     * @param depth
     * @param davPropertyName
     * @return
     * @throws IOException
     */
    public boolean report(String url, String typelocalName, Namespace typeNamespace, int depth, DavPropertyName... davPropertyName)
        throws IOException {
        DavPropertyNameSet propertyNames = new DavPropertyNameSet();
        for (DavPropertyName propertyName : davPropertyName) {
            propertyNames.add(propertyName);
        }
        ReportInfo reportInfo = new ReportInfo(typelocalName, typeNamespace, depth, propertyNames);
        HttpReport httpReport = new HttpReport(url, reportInfo);
        return webDavSupport.execute(httpReport, new ValidatingResponseHandler<Boolean>() {
            @Override
            public Boolean handleResponse(HttpResponse httpResponse) {
                this.validateResponse(httpResponse);
                return httpReport.succeeded(httpResponse);
            }
        });
    }

    /**
     * 订阅
     * 
     * @param url - 网络路径
     * @param eventTypes - 事件类型
     * @param filters - 过滤器
     * @param noLocal - 是否本地
     * @param isDeep - 是否深度
     * @param timeout - 超时时间
     * @param subscriptionId - 订阅ID
     * @return
     * @throws IOException
     */
    public boolean subScribe(String url, EventType[] eventTypes, Filter[] filters, boolean noLocal, boolean isDeep, long timeout,
        String subscriptionId) throws IOException {
        SubscriptionInfo subscriptionInfo = new SubscriptionInfo(eventTypes, filters, noLocal, isDeep, timeout);
        return subScribe(url, subscriptionInfo, subscriptionId);
    }

    /**
     * 订阅
     *
     * @param url
     * @return
     */
    public boolean subScribe(String url, SubscriptionInfo info, String subscriptionId) throws IOException {
        HttpSubscribe httpSubscribe = new HttpSubscribe(url, info, subscriptionId);
        return webDavSupport.execute(httpSubscribe, new ValidatingResponseHandler<Boolean>() {
            @Override
            public Boolean handleResponse(HttpResponse httpResponse) {
                this.validateResponse(httpResponse);
                return httpSubscribe.succeeded(httpResponse);
            }
        });
    }

    public boolean unSubScribe(String url, String subscriptionId) throws IOException {
        HttpUnsubscribe unsubscribe = new HttpUnsubscribe(url, subscriptionId);
        return webDavSupport.execute(unsubscribe, new ValidatingResponseHandler<Boolean>() {
            @Override
            public Boolean handleResponse(HttpResponse httpResponse) {
                this.validateResponse(httpResponse);
                return unsubscribe.succeeded(httpResponse);
            }
        });
    }

    /**
     * 创建虚拟工作区
     * 
     * @see <a href="http://webdav.org/specs/rfc3253.html#rfc.section.6.3">RFC 3253, Section 6.3</a>
     * @param url
     * @return
     * @throws IOException
     */
    public boolean mkworkspace(String url) throws IOException {
        HttpMkworkspace mkworkspace = new HttpMkworkspace(url);
        return webDavSupport.execute(mkworkspace, new ValidatingResponseHandler<Boolean>() {
            @Override
            public Boolean handleResponse(HttpResponse httpResponse) throws IOException {
                this.validateResponse(httpResponse);
                return mkworkspace.succeeded(httpResponse);
            }
        });
    }

    /**
     * 版本控制
     * 
     * @see <a href="http://webdav.org/specs/rfc3253.html#rfc.section.3.5">RFC 3253, Section 3.5</a>
     * @param url
     * @return
     * @throws IOException
     */
    public boolean versionControl(String url) throws IOException {
        HttpVersionControl versionControl = new HttpVersionControl(url);
        return webDavSupport.execute(versionControl, new ValidatingResponseHandler<Boolean>() {
            @Override
            public Boolean handleResponse(HttpResponse httpResponse) throws IOException {
                this.validateResponse(httpResponse);
                return versionControl.succeeded(httpResponse);
            }
        });
    }
}
