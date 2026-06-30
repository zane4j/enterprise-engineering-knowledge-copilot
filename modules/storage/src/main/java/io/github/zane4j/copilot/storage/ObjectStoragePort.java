package io.github.zane4j.copilot.storage;

import java.io.InputStream;
import java.net.URI;
import java.time.Duration;

/** Port for original document storage. */
public interface ObjectStoragePort {

    StoredObject put(UploadCommand command, InputStream content);

    /**
     * Opens an object stream. The caller is responsible for closing the returned stream.
     */
    InputStream get(String objectKey);

    URI createDownloadUrl(String objectKey, Duration ttl);

    void delete(String objectKey);

    record UploadCommand(String objectKey, String contentType, long contentLength) {
    }

    record StoredObject(String objectKey, String checksum) {
    }
}
