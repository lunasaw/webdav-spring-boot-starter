package io.github.lunasaw.webdav.hander;

import org.apache.http.HttpResponse;
import org.apache.jackrabbit.webdav.client.methods.BaseDavRequest;

/**
 * {@link org.apache.http.client.ResponseHandler} which just executes the request and checks the answer is
 * in the valid range of {@link ValidatingResponseHandler#validateResponse(HttpResponse)}.
 *
 * @author mirko
 */
public class BooleanResponseHandler extends ValidatingResponseHandler<Boolean> {

    private BaseDavRequest baseDavRequest;

    public BooleanResponseHandler(BaseDavRequest baseDavRequest) {
        this.baseDavRequest = baseDavRequest;
    }

    @Override
    public Boolean handleResponse(HttpResponse httpResponse) {
        this.validateResponse(httpResponse);
        return baseDavRequest.succeeded(httpResponse);
    }
}
