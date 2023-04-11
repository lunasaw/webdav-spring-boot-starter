package io.github.lunasaw.webdav.hander;

import org.apache.http.HttpResponse;

import java.io.IOException;

/**
 * {@link org.apache.http.client.ResponseHandler} which just executes the request and checks the answer is
 * in the valid range of {@link ValidatingResponseHandler#validateResponse(org.apache.http.HttpResponse)}.
 *
 * @author mirko
 */
public class VoidResponseHandler extends ValidatingResponseHandler<Void>
{
    @Override
    public Void handleResponse(HttpResponse response) throws IOException {
        this.validateResponse(response);
        return null;
    }
}
