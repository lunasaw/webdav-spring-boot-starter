package io.github.lunasaw.webdav.hander;

import com.luna.common.file.FileTools;
import io.github.lunasaw.webdav.exception.WebDavException;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;

import java.io.IOException;

/**
 * @author weidian
 * @description
 * @date 2023/4/7
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class DownloadHandler extends ValidatingResponseHandler<Boolean> {

    private String localPath;

    public DownloadHandler(String localPath) {
        this.localPath = localPath;
    }

    @Override
    public Boolean handleResponse(HttpResponse response) {
        // Process the response from the server.
        HttpEntity entity = response.getEntity();
        StatusLine statusLine = response.getStatusLine();
        if (entity == null) {
            throw new WebDavException("No entity found in response", statusLine.getStatusCode(),
                statusLine.getReasonPhrase());
        }
        this.validateResponse(response);
        try {
            saveFile(response);
            return FileTools.isExists(localPath);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 保存文件
     */
    public void saveFile(HttpResponse httpResponse) throws IOException {
        FileTools.write(httpResponse.getEntity().getContent(), localPath, true);
    }
}
