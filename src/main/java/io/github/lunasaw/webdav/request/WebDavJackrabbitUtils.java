package io.github.lunasaw.webdav.request;

import com.google.common.base.Splitter;
import com.google.common.collect.Sets;
import com.luna.common.constant.Constant;
import com.luna.common.constant.StrPoolConstant;
import com.luna.common.utils.Assert;
import io.github.lunasaw.webdav.WebDavSupport;
import io.github.lunasaw.webdav.entity.MultiStatusResult;
import io.github.lunasaw.webdav.hander.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.bind.BindInfo;
import org.apache.jackrabbit.webdav.bind.UnbindInfo;
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
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.util.*;

/**
 * @author chenzhangyue
 * @description jackrabbit webdav 工具类 {@link org.apache.jackrabbit.webdav.client.methods} 请求
 * @date 2023/4/4
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

    public boolean move(String url, String dest, boolean overwrite) throws IOException {
        HttpMove httpMove = new HttpMove(url, dest, overwrite);
        return webDavSupport.execute(httpMove, new BooleanResponseHandler(httpMove));
    }

    /**
     * 创建文件夹
     *
     * @param url 路径
     * @return
     */
    public boolean mkdir(String url) throws IOException {
        HttpMkcol mkcol = new HttpMkcol(url);
        return webDavSupport.execute(mkcol, new BooleanResponseHandler(mkcol));
    }

    /**
     * 判断文件或者文件夹是否存在
     *
     * @param url 路径
     * @return
     */
    public boolean exist(String url) throws IOException {
        MultiStatusResult list = list(url, DavConstants.PROPFIND_BY_PROPERTY, Constant.NUMBER_ONE);
        return Optional.ofNullable(list.getMultiStatus()).map(MultiStatusResult.Multistatus::getResponse).map(CollectionUtils::isNotEmpty)
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
        return webDavSupport.execute(httpProppatch, new BooleanResponseHandler(httpProppatch));
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
        webDavSupport.execute(httpCopy, new VoidResponseHandler());
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
        return webDavSupport.execute(lockInfo, new BooleanResponseHandler(lockInfo));
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
        return webDavSupport.execute(httpDelete, new BooleanResponseHandler(httpDelete));
    }

    public boolean update(String url, String[] updateSource, int updateType, DavPropertyName... davPropertyName) throws IOException {
        DavPropertyNameSet davPropertyNames = new DavPropertyNameSet();
        for (DavPropertyName propertyName : davPropertyName) {
            davPropertyNames.add(propertyName);
        }
        UpdateInfo updateInfo = new UpdateInfo(updateSource, updateType, davPropertyNames);
        HttpUpdate httpUpdate = new HttpUpdate(url, updateInfo);
        return webDavSupport.execute(httpUpdate, new BooleanResponseHandler(httpUpdate));
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
        return webDavSupport.execute(httpLabel, new BooleanResponseHandler(httpLabel));
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
        return webDavSupport.execute(httpReport, new BooleanResponseHandler(httpReport));
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
        return webDavSupport.execute(httpSubscribe, new BooleanResponseHandler(httpSubscribe));
    }

    public boolean unSubScribe(String url, String subscriptionId) throws IOException {
        HttpUnsubscribe unsubscribe = new HttpUnsubscribe(url, subscriptionId);
        return webDavSupport.execute(unsubscribe, new BooleanResponseHandler(unsubscribe));
    }

    /**
     * 创建虚拟工作区 #checkIn #checkOout
     * 
     * @see <a href="http://webdav.org/specs/rfc3253.html#rfc.section.6.3">RFC 3253, Section 6.3</a>
     * @param url
     * @return
     * @throws IOException
     */
    public boolean mkworkspace(String url) throws IOException {
        HttpMkworkspace mkworkspace = new HttpMkworkspace(url);
        return webDavSupport.execute(mkworkspace, new BooleanResponseHandler(mkworkspace));
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
        return webDavSupport.execute(versionControl, new BooleanResponseHandler(versionControl));
    }

    /**
     * BIND 方法通过添加从 BIND 主体中指定的段到 BIND 主体中标识的资源的新绑定来修改 Request-URI 标识的集合
     * 
     * @param url - 网络路径
     * @param href - 资源路径 相对路径
     * @param segment - 资源名称
     * @see <a href="http://webdav.org/specs/rfc5842.html#rfc.section.4">RFC 5842, Section 4</a>*
     * @return
     * @throws IOException
     */
    public boolean bind(String url, String href, String segment) throws IOException {
        BindInfo bindInfo = new BindInfo(href, segment);
        HttpBind httpBind = new HttpBind(url, bindInfo);
        return webDavSupport.execute(httpBind, new BooleanResponseHandler(httpBind));
    }

    /**
     * UNBIND 方法通过从 Request-URI 标识的集合中删除指定的段来修改 Request-URI 标识的集合
     * 
     * @param url - 网络路径
     * @param segment - 资源名称
     * @return
     * @throws IOException
     */
    public boolean unBind(String url, String segment) throws IOException {
        UnbindInfo unbindInfo = new UnbindInfo(segment);
        HttpUnbind httpBind = new HttpUnbind(url, unbindInfo);
        return webDavSupport.execute(httpBind, new BooleanResponseHandler(httpBind));
    }
}
