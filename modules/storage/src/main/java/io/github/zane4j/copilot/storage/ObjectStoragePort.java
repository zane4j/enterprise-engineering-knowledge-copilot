package io.github.zane4j.copilot.storage;

import java.io.InputStream;
import java.net.URI;
import java.time.Duration;

/**
 * Port for original document storage. A MinIO/S3 adapter will implement this in Phase 1.
 */
public interface ObjectStoragePort {

    StoredObject put(UploadCommand command, InputStream content);

    URI createDownloadUrl(String objectKey, Duration ttl);

    void delete(String objectKey);

    record UploadCommand(String objectKey, String contentType, long contentLength) {
    }

    record StoredObject(String objectKey, String checksum) {
    }
}
