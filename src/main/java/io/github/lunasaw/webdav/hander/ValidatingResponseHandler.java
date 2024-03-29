/*
 * Copyright 2009-2011 Jon Stevens et al.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.lunasaw.webdav.hander;

import com.alibaba.fastjson2.JSON;
import com.luna.common.net.HttpUtils;
import com.luna.common.thread.AsyncEngineUtils;
import io.github.lunasaw.webdav.properties.WebDavConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ResponseHandler;
import org.apache.jackrabbit.webdav.client.methods.BaseDavRequest;
import org.springframework.stereotype.Component;


/**
 * Basic response handler which takes an url for documentation.
 *
 * @param <T> return type of {@link ResponseHandler#handleResponse(HttpResponse)}.
 * @author mirko
 */
@Component
@Slf4j
public abstract class ValidatingResponseHandler<T> implements ResponseHandler<T> {

    /**
     * Checks the response for a statuscode between {@link HttpStatus#SC_OK} and {@link HttpStatus#SC_MULTIPLE_CHOICES}
     * and throws an {@link RuntimeException} otherwise.
     *
     * @param response to check
     * @throws RuntimeException when the status code is not acceptable.
     */
    protected void validateResponse(HttpResponse response) throws RuntimeException {
        StatusLine statusLine = response.getStatusLine();
        int statusCode = statusLine.getStatusCode();
        if (WebDavConfig.openLog) {
            AsyncEngineUtils.execute(() -> log.info("validateResponse::response = {}", JSON.toJSON(response)));
        }
        if (statusCode >= HttpStatus.SC_OK && statusCode < HttpStatus.SC_MULTIPLE_CHOICES) {
            return;
        }
        throw new RuntimeException("Unexpected response: " + statusLine.getStatusCode() + statusLine.getReasonPhrase());
    }
}