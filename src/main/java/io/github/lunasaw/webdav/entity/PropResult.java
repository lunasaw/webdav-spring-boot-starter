package io.github.lunasaw.webdav.entity;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.Data;

import java.util.List;

/**
 * @author weidian
 */
@Data
public class PropResult{

    @JSONField(name = "D:prop")
    private Prop prop;

    @Data
    public static class Prop {

        @JSONField(name = "D:lockdiscovery")
        private Lockdiscovery lockdiscovery;

        @JSONField(name = "xmlns:D")
        private String        xmlnsD;
    }

    @Data
    public static class Lockdiscovery {

        @JSONField(name = "D:activelock")
        private Activelock activelock;
    }

    @Data
    public static class Activelock {

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
        private String dExclusive;
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
}