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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import com.alibaba.fastjson2.JSON;
import com.luna.common.io.IoUtil;
import io.github.lunasaw.webdav.entity.PropResult;
import io.github.lunasaw.webdav.exception.WebDavException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.json.JSONObject;
import org.json.XML;

/**
 * @author weidian
 */
public class LockResponseHandler extends ValidatingResponseHandler<String> {
    @Override
    public String handleResponse(HttpResponse response) throws IOException {
        super.validateResponse(response);

        // Process the response from the server.
        HttpEntity entity = response.getEntity();
        StatusLine statusLine = response.getStatusLine();
        if (entity == null) {
            throw new WebDavException("No entity found in response", statusLine.getStatusCode(),
                statusLine.getReasonPhrase());
        }
        try {
            return this.getToken(entity.getContent());
        } catch (IOException e) {
            // JAXB error unmarshalling response stream
            throw new WebDavException(e.getMessage(), statusLine.getStatusCode(), statusLine.getReasonPhrase());
        }
    }

    /**
     * Helper method for getting the Multistatus response processor.
     *
     * @param stream The input to read the status
     * @return Multistatus element parsed from the stream
     * @throws IOException When there is a JAXB error
     */
    protected String getToken(InputStream stream)
        throws IOException {
        String read = IoUtil.read(stream, Charset.defaultCharset());
        JSONObject jsonObject = XML.toJSONObject(read);
        PropResult propResult = JSON.parseObject(jsonObject.toString(), PropResult.class);
        return propResult.getProp().getLockdiscovery().getActiveLock().getLocktoken().getHref().iterator().next();
    }
}
