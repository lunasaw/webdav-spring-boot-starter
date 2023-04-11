package io.github.lunasaw.webdav.entity;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.Data;

import java.util.List;

/**
 * @author weidian
 * @description 直接序列化返回值
 */
@Data
public class PropResult {

    @JSONField(name = "D:prop")
    private Prop prop;

    @Data
    public static class Prop {

        @JSONField(name = "D:lockdiscovery")
        private Lockdiscovery lockdiscovery;

        @JSONField(name = "xmlns:D")
        private String        xmlnsD;

        @JSONField(name = "lp1:creationdate")
        private String        creationDate;

        @JSONField(name = "lp1:resourcetype")
        private ResourceType  resourceType;

        @JSONField(name = "D:supportedlock")
        private SupportedLock supportedLock;

        @JSONField(name = "lp1:getetag")
        private String        getTag;

        @JSONField(name = "lp1:getlastmodified")
        private String        getLastModified;

        @JSONField(name = "lp1:getcontentlength")
        private String        getContentLength;

        @JSONField(name = "lp2:executable")
        private String        executable;

        @JSONField(name = "D:getcontenttype")
        private String        getContentType;

        @JSONField(name = "D:lockdiscovery")
        private String        lockDiscovery;
    }

    @Data
    public static class Lockdiscovery {

        @JSONField(name = "D:activelock")
        private ActiveLock activeLock;
    }

    @Data
    public static class ActiveLock {

        @JSONField(name = "ns0:owner")
        private NsOwner   nsOwner;

        @JSONField(name = "D:locktoken")
        private LockToken locktoken;

        @JSONField(name = "D:locktype")
        private Locktype  lockType;

        @JSONField(name = "D:lockscope")
        private LockScope lockScope;

        @JSONField(name = "D:timeout")
        private String    timeout;

        @JSONField(name = "D:depth")
        private String    depth;
    }

    @Data
    public static class LockScope {

        @JSONField(name = "D:exclusive")
        private String exclusive;

        @JSONField(name = "D:shared")
        private String shared;
    }

    @Data
    public static class LockToken {

        @JSONField(name = "D:href")
        private List<String> href;
    }

    @Data
    public static class Locktype {

        @JSONField(name = "D:write")
        private String write;
    }

    @Data
    public class NsOwner {

        @JSONField(name = "xmlns:ns0")
        private String xmlnsNs0;
    }

    @Data
    public class SupportedLock {

        @JSONField(name = "D:lockentry")
        private List<LockEntryItem> lockEntry;
    }

    @Data
    public class ResourceType {

        @JSONField(name = "D:collection")
        private String collection;
    }

    @Data
    public class LockEntryItem {

        @JSONField(name = "D:lockscope")
        private LockScope lockScope;

        @JSONField(name = "D:locktype")
        private Locktype  locktype;
    }
}