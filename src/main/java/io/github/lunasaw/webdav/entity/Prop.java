package io.github.lunasaw.webdav.entity;

import lombok.Data;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Data
public class Prop {
    private Long          getLastModified;
    private String        geteTag;
    private SupportedLock supportedLock;
    private String        getContentType;
    private Long          creationDate;
    private String        getContentLength;
    private Boolean       executable;
    private ResourceType  resourceType;

    public void setExecutable(String executable) {
        this.executable = !"F".equals(executable);
    }

    public void setCreationDate(String creationDate) {
        Instant instant = Instant.parse(creationDate);
        this.creationDate = instant.toEpochMilli();
    }

    public void setGetLastModified(String getLastModified) {
        String dateString = "Thu, 06 Apr 2023 08:42:08 UTC";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH);
        ZonedDateTime zonedDateTime = ZonedDateTime.parse(dateString, formatter);
        long timestamp = zonedDateTime.toInstant().toEpochMilli();
        this.getLastModified = timestamp;
    }
}