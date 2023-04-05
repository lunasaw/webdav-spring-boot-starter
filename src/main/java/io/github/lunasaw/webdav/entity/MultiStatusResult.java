package io.github.lunasaw.webdav.entity;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.Data;

import java.util.List;

/**
 * @author luna
 * @description
 * @date 2023/4/5
 */
@Data
public class MultiStatusResult {

    @JSONField(name = "D:multistatus")
    private Multistatus multistatus;

    @Data
    public static class Multistatus {

        @JSONField(name = "xmlns:D")
        private String             xmlnsD;

        @JSONField(name = "xmlns:ns0")
        private String             xmlnsNs0;

        @JSONField(name = "D:response")
        private List<ResponseItem> response;

    }

    @Data
    public static class ResponseItem {

        @JSONField(name = "D:href")
        private String   href;

        @JSONField(name = "D:propstat")
        private Propstat propstat;

    }

    @Data
    public static class Propstat {

        @JSONField(name = "D:prop")
        private String dProp;

        @JSONField(name = "D:status")
        private String dStatus;
    }

}
