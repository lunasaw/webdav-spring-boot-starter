package io.github.lunasaw.webdav.entity;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.Data;

import java.util.List;

/**
 * @author weidian
 */
@Data
public class PropResult {

    @JSONField(name = "D:prop")
    private Prop prop;

    @Data
    public static class Prop {

        @JSONField(name = "D:lockdiscovery")
        private Lockdiscovery   lockdiscovery;

        @JSONField(name = "xmlns:D")
        private String          xmlnsD;

        @JSONField(name = "lp1:creationdate")
        private String          creationDate;

        @JSONField(name = "lp1:resourcetype")
        private Lp1Resourcetype resourceType;

        @JSONField(name = "D:supportedlock")
        private Supportedlock   supportedLock;

        @JSONField(name = "lp1:getetag")
        private String          geteTag;

        @JSONField(name = "lp1:getlastmodified")
        private String          getLastModified;

        @JSONField(name = "lp1:getcontentlength")
        private String          getContentLength;

        @JSONField(name = "lp2:executable")
        private String          executable;

        @JSONField(name = "D:getcontenttype")
        private String          getContentType;

        @JSONField(name = "D:lockdiscovery")
        private String          lockDiscovery;
    }

    @Data
    public static class Lockdiscovery {

        @JSONField(name = "D:activelock")
        private ActiveLock activelock;
    }

    @Data
    public static class ActiveLock {

        @JSONField(name = "ns0:owner")
        private Ns0Owner  ns0Owner;

        @JSONField(name = "D:locktoken")
        private Locktoken locktoken;

        @JSONField(name = "D:locktype")
        private Locktype  locktype;

        @JSONField(name = "D:lockscope")
        private Lockscope lockscope;

        @JSONField(name = "D:timeout")
        private String    timeout;

        @JSONField(name = "D:depth")
        private String    depth;
    }

    @Data
    public static class Lockscope {

        @JSONField(name = "D:exclusive")
        private String exclusive;


        @JSONField(name = "D:shared")
        private String shared;
    }

    @Data
    public static class Locktoken {

        @JSONField(name = "D:href")
        private List<String> href;
    }

    @Data
    public static class Locktype {

        @JSONField(name = "D:write")
        private String write;
    }

    @Data
    public class Ns0Owner {

        @JSONField(name = "xmlns:ns0")
        private String xmlnsNs0;
    }

    @Data
    public class Supportedlock {

        @JSONField(name = "D:lockentry")
        private List<LockentryItem> lockentry;
    }

    @Data
    public class Lp1Resourcetype {

        @JSONField(name = "D:collection")
        private String collection;
    }

    @Data
    public class LockentryItem {

        @JSONField(name = "D:lockscope")
        private Lockscope dLockscope;

        @JSONField(name = "D:locktype")
        private Locktype  dLocktype;
    }
}