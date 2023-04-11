package io.github.lunasaw.webdav.entity;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import com.alibaba.fastjson2.annotation.JSONField;
import com.luna.common.constant.CharPoolConstant;
import com.luna.common.constant.StrPoolConstant;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * @author weidian
 * @description 带解析的返回值
 */
@Data
@Slf4j
public class ResponseResult {

    private ResultStatus multiStatus;

    @Data
    public static class ResultStatus {
        private String             xmlnsD;
        private List<ResponseItem> response;
        private String             xmlnsNs0;
    }

    @Data
    public static class LockScope {
        private String shared;
        private String exclusive;
    }

    @Data
    public static class LockEntryItem {
        private LockScope lockScope;
        private LockType  locktype;
    }

    @Data
    public static class LockType {
        private String write;
    }

    @Data
    public static class PropStat {
        private Prop   prop;
        private String dStatus;
    }

    @Data
    public static class ResourceType {
        private String collection;
    }

    @Data
    public static class ResponseItem {
        private String   xmlnsLp2;
        private String   xmlnsLp1;
        private PropStat propstat;
        private String   href;

        public String getName() {
            String path = this.href;
            try {
                if (path.endsWith(StrPoolConstant.SLASH)) {
                    path = path.substring(0, path.length() - 1);
                }
                return path.substring(path.lastIndexOf(CharPoolConstant.SLASH) + 1);
            } catch (StringIndexOutOfBoundsException e) {
                log.warn(String.format("Failed to parse name from path %s", path));
                return null;
            }
        }
    }

    @Data
    public static class SupportedLock {
        private List<LockEntryItem> lockEntry;
    }

    @Data
    public static class Prop {

        public static final String HTTPD_UNIX_DIRECTORY_CONTENT_TYPE = "httpd/unix-directory";

        private Long               getLastModified;
        private SupportedLock      supportedLock;
        private String             getContentType;
        private Long               creationDate;
        private String             getTag;
        private Long               getContentLength;
        private Boolean            executable;
        private ResourceType       resourceType;

        @JSONField(name = "isDirectory")
        public boolean isDirectory() {
            return HTTPD_UNIX_DIRECTORY_CONTENT_TYPE.equals(this.getContentType);
        }

        public void setGetContentLength(String getContentLength) {
            this.getContentLength = Long.valueOf(getContentLength);
        }

        public void setExecutable(String executable) {
            this.executable = !"F".equals(executable);
        }

        public void setCreationDate(String creationDate) {
            Instant instant = Instant.parse(creationDate);
            this.creationDate = instant.toEpochMilli();
        }

        public void setGetLastModified(String getLastModified) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH);
            ZonedDateTime zonedDateTime = ZonedDateTime.parse(getLastModified, formatter);
            this.getLastModified = zonedDateTime.toInstant().toEpochMilli();
        }
    }
}