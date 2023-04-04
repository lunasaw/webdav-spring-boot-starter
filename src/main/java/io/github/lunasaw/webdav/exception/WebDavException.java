package io.github.lunasaw.webdav.exception;

import com.luna.common.exception.BaseException;
import lombok.Data;

/**
 * @author weidian
 * @description
 * @date 2023/4/4
 */
@Data
public class WebDavException extends BaseException {

    private String reasonPhrase;

    public WebDavException(String message, int code, String reasonPhrase) {
        super(code, message);
        this.reasonPhrase = reasonPhrase;
    }
}
